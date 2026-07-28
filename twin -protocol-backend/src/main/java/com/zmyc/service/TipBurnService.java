package com.zmyc.service;

import com.zmyc.common.config.BurnConfig;
import com.zmyc.common.enums.AssetType;
import com.zmyc.common.enums.RewardType;
import com.zmyc.common.enums.TxStatus;
import com.zmyc.common.util.TimeUtils;
import com.zmyc.common.util.Web3jUtils;
import com.zmyc.domain.dto.RewardItem;
import com.zmyc.infrastructure.entity.RewardRecordDO;
import com.zmyc.infrastructure.entity.TipBurnRecordDO;
import com.zmyc.infrastructure.entity.UserDepositDO;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.repository.RewardRecordRepository;
import com.zmyc.infrastructure.repository.TipBurnRecordRepository;
import com.zmyc.infrastructure.repository.UserDepositRepository;
import com.zmyc.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TIP燃烧服务
 */
@Slf4j
@Service
public class TipBurnService {

    private static final String TX_STATUS_SUCCESS = "0x1";
    private static final Long BURN_START_DATE = 1704067200L;
    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private BurnConfig burnConfig;

    @Autowired
    private TipBurnRecordRepository burnRecordRepository;

    @Autowired
    private com.zmyc.common.config.Web3jConfig.Web3jManager web3jManager;

    /**
     * 执行每日燃烧任务（DailyBurnJob 调用）
     * 只做幂等检查 + 创建记录 + 非阻塞投递，立即返回
     * 燃烧确认和分红触发由 BurnConfirmJob 驱动
     */
    public void executeDailyBurn() {
        Long today = TimeUtils.getTodayZeroTimestamp();

        // 幂等检查：今日是否已有燃烧记录
        TipBurnRecordDO existing = burnRecordRepository.findByBurnDate(today);
        if (existing != null) {
            log.warn("今日燃烧任务已存在，跳过执行: burnDate={}, status={}", today, existing.getStatus());
            return;
        }

        // 链上时间检查：读合约 lastBurnTime，判断是否已是新的一天
        if (hasAlreadyBurnedToday()) {
            log.warn("链上今日已燃烧，跳过执行: burnDate={}", today);
            return;
        }

        // 计算当日燃烧比例
        int burnRate = calculateBurnRate();
        log.info("开始执行每日燃烧任务: burnDate={}, burnRate={}%", today, burnRate / 100.0);

        // 创建待执行记录
        TipBurnRecordDO record = new TipBurnRecordDO();
        record.setBurnDate(today);
        record.setBurnRate(burnRate);
        record.setStatus(TipBurnRecordDO.Status.PENDING);
        record.setRetryCount(0);
        burnRecordRepository.save(record);

        // 非阻塞发送，失败会自行落库 FAILED，补偿任务重试
        executeBurn(record);
    }

    /**
     * 执行燃烧操作：重试3次
     *
     */
    protected void executeBurn(TipBurnRecordDO record) {
        // 链上检查：当日已燃烧则跳过，不发必然 revert 的交易
        if (hasAlreadyBurnedToday()) {
            log.warn("链上今日已燃烧，跳过本次执行: recordId={}", record.getId());
            return;
        }

        try {
            Web3j web3j = web3jManager.getWeb3j(String.valueOf(burnConfig.getChainId()));

            Function function = new Function(
                    "burnPoolTokens",
                    List.of(new Uint256(BigInteger.valueOf(record.getBurnRate()))),
                    Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);
            String txHash = null;
            for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
                 txHash = Web3jUtils.sendTransaction(
                        web3j,
                        encodedFunction,
                        burnConfig.getTradeContractAddress(),
                        burnConfig.getChainId(),
                        burnConfig.getOperatorPrivateKey()
                );
                if (StringUtils.isBlank(txHash)) {
                    Thread.sleep(2000);
                }
            }
            if (StringUtils.isBlank(txHash)) {
                throw new RuntimeException("燃烧交易发送失败，重试3次仍无txHash");
            }

            // 立即存 txHash + PROCESSING，进程崩溃也能被 BurnConfirmJob 接管
            record.setTxHash(txHash);
            record.setStatus(TipBurnRecordDO.Status.PROCESSING);
            record.setRetryCount(record.getRetryCount() + 1);
            burnRecordRepository.save(record);
            log.info("燃烧交易已提交，等待 BurnConfirmJob 确认: recordId={}, txHash={}, burnRate={}‰",
                    record.getId(), txHash, record.getBurnRate());

        } catch (Exception e) {
            record.setStatus(TipBurnRecordDO.Status.FAILED);
            record.setFailReason(e.getMessage());
            record.setRetryCount(record.getRetryCount() + 1);
            burnRecordRepository.save(record);
            log.error("燃烧交易提交失败，等待补偿任务重试: recordId={}, retryCount={}",
                    record.getId(), record.getRetryCount(), e);
        }
    }

    /**
     * 读取链上 lastBurnTime（秒级时间戳）
     */
    private long readChainLastBurnTime() throws Exception {
        Web3j web3j = web3jManager.getWeb3j(String.valueOf(burnConfig.getChainId()));
        Function function = new Function(
                "lastBurnTime",
                Collections.emptyList(),
                List.of(new TypeReference<Uint256>() {})
        );
        String encoded = FunctionEncoder.encode(function);
        String result = web3j.ethCall(
                Transaction.createEthCallTransaction(null, burnConfig.getTradeContractAddress(), encoded),
                DefaultBlockParameterName.LATEST
        ).send().getValue();

        if (result == null || result.equals("0x") || result.isBlank()) {
            return 0L;
        }
        List<Type> decoded = FunctionReturnDecoder.decode(result, function.getOutputParameters());
        return ((Uint256) decoded.getFirst()).getValue().longValue();
    }

    /**
     * 链上今日是否已燃烧（镜像合约判断：currentDay <= lastBurnDay）
     * true = 已燃烧，跳过；false = 未燃烧，可以发交易
     * 读链失败时保守返回 true（跳过），避免提交必然 revert 的交易
     */
    private boolean hasAlreadyBurnedToday() {
        try {
            long lastBurnTime = readChainLastBurnTime();
            if (lastBurnTime == 0) {
                return false;  // 合约从未燃烧过
            }
            long blockTimestamp = Web3jUtils.getLatestBlockTimestamp(
                    web3jManager.getWeb3j(String.valueOf(burnConfig.getChainId())));
            long burnInterval = 86400L;
            long lastBurnDay = lastBurnTime / burnInterval;
            long currentDay = blockTimestamp / burnInterval;
            return currentDay <= lastBurnDay;  // true = 今日已燃烧
        } catch (Exception e) {
            log.error("读取链上lastBurnTime失败，保守跳过燃烧", e);
            return true;
        }
    }

    /**
     * 计算当日燃烧比例
     * 初始80%，每天降低0.5%，最低30%
     */
    private int calculateBurnRate() {
        int successDays = burnRecordRepository.countDaysSinceStart(BURN_START_DATE);

        int currentRate = burnConfig.getInitialBurnRate()
                - (successDays * burnConfig.getDailyDecreaseRate());

        return Math.max(currentRate, burnConfig.getMinBurnRate());
    }


}
