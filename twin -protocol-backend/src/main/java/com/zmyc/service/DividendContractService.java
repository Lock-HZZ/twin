package com.zmyc.service;

import com.zmyc.common.config.DividendContractConfig;
import com.zmyc.contract.DividendContract;
import com.zmyc.domain.dto.RewardItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 分红合约调用服务
 * 封装与 Dividend.sol 合约的交互逻辑
 */
@Service
public class DividendContractService {

    private static final Logger log = LoggerFactory.getLogger(DividendContractService.class);

    private static final String TX_STATUS_SUCCESS = "0x1";
    private static final int MAX_RECEIPT_RETRIES = 40;
    private static final long RECEIPT_POLL_INTERVAL_MS = 3000L;
    private static final int REQUIRED_CONFIRMATIONS = 3;

    // ------------------------------------------------------------------ //
    //  类型枚举                                                            //
    // ------------------------------------------------------------------ //

    /**
     * 链上奖励类型枚举，与 Dividend.sol rewardType 字段一一对应。
     * 新增奖励类型只需在此处添加一行，业务代码无需修改。
     */
    public enum RewardType {
        STAKE_DIVIDEND((byte) 1),
        REFERRAL_REWARD((byte) 2),
        LP_MINING((byte) 3);
        // 示例：TASK_REWARD((byte) 4), ACTIVITY_REWARD((byte) 5)

        public final byte code;

        RewardType(byte code) {
            this.code = code;
        }

        public static RewardType of(byte code) {
            for (RewardType t : values()) {
                if (t.code == code) return t;
            }
            throw new IllegalArgumentException("Unknown reward type: " + code);
        }
    }

    /**
     * 链上资产类型枚举，携带链上精度，用于 BigDecimal → wei 转换。
     * USDC 为 6 位小数，TIP 为 18 位小数。
     */
    public enum AssetType {
        USDC((byte) 0, BigInteger.TEN.pow(6)),
        TIP((byte) 1, BigInteger.TEN.pow(18));

        public final byte code;
        public final BigInteger decimals;

        AssetType(byte code, BigInteger decimals) {
            this.code = code;
            this.decimals = decimals;
        }

        public static AssetType of(byte code) {
            for (AssetType t : values()) {
                if (t.code == code) return t;
            }
            throw new IllegalArgumentException("Unknown asset type: " + code);
        }
    }

    /**
     * 交易最终状态
     */
    public enum TxStatus {
        SUCCESS,
        FAILED,
        PENDING
    }

    // ------------------------------------------------------------------ //
    //  初始化                                                              //
    // ------------------------------------------------------------------ //

    @Autowired
    private DividendContractConfig config;

    @Autowired
    private com.zmyc.common.config.Web3jConfig.Web3jManager web3jManager;

    private Credentials credentials;
    private StaticGasProvider gasProvider;

    @PostConstruct
    public void init() {
        try {
            credentials = Credentials.create(config.getOperatorPrivateKey());
            log.info("操作员地址: {}", credentials.getAddress());

            BigInteger gasPrice = config.getGasPrice() != null
                    ? BigInteger.valueOf(config.getGasPrice())
                    : getNetworkGasPrice();
            gasProvider = new StaticGasProvider(gasPrice, BigInteger.valueOf(config.getGasLimit()));

            log.info("分红合约服务初始化完成: chainId={}, contract={}",
                    config.getChainId(), config.getContractAddress());
        } catch (Exception e) {
            log.error("初始化分红合约服务失败", e);
            throw new RuntimeException("初始化分红合约服务失败", e);
        }
    }

    // ------------------------------------------------------------------ //
    //  核心公共方法                                                         //
    // ------------------------------------------------------------------ //

    /**
     * 通用幂等批量奖励上链。
     * <p>
     * 所有奖励类型（质押分红、推荐奖励、LP 挖矿、任意扩展类型）均通过此方法发放。
     * 同一 batchId 在链上只会成功执行一次，后端可安全重发（超时/网络抖动重试）。
     *
     * @param batchId 批次唯一标识，32 字节 hex（含 0x 前缀），全局唯一，由调用方生成
     * @param items   奖励列表，每项需包含 userAddress、rewardType、assetType、amount
     * @return 交易哈希
     */
    public String sendRewardBatch(String batchId, List<RewardItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("奖励列表不能为空");
        }

        int size = items.size();
        List<String> users = new ArrayList<>(size);
        List<BigInteger> rewardTypes = new ArrayList<>(size);
        List<BigInteger> assetTypes = new ArrayList<>(size);
        List<BigInteger> amounts = new ArrayList<>(size);

        for (RewardItem item : items) {
            Objects.requireNonNull(item.getUserAddress(), "userAddress 不能为空");
            Objects.requireNonNull(item.getAmount(), "amount 不能为空");
            Objects.requireNonNull(item.getRewardType(), "rewardType 不能为空");
            Objects.requireNonNull(item.getAssetType(), "assetType 不能为空");
            if (item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("奖励金额必须大于0: user=" + item.getUserAddress());
            }

            users.add(item.getUserAddress());
            rewardTypes.add(BigInteger.valueOf(item.getRewardType()));
            assetTypes.add(BigInteger.valueOf(item.getAssetType()));
            amounts.add(toChainAmount(item.getAmount(), item.getAssetType()));
        }

        try {
            log.info("发送奖励批次(幂等): batchId={}, count={}", batchId, size);
            byte[] batchIdBytes = Numeric.hexStringToByteArray(batchId);
            TransactionReceipt receipt = getContract()
                    .addRewardsBatch(batchIdBytes, users, rewardTypes, assetTypes, amounts)
                    .send();

            String txHash = receipt.getTransactionHash();
            if (txHash == null || txHash.isEmpty()) {
                throw new RuntimeException("交易发送失败，未返回 txHash");
            }

            log.info("奖励批次已发送: batchId={}, count={}, txHash={}", batchId, size, txHash);
            return txHash;
        } catch (Exception e) {
            log.error("发送奖励批次失败: batchId={}, count={}, error={}", batchId, size, e.getMessage(), e);
            throw new RuntimeException("发送奖励批次失败: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ //
    //  幂等查询                                                             //
    // ------------------------------------------------------------------ //

    /**
     * 查询某批次是否已在链上处理（幂等校验）
     */
    public boolean isBatchProcessed(String batchId) {
        try {
            byte[] batchIdBytes = Numeric.hexStringToByteArray(batchId);
            return getContract().isBatchProcessed(batchIdBytes).send();
        } catch (Exception e) {
            log.warn("查询批次处理状态失败: batchId={}, error={}", batchId, e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ //
    //  交易确认                                                             //
    // ------------------------------------------------------------------ //

    /**
     * 等待并确认交易最终状态（轮询 receipt，要求足够确认数以防 reorg）
     */
    public TxStatus waitForConfirmation(String txHash) {
        for (int i = 0; i < MAX_RECEIPT_RETRIES; i++) {
            try {
                Optional<TransactionReceipt> receiptOpt = getReceipt(txHash);

                if (receiptOpt.isPresent()) {
                    TransactionReceipt receipt = receiptOpt.get();

                    if (!TX_STATUS_SUCCESS.equals(receipt.getStatus())) {
                        log.error("交易链上执行失败(revert): txHash={}, status={}", txHash, receipt.getStatus());
                        return TxStatus.FAILED;
                    }

                    BigInteger txBlock = receipt.getBlockNumber();
                    BigInteger currentBlock = getWeb3j().ethBlockNumber().send().getBlockNumber();
                    long confirmations = currentBlock.subtract(txBlock).longValue() + 1;

                    if (confirmations >= REQUIRED_CONFIRMATIONS) {
                        log.info("交易已确认: txHash={}, block={}, confirmations={}, gasUsed={}",
                                txHash, txBlock, confirmations, receipt.getGasUsed());
                        return TxStatus.SUCCESS;
                    }

                    log.debug("交易已上链但确认数不足: txHash={}, confirmations={}/{}",
                            txHash, confirmations, REQUIRED_CONFIRMATIONS);
                }

                Thread.sleep(RECEIPT_POLL_INTERVAL_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待交易确认被中断: txHash={}", txHash);
                return TxStatus.PENDING;
            } catch (Exception e) {
                log.warn("查询交易 receipt 出错(将重试): txHash={}, error={}", txHash, e.getMessage());
                try {
                    Thread.sleep(RECEIPT_POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return TxStatus.PENDING;
                }
            }
        }

        log.warn("交易在超时时间内未确认: txHash={}", txHash);
        return TxStatus.PENDING;
    }

    /**
     * 复查历史交易的最终状态（供补偿任务使用）
     */
    public TxStatus checkTransactionStatus(String txHash) {
        try {
            Optional<TransactionReceipt> receiptOpt = getReceipt(txHash);
            if (receiptOpt.isEmpty()) {
                return TxStatus.PENDING;
            }

            TransactionReceipt receipt = receiptOpt.get();
            if (!TX_STATUS_SUCCESS.equals(receipt.getStatus())) {
                return TxStatus.FAILED;
            }

            BigInteger currentBlock = getWeb3j().ethBlockNumber().send().getBlockNumber();
            long confirmations = currentBlock.subtract(receipt.getBlockNumber()).longValue() + 1;
            return confirmations >= REQUIRED_CONFIRMATIONS ? TxStatus.SUCCESS : TxStatus.PENDING;

        } catch (Exception e) {
            log.warn("复查交易状态出错: txHash={}, error={}", txHash, e.getMessage());
            return TxStatus.PENDING;
        }
    }

    /**
     * 判断交易是否仍在链上"生存"（已确认成功 或 仍在内存池）。
     * 用于补偿任务判断是否需要重发：返回 true 时不应重发，避免 nonce 冲突。
     */
    public boolean isTransactionAlive(String txHash) {
        if (txHash == null || txHash.isEmpty()) {
            return false;
        }
        try {
            Optional<TransactionReceipt> receiptOpt = getReceipt(txHash);
            if (receiptOpt.isPresent()) {
                return TX_STATUS_SUCCESS.equals(receiptOpt.get().getStatus());
            }
            var txResp = getWeb3j().ethGetTransactionByHash(txHash).send();
            boolean existsInPool = txResp.getTransaction().isPresent();
            if (existsInPool) {
                log.debug("交易仍在内存池待打包: txHash={}", txHash);
            }
            return existsInPool;
        } catch (Exception e) {
            log.warn("查询交易存活状态异常，保守视为存活: txHash={}, error={}", txHash, e.getMessage());
            return true;
        }
    }

    // ------------------------------------------------------------------ //
    //  余额查询                                                             //
    // ------------------------------------------------------------------ //

    /**
     * 查询用户在分红合约中的余额（业务精度，非 wei）
     */
    public BigDecimal getBalance(String userAddress, byte assetType) {
        try {
            AssetType asset = AssetType.of(assetType);
            BigInteger balanceRaw = getContract()
                    .getTotalBalance(userAddress, BigInteger.valueOf(assetType))
                    .send();
            return new BigDecimal(balanceRaw).divide(new BigDecimal(asset.decimals), 18, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("查询用户余额失败: user={}, assetType={}, error={}", userAddress, assetType, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    // ------------------------------------------------------------------ //
    //  私有工具方法                                                          //
    // ------------------------------------------------------------------ //

    /**
     * 将业务精度金额转换为链上原始整数（wei/最小单位）。
     * USDC 精度 10^6，TIP 精度 10^18，自动按 assetType 选择。
     */
    private BigInteger toChainAmount(BigDecimal amount, byte assetType) {
        AssetType asset = AssetType.of(assetType);
        return amount.multiply(new BigDecimal(asset.decimals))
                .setScale(0, RoundingMode.DOWN)
                .toBigInteger();
    }

    private Web3j getWeb3j() {
        return web3jManager.getWeb3j(String.valueOf(config.getChainId()));
    }

    private DividendContract getContract() {
        Web3j web3j = getWeb3j();
        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, config.getChainId());
        return DividendContract.load(config.getContractAddress(), web3j, txManager, gasProvider);
    }

    private BigInteger getNetworkGasPrice() {
        try {
            return getWeb3j().ethGasPrice().send().getGasPrice();
        } catch (Exception e) {
            log.warn("获取网络 Gas 价格失败，使用默认值", e);
            return BigInteger.valueOf(5_000_000_000L);
        }
    }

    private Optional<TransactionReceipt> getReceipt(String txHash) throws Exception {
        EthGetTransactionReceipt response = getWeb3j().ethGetTransactionReceipt(txHash).send();
        return response.getTransactionReceipt();
    }
}
