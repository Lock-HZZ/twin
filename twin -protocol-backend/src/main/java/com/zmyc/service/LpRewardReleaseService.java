package com.zmyc.service;

import com.zmyc.common.enums.AssetType;
import com.zmyc.common.enums.RewardType;
import com.zmyc.common.util.TimeUtils;
import com.zmyc.domain.dto.RewardItem;
import com.zmyc.infrastructure.entity.LpRewardReleaseDO;
import com.zmyc.infrastructure.entity.RewardRecordDO;
import com.zmyc.infrastructure.entity.UserDepositDO;
import com.zmyc.infrastructure.repository.LpRewardReleaseRepository;
import com.zmyc.infrastructure.repository.RewardRecordRepository;
import com.zmyc.infrastructure.repository.UserDepositRepository;
import com.zmyc.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LP挖矿奖励释放服务
 * 负责：
 * 1. 监听到 RemoveLiquidity 事件后，生成60天释放计划
 * 2. 每日定时任务调用，将当日应释放的奖励写入 reward_record
 */
@Slf4j
@Service
public class LpRewardReleaseService {

    @Autowired
    private LpRewardReleaseRepository lpReleaseRepository;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDepositRepository depositRepository;

    @Autowired
    private DividendContractService dividendContractService;

    @Autowired
    private UserLevelService  userLevelService;

    private static final int RELEASE_DAYS = 60;
    private static final int BATCH_SIZE = 200;

    /**
     * 创建60天释放计划（事件监听器调用）
     */
    @Transactional
    public void createReleaseSchedule(String userAddress, BigDecimal tipToDividend, String removeTxHash) {
        if (tipToDividend == null || tipToDividend.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("移除LP奖励金额为0，跳过释放计划: txHash={}", removeTxHash);
            return;
        }

        // 幂等：同一笔移除LP交易只生成一次
        if (lpReleaseRepository.existsByRemoveTxHash(removeTxHash)) {
            log.warn("释放计划已存在，跳过重复创建: removeTxHash={}", removeTxHash);
            return;
        }

        // 查用户ID
        Long userId = userRepository.findUserIdByAddress(userAddress);
        if (userId == null) {
            log.error("用户不存在: address={}, txHash={}", userAddress, removeTxHash);
            return;
        }

        // 查入金订单（通过 withdrawTxHash 关联）
        UserDepositDO deposit = depositRepository.findByWithdrawTxHash(removeTxHash);
        if (deposit == null) {
            log.warn("未找到对应的入金订单: removeTxHash={}", removeTxHash);
            return;
        }

        deposit.setStatus(UserDepositDO.Status.REMOVED);
        depositRepository.save(deposit);
        // 移除LP后，该用户的推荐人链上可能失去一个有效直推，需更新等级
        userLevelService.updateUserAndAncestorsLevel(deposit.getUserId());

        // 生成60条释放记录
        BigDecimal dailyAmount = tipToDividend.divide(new BigDecimal(RELEASE_DAYS), 18, RoundingMode.DOWN);
        long now = TimeUtils.now();
        LocalDate today = LocalDate.now();

        List<LpRewardReleaseDO> releases = new ArrayList<>();
        for (int i = 1; i <= RELEASE_DAYS; i++) {
            LocalDate releaseDate = today.plusDays(i);
            Integer releaseDateInt = Integer.parseInt(releaseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            LpRewardReleaseDO release = new LpRewardReleaseDO();
            release.setUserId(userId);
            release.setDepositId(deposit.getId());
            release.setRemoveTxHash(removeTxHash);
            release.setTotalAmount(tipToDividend);
            release.setDailyAmount(dailyAmount);
            release.setReleaseDate(releaseDateInt);
            release.setReleasedAmount(BigDecimal.ZERO);
            release.setStatus(LpRewardReleaseDO.Status.PENDING);
            release.setCreatedDate(now);
            release.setLastUpdatedDate(now);

            releases.add(release);
        }

        lpReleaseRepository.insertBatch(releases);
        log.info("LP奖励释放计划已创建: userId={}, depositId={}, totalAmount={}, days={}, dailyAmount={}, removeTxHash={}",
                userId, deposit.getId(), tipToDividend, RELEASE_DAYS, dailyAmount, removeTxHash);
    }

    /**
     * 释放今日应发的LP奖励（每日定时任务调用）
     * 按 BATCH_SIZE 分片，每片独立 batchId，防止单次交易过大被链上拒绝
     */
    @Transactional
    public void releaseTodayRewards() {
        Integer today = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        List<LpRewardReleaseDO> pending = lpReleaseRepository.findPendingByReleaseDate(today);

        if (pending.isEmpty()) {
            log.info("今日无待释放的LP奖励: date={}", today);
            return;
        }

        log.info("今日待释放LP奖励: count={}, date={}", pending.size(), today);
        long now = TimeUtils.now();

        // 分片处理，每片独立 batchId
        List<List<LpRewardReleaseDO>> shards = partition(pending);
        for (int i = 0; i < shards.size(); i++) {
            List<LpRewardReleaseDO> shard = shards.get(i);
            String batchId = generateBatchId(today, i);

            List<RewardRecordDO> records = shard.stream().map(release -> {
                RewardRecordDO record = new RewardRecordDO();
                record.setUserId(release.getUserId());
                record.setAmount(release.getDailyAmount());
                record.setRewardType(RewardType.LP_MINING.code);
                record.setAssetType(AssetType.TIP.code);
                record.setBusinessId(release.getDepositId());
                record.setRewardDate(today);
                record.setBatchId(batchId);
                record.setStatus(RewardRecordDO.Status.PENDING);
                record.setRemark("LP挖矿奖励");
                record.setCreatedDate(now);
                record.setUpdatedDate(now);
                return record;
            }).collect(Collectors.toList());

            rewardRecordRepository.insertBatch(records);
            log.info("LP奖励入库: shard={}/{}, count={}, batchId={}", i + 1, shards.size(), records.size(), batchId);

            // 先标记释放计划，再发送（发送失败不影响入库，由RewardConfirmJob重发）
            List<Long> releaseIds = shard.stream().map(LpRewardReleaseDO::getId).collect(Collectors.toList());
            lpReleaseRepository.updateStatus(releaseIds, LpRewardReleaseDO.Status.RELEASED, batchId);

            sendBatch(batchId, records);
        }
    }

    private void sendBatch(String batchId, List<RewardRecordDO> records) {
        try {
            // 获取用户地址
            Set<Long> userIds = records.stream().map(RewardRecordDO::getUserId).collect(Collectors.toSet());
            Map<Long, String> addresses = userRepository.findAddressesByUserIds(userIds);

            List<RewardItem> items = records.stream()
                    .filter(r -> addresses.containsKey(r.getUserId()))
                    .map(r ->RewardItem.builder()
                            .userId(r.getUserId())
                            .userAddress(addresses.get(r.getUserId()))
                            .amount(r.getAmount())
                            .rewardType(r.getRewardType())
                            .assetType(r.getAssetType())
                            .businessId(r.getBusinessId())
                            .build())
                    .collect(Collectors.toList());

            if (items.isEmpty()) {
                log.warn("无有效地址，跳过上链: batchId={}", batchId);
                return;
            }

            String txHash = dividendContractService.sendRewardBatch(batchId, items);
            long now = System.currentTimeMillis() / 1000;
            rewardRecordRepository.markBatchSent(batchId, txHash, now);

            log.info("LP奖励批次已发送: batchId={}, txHash={}, count={}", batchId, txHash, items.size());
        } catch (Exception e) {
            log.error("LP奖励批次发送失败，保持PENDING待重发: batchId={}", batchId, e);
        }
    }

    private <T> List<List<T>> partition(List<T> list) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            result.add(list.subList(i, Math.min(i + BATCH_SIZE, list.size())));
        }
        return result;
    }

    private String generateBatchId(Integer releaseDate, int shardIndex) {
        String data = "LP_MINING_" + releaseDate + "_" + shardIndex;
        byte[] hash = org.web3j.crypto.Hash.sha3(data.getBytes());
        return Numeric.toHexString(hash);
    }
}
