package com.zmyc.service;

import com.zmyc.application.vo.response.RemoveLiquidityResponse;
import com.zmyc.common.config.TradeConfig;
import com.zmyc.common.context.UserContext;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.common.util.TimeUtils;
import com.zmyc.common.util.Web3jUtils;
import com.zmyc.infrastructure.entity.UserDepositDO;
import com.zmyc.infrastructure.repository.UserDepositRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Trade合约服务 - 处理移除LP操作
 */
@Slf4j
@Service
public class TradeContractService {

    private static final String REDIS_LOCK_PREFIX = "trade:remove_liquidity:";
    private static final long LOCK_EXPIRE_SECONDS = 300;
    // REMOVING 状态超过此时间仍未确认，视为失败并重置允许重试（秒）
    private static final long REMOVING_TIMEOUT_SECONDS = 600;
    private static final String TX_STATUS_SUCCESS = "0x1";

    @Autowired
    private TradeConfig tradeConfig;

    @Autowired
    private UserDepositRepository depositRepository;

    @Autowired
    private com.zmyc.common.config.Web3jConfig.Web3jManager web3jManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Credentials credentials;

    @PostConstruct
    public void init() {
        try {
            credentials = Credentials.create(tradeConfig.getOperatorPrivateKey());
            log.info("Trade合约操作员地址: {}", credentials.getAddress());
            log.info("Trade合约服务初始化完成: chainId={}, contract={}",
                    tradeConfig.getChainId(), tradeConfig.getContractAddress());
        } catch (Exception e) {
            log.error("初始化Trade合约服务失败", e);
            throw new RuntimeException("初始化Trade合约服务失败", e);
        }
    }

    // ------------------------------------------------------------------ //
    //  移除LP - 对外接口                                                   //
    // ------------------------------------------------------------------ //

    /**
     * 移除LP - 后端直接调用合约
     * <p>
     * 防重复调用机制：
     * 1. Redis锁防止并发请求同时进入
     * 2. 订单状态 REMOVING 防止锁释放后的重复提交
     * 3. withdrawTxHash 记录已发出的交易，供回执轮询使用
     */
    @Transactional
    public RemoveLiquidityResponse removeLiquidity(Long depositId) {
        Long userId = UserContext.getCurrentUserId();
        String userAddress = UserContext.getCurrentAddress();
        if (userAddress == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }

        // 1. Redis 防并发锁（防止同一订单同时发起两次请求）
        String lockKey = REDIS_LOCK_PREFIX + depositId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            log.warn("订单正在处理中，拒绝重复请求: depositId={}", depositId);
            throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_PROCESSING);
        }

        try {
            // 2. 查询并验证订单
            UserDepositDO deposit = depositRepository.findById(depositId);
            if (deposit == null) {
                throw new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND);
            }
            if (!deposit.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.DEPOSIT_NOT_BELONG_TO_USER);
            }
            // REMOVING 状态说明已有交易在途，拒绝重复提交
            if (UserDepositDO.Status.REMOVING.equals(deposit.getStatus())) {
                throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_PROCESSING);
            }
            if (!UserDepositDO.Status.COMPLETED.equals(deposit.getStatus())) {
                throw new BusinessException(ErrorCode.DEPOSIT_NOT_COMPLETED);
            }

            // 3. 发送合约交易，只要节点接受就拿到 txHash
            long lockDuration = (TimeUtils.now() - deposit.getCreatedDate()) / 24 * 3600;
            String txHash = callRemoveLiquidity(userAddress, deposit.getLiquidity(), lockDuration);

            deposit.setWithdrawTxHash(txHash);
            deposit.setStatus(UserDepositDO.Status.REMOVING);
            depositRepository.save(deposit);

            log.info("移除LP交易已发送，等待上链确认: userId={}, depositId={}, liquidity={}, txHash={}",
                    userId, depositId, deposit.getLiquidity(), txHash);

            RemoveLiquidityResponse response = new RemoveLiquidityResponse();
            response.setTxHash(txHash);
            response.setStatus("PENDING");
            return response;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("移除LP失败: depositId={}", depositId, e);
            throw new BusinessException(ErrorCode.CONTRACT_CALL_FAILED);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    // ------------------------------------------------------------------ //
    //  定时任务：轮询 REMOVING 订单回执                                     //
    // ------------------------------------------------------------------ //

    /**
     * 轮询 REMOVING 状态的订单，确认链上结果（由 RemoveLiquidityConfirmJob 调用）：
     * - 回执成功 (0x1) → REMOVED
     * - 回执失败 (0x0) 或超时 → 重置为 COMPLETED，允许用户重试
     */
    public void pollRemovingOrders() {
        List<UserDepositDO> removing = depositRepository.findByStatus(UserDepositDO.Status.REMOVING);
        if (removing.isEmpty()) {
            return;
        }

        Web3j web3j;
        try {
            web3j = web3jManager.getWeb3j(String.valueOf(tradeConfig.getChainId()));
        } catch (Exception e) {
            log.error("获取Web3j失败，跳过本次轮询", e);
            return;
        }

        long now = System.currentTimeMillis() / 1000;

        for (UserDepositDO deposit : removing) {
            try {
                pollSingle(web3j, deposit, now);
            } catch (Exception e) {
                log.error("轮询订单回执异常: depositId={}", deposit.getId(), e);
            }
        }
    }

    protected void pollSingle(Web3j web3j, UserDepositDO deposit, long now) throws Exception {
        String txHash = deposit.getWithdrawTxHash();
        if (txHash == null || txHash.isBlank()) {
            log.error("订单处于 REMOVING 状态但无 withdrawTxHash，重置为 COMPLETED: depositId={}", deposit.getId());
            deposit.setStatus(UserDepositDO.Status.COMPLETED);
            depositRepository.save(deposit);
            return;
        }

        TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash)
                .send()
                .getTransactionReceipt()
                .orElse(null);

        if (receipt != null) {
            if (TX_STATUS_SUCCESS.equalsIgnoreCase(receipt.getStatus())) {
                // 上链成功
                deposit.setStatus(UserDepositDO.Status.REMOVED);
                depositRepository.save(deposit);
                log.info("移除LP上链成功: depositId={}, txHash={}", deposit.getId(), txHash);
            } else {
                // 交易 revert
                deposit.setStatus(UserDepositDO.Status.COMPLETED);
                deposit.setWithdrawTxHash(null);
                depositRepository.save(deposit);
                log.error("移除LP交易revert，已重置为COMPLETED: depositId={}, txHash={}", deposit.getId(), txHash);
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  链上事件回调                                                         //
    // ------------------------------------------------------------------ //

    /**
     * 处理链上 RemoveLiquidity 事件（由事件监听器调用）
     */
    @Transactional
    public void processRemoveLiquidityEvent(String user, BigDecimal liquidity, BigDecimal usdcOut, BigDecimal tipToDividend, String txHash) {
        UserDepositDO deposit = depositRepository.findByWithdrawTxHash(txHash);
        if (deposit == null) {
            log.warn("未找到对应的入金订单: txHash={}", txHash);
            return;
        }

        deposit.setStatus(UserDepositDO.Status.REMOVED);
        deposit.setLiquidity(BigDecimal.ZERO);
        depositRepository.save(deposit);

        log.info("移除LP事件处理完成: depositId={}, usdcOut={}, tipToDividend={}, txHash={}",
                deposit.getId(), usdcOut, tipToDividend, txHash);
    }

    // ------------------------------------------------------------------ //
    //  私有：发送合约调用                                                   //
    // ------------------------------------------------------------------ //

    private String callRemoveLiquidity(String user, BigDecimal liquidity, long lockDuration) {
        try {
            Web3j web3j = web3jManager.getWeb3j(String.valueOf(tradeConfig.getChainId()));

            long currentBlockTime = Web3jUtils.getLatestBlockTimestamp(web3j);
            BigInteger deadline = BigInteger.valueOf(currentBlockTime + 600);

            BigInteger liquidityWei = liquidity.multiply(new BigDecimal(BigInteger.TEN.pow(18))).toBigInteger();

            BigInteger lockDurationWei = BigInteger.valueOf(lockDuration);

            Function function = new Function(
                    "removeLiquidity",
                    Arrays.asList(
                            new Address(user),
                            new Uint256(liquidityWei),
                            new Uint256(lockDurationWei),
                            new Uint256(deadline)
                    ),
                    Collections.emptyList()
            );

            String txHash = Web3jUtils.sendTransaction(
                    web3j,
                    FunctionEncoder.encode(function),
                    tradeConfig.getContractAddress(),
                    tradeConfig.getChainId(),
                    tradeConfig.getOperatorPrivateKey()
            );

            log.info("removeLiquidity 交易已提交至节点: user={}, liquidity={}, txHash={}", user, liquidity, txHash);
            return txHash;

        } catch (Exception e) {
            log.error("调用 removeLiquidity 失败: user={}, liquidity={}", user, liquidity, e);
            throw new BusinessException(ErrorCode.CONTRACT_CALL_FAILED);
        }
    }
}
