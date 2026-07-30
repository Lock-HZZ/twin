package com.zmyc.service;

import com.zmyc.common.context.UserContext;
import com.zmyc.common.enums.AssetType;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.enums.RewardType;
import com.zmyc.common.enums.TxStatus;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.common.util.TimeUtils;
import com.zmyc.domain.dto.RewardItem;
import com.zmyc.infrastructure.entity.RewardRecordDO;
import com.zmyc.infrastructure.entity.UserStakeDO;
import com.zmyc.infrastructure.repository.RewardRecordRepository;
import com.zmyc.infrastructure.repository.SystemConfigRepository;
import com.zmyc.infrastructure.repository.UserRepository;
import com.zmyc.infrastructure.repository.UserStakeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zmyc.common.constant.SystemConfigKey.*;
import static com.zmyc.common.enums.TxStatus.SUCCESS;

@Service
public class UserStakeService {

    private static final Logger log = LoggerFactory.getLogger(UserStakeService.class);

    @Autowired
    private UserStakeRepository stakeRepository;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private DividendContractService dividendContractService;

    @Autowired
    private com.zmyc.common.config.DividendContractConfig dividendContractConfig;

    @Autowired
    private UserRepository userRepository;

    /**
     * 记录链上质押事件（监听链上事件后调用）
     */
    @Transactional
    public void recordStakeFromChain(Long userId, Long stakeId, BigDecimal amount, Integer plan, Long startTime, Long endTime, String txHash) {
        // 校验套餐
        if (plan != 30 && plan != 90 && plan != 180 && plan != 360) {
            throw new BusinessException(ErrorCode.STAKE_PLAN_INVALID);
        }

        // 获取对应套餐的APY
        BigDecimal apy = getApyByPlan(plan);
        if (apy == null) {
            log.error("未配置套餐{}的APY", plan);
            apy = BigDecimal.ZERO;
        }

        UserStakeDO stake = new UserStakeDO();
        stake.setUserId(userId);
        stake.setStakeId(stakeId);
        stake.setAmount(amount);
        stake.setPlan(plan);
        stake.setApy(apy);
        stake.setStartTime(startTime);
        stake.setEndTime(endTime);
        stake.setStatus(UserStakeDO.Status.ACTIVE);
        stake.setTxHash(txHash);
        stake.setCreatedDate(System.currentTimeMillis() / 1000);
        stakeRepository.save(stake);

        log.info("记录质押: userId={}, stakeId={}, amount={}, plan={}, apy={}%", userId, stakeId, amount, plan, apy);
    }

    /**
     * 记录链上赎回事件（监听链上事件后调用）
     */
    @Transactional
    public void recordWithdrawFromChain(Long stakeId, String txHash) {
        UserStakeDO stake = stakeRepository.findByStakeId(stakeId);
        if (stake == null) {
            log.error("质押记录不存在: stakeId={}", stakeId);
            throw new BusinessException(ErrorCode.STAKE_NOT_FOUND);
        }

        if (stake.getStatus() == UserStakeDO.Status.WITHDRAWN) {
            log.warn("质押已赎回: stakeId={}", stakeId);
            return;
        }

        stake.setStatus(UserStakeDO.Status.WITHDRAWN);
        stake.setTxHash(txHash);
        stakeRepository.save(stake);

        log.info("记录赎回: stakeId={}, userId={}", stakeId, stake.getUserId());
    }

    /**
     * 查询用户所有质押记录
     */
    public List<UserStakeDO> getUserStakes(Long userId) {
        return stakeRepository.findByUserId(userId);
    }

    /**
     * 查询用户进行中的质押记录
     */
    public List<UserStakeDO> getUserActiveStakes(Long userId) {
        return stakeRepository.findActiveStakesByUserId(userId);
    }

    /**
     * 主任务入口（每天一次）：计算今日分红 → 发送 PENDING 记录 → 标 SENT。
     * 职责边界：主任务负责初次发送，发完标 SENT 即可，不等确认、不做补偿。
     * 补偿由 RewardConfirmJob 统一处理。
     */
    public void generateAndDistributeDailyDividends() {
        Long today = TimeUtils.getTodayZeroTimestamp();
        prepareTodayDividends(today);
        sendPendingDividends();
    }

    /**
     * 计算今日分红并以 PENDING 批量落库，同时按分片计算并写入 batchId。
     * 不加事务：纯内存计算 + 一次批量插入，insertBatch 内部自带事务。
     * 幂等依赖：先查当日已存在的 stakeId 集合，内存过滤，不依赖唯一索引捕异常。
     */
    public void prepareTodayDividends(Long today) {
        List<UserStakeDO> activeStakes = stakeRepository.findAllActiveStakes();
        if (activeStakes.isEmpty()) {
            return;
        }

        Integer rewardDate = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        Set<Long> existingStakeIds = rewardRecordRepository.findExistingStakeIdsByDateAndType(rewardDate, RewardType.STAKE_DIVIDEND.code);

        long now = System.currentTimeMillis() / 1000;
        int batchSize = dividendContractConfig.getBatchSize();
        int shardIndex = 0;
        int inShard = 0;
        List<RewardRecordDO> toInsert = new ArrayList<>();

        for (UserStakeDO stake : activeStakes) {
            if (existingStakeIds.contains(stake.getId())) {
                continue;
            }

            BigDecimal dailyAmount = stake.getAmount()
                    .multiply(stake.getApy())
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

            RewardRecordDO reward = new RewardRecordDO();
            reward.setUserId(stake.getUserId());
            reward.setAmount(dailyAmount);
            reward.setRewardType(RewardType.STAKE_DIVIDEND.code);
            reward.setAssetType((byte) 1); // TIP
            reward.setBatchId(computeBatchId(today, shardIndex));
            reward.setBusinessId(stake.getId()); // 关联质押记录ID
            reward.setRewardDate(rewardDate);
            reward.setStatus(RewardRecordDO.Status.PENDING);
            reward.setRemark("质押分红");
            reward.setCreatedDate(now);
            toInsert.add(reward);

            if (++inShard >= batchSize) {
                shardIndex++;
                inShard = 0;
            }
        }

        if (toInsert.isEmpty()) {
            log.info("今日分红已全部准备，无新记录: date={}", today);
            return;
        }

        rewardRecordRepository.insertBatch(toInsert);
        log.info("今日分红准备完成: date={}, 新建={}, 分片数={}", today, toInsert.size(), shardIndex + (inShard > 0 ? 1 : 0));
    }

    /**
     * 发送所有 PENDING 记录（仅主任务调用）。按 batchId 分组，逐批发送。
     */
    public void sendPendingDividends() {
        List<RewardRecordDO> pending = rewardRecordRepository.findByStatusAndRewardType(
                RewardRecordDO.Status.PENDING.getValue(), RewardType.STAKE_DIVIDEND.code);
        if (pending.isEmpty()) {
            log.info("无待发放分红");
            return;
        }

        Map<String, List<RewardRecordDO>> grouped = pending.stream()
                .filter(r -> r.getBatchId() != null && !r.getBatchId().isEmpty())
                .collect(Collectors.groupingBy(RewardRecordDO::getBatchId));

        for (Map.Entry<String, List<RewardRecordDO>> entry : grouped.entrySet()) {
            sendBatch(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 发送单个批次：发交易 → 保证 txHash 落库(SENT) → 等确认标 PAID。
     * 仅由主任务调用，处理首次发送 PENDING 记录。
     */
    private void sendBatch(String batchId, List<RewardRecordDO> records) {
        long now = System.currentTimeMillis() / 1000;

        // 幂等保护：若链上已处理该批次，直接标成功，不再发送
        if (dividendContractService.isBatchProcessed(batchId)) {
            rewardRecordRepository.markBatchPaid(batchId, null, now);
            log.info("批次已在链上处理，批量标记成功: batchId={}", batchId);
            return;
        }

        // 批量查询用户地址（一次查询）
        Set<Long> userIds = records.stream().map(RewardRecordDO::getUserId).collect(Collectors.toSet());
        Map<Long, String> addressMap = userRepository.findAddressesByUserIds(userIds);

        // 收集有效记录（地址存在的）
        List<RewardRecordDO> toSend = new ArrayList<>();
        List<String> addresses = new ArrayList<>();
        for (RewardRecordDO record : records) {
            String address = addressMap.get(record.getUserId());
            if (address == null || address.isEmpty()) {
                log.error("用户地址为空，跳过分红: id={}, userId={}", record.getId(), record.getUserId());
                continue;
            }
            toSend.add(record);
            addresses.add(address);
        }

        if (toSend.isEmpty()) {
            return;
        }

        // 发送交易（幂等，传 batchId）
        String txHash;
        try {
            List<RewardItem> items = new ArrayList<>(toSend.size());
            for (int i = 0; i < toSend.size(); i++) {
                items.add(RewardItem.builder()
                        .userAddress(addresses.get(i))
                        .rewardType(RewardType.STAKE_DIVIDEND.code)
                        .assetType(AssetType.TIP.code)
                        .amount(toSend.get(i).getAmount())
                        .build());
            }
            txHash = dividendContractService.sendRewardBatch(batchId, items);
        } catch (Exception e) {
            // 发送失败：记录仍为 PENDING（无 txHash），下次主任务重发。补偿任务不会碰它。
            log.error("批次交易发送失败，保持 PENDING 待重发: batchId={}, count={}", batchId, toSend.size(), e);
            return;
        }

        // 关键：拿到 txHash 后第一时间落库（SENT），保证"发出的交易一定有 hash"。
        // 此后即使进程崩溃，记录也已是 SENT+txHash，补偿任务能接手确认。
        rewardRecordRepository.markBatchSent(batchId, txHash, System.currentTimeMillis() / 1000);

        // 等待确认并更新最终状态
        TxStatus status = dividendContractService.waitForConfirmation(txHash);
        switch (status) {
            case SUCCESS -> {
                rewardRecordRepository.markBatchPaid(batchId, txHash, System.currentTimeMillis() / 1000);
                log.info("批次分红发放成功: batchId={}, count={}, txHash={}", batchId, toSend.size(), txHash);
            }
            case FAILED -> log.error("批次交易链上失败(revert)，保持 SENT 交补偿任务复查: batchId={}, txHash={}",
                    batchId, txHash);
            case PENDING -> log.warn("批次交易未在超时内确认，保持 SENT 交补偿任务复查: batchId={}, txHash={}",
                    batchId, txHash);
        }
    }

    /**
     * 重发批次（补偿任务专用）：已有 txHash 但交易失效，重新发送并覆盖 txHash。
     * 因为记录已经是 SENT，所以直接覆盖 txHash，不需要改状态。
     */
    private void resendBatch(String batchId, List<RewardRecordDO> records) {
        long now = System.currentTimeMillis() / 1000;

        // 二次确认链上未处理（防止并发）
        if (dividendContractService.isBatchProcessed(batchId)) {
            rewardRecordRepository.markBatchPaid(batchId, null, now);
            log.info("补偿重发前发现批次已在链上处理: batchId={}", batchId);
            return;
        }

        // 批量查询用户地址（一次查询）
        Set<Long> userIds = records.stream().map(RewardRecordDO::getUserId).collect(Collectors.toSet());
        Map<Long, String> addressMap = userRepository.findAddressesByUserIds(userIds);

        // 收集有效记录
        List<RewardRecordDO> toSend = new ArrayList<>();
        List<String> addresses = new ArrayList<>();
        for (RewardRecordDO record : records) {
            String address = addressMap.get(record.getUserId());
            if (address == null || address.isEmpty()) {
                log.error("补偿重发：用户地址为空，跳过: id={}, userId={}", record.getId(), record.getUserId());
                continue;
            }
            toSend.add(record);
            addresses.add(address);
        }

        if (toSend.isEmpty()) {
            return;
        }

        // 重新发送交易（同一 batchId，链上幂等）
        String newTxHash;
        try {
            List<RewardItem> items = new ArrayList<>(toSend.size());
            for (int i = 0; i < toSend.size(); i++) {
                items.add(RewardItem.builder()
                        .userAddress(addresses.get(i))
                        .rewardType(RewardType.STAKE_DIVIDEND.code)
                        .assetType(AssetType.TIP.code)
                        .amount(toSend.get(i).getAmount())
                        .build());
            }
            newTxHash = dividendContractService.sendRewardBatch(batchId, items);
        } catch (Exception e) {
            log.error("补偿重发失败，保持 SENT 待下次重试: batchId={}, count={}", batchId, toSend.size(), e);
            return;
        }

        // 覆盖旧的 txHash（记录本来就是 SENT，只是换新 hash）
        rewardRecordRepository.markBatchSent(batchId, newTxHash, now);
        log.info("补偿重发成功，更新 txHash: batchId={}, oldTxHash={}, newTxHash={}",
                batchId, records.get(0).getTxHash(), newTxHash);

        // 不在这里等待确认（补偿任务高频，下次进来会继续查）
    }

    /**
     * 计算分片 batchId = keccak256(dividendDate || shardIndex)，稳定可复现。
     */
    private String computeBatchId(Long dividendDate, int shardIndex) {
        String seed = "stake-dividend:" + dividendDate + ":" + shardIndex;
        return Numeric.toHexString(Hash.sha3(seed.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 根据套餐获取APY
     */
    private BigDecimal getApyByPlan(Integer plan) {
        String configKey = switch (plan) {
            case 30 -> STAKE_APY_30;
            case 90 -> STAKE_APY_90;
            case 180 -> STAKE_APY_180;
            case 360 -> STAKE_APY_360;
            default -> null;
        };

        if (configKey == null) {
            return null;
        }

        return systemConfigRepository.getConfigValueAsBigDecimal(configKey, BigDecimal.ZERO);
    }

    /**
     * 获取当前用户的质押记录
     */
    public List<UserStakeDO> getCurrentUserStakes() {
        Long userId = UserContext.getCurrentUserId();
        return stakeRepository.findByUserId(userId);
    }

    /**
     * 获取当前用户的分红记录
     */
    public List<RewardRecordDO> getCurrentUserDividends() {
        Long userId = UserContext.getCurrentUserId();
        return rewardRecordRepository.findByUserIdAndRewardType(userId, RewardType.STAKE_DIVIDEND.code);
    }

}
