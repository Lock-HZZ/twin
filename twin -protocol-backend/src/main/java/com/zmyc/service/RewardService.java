package com.zmyc.service;

import com.zmyc.common.enums.AssetType;
import com.zmyc.common.enums.RewardType;
import com.zmyc.domain.dto.RewardItem;
import com.zmyc.infrastructure.entity.RewardRecordDO;
import com.zmyc.infrastructure.entity.TipBurnRecordDO;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.entity.UserDepositDO;
import com.zmyc.infrastructure.entity.UserPerformanceDO;
import com.zmyc.infrastructure.repository.RewardRecordRepository;
import com.zmyc.infrastructure.repository.UserDepositRepository;
import com.zmyc.infrastructure.repository.UserPerformanceRepository;
import com.zmyc.infrastructure.repository.UserRelationClosureRepository;
import com.zmyc.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RewardService {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @Autowired
    private DividendContractService dividendContractService;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserPerformanceRepository performanceRepository;

    @Autowired
    private UserRelationClosureRepository closureRepository;

    @Autowired
    private UserRelationService userRelationService;

    private static final int DIVIDEND_BATCH_SIZE = 200;

    /**
     * 分红逻辑
     * 入金加权分红 60%，节点分红 15%，合伙人 6%，动态分币 14%
     * 先全量入库（含batchId，PENDING），再分批上链：
     *   发送 → 立即存 txHash(SENT) → waitForConfirmation → 标 PAID
     * 补偿任务通过 reconcileBurnDividends() 处理未确认的 SENT 批次
     */
    @Transactional
    public void distributeBurnDividend(TipBurnRecordDO record) {
        log.info("开始分配燃烧分红: recordId={}, dividendAmount={}", record.getId(), record.getDividendAmount());

        // 幂等：已存在分红记录则跳过（checkBurnStatus 可能被多次触发）
        if (rewardRecordRepository.existsByBusinessId(record.getId())) {
            log.warn("燃烧分红记录已存在，跳过重复分配: recordId={}", record.getId());
            return;
        }

        BigDecimal totalDividend = record.getDividendAmount();
        if (totalDividend == null || totalDividend.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("燃烧分红金额为0或未设置，跳过分红: recordId={}", record.getId());
            return;
        }

        long now = System.currentTimeMillis() / 1000;
        int rewardDate = Integer.parseInt(new java.text.SimpleDateFormat("yyyyMMdd")
                .format(new java.util.Date(now * 1000)));

        // --- 1. 计算各类分红 ---
        List<RewardRecordDO> allRecords = new ArrayList<>();

        BigDecimal depositDividend = totalDividend.multiply(new BigDecimal("0.60"));
        allRecords.addAll(toRewardRecords(record.getId(), rewardDate,
                calculateDepositWeightedDividend(depositDividend),
                RewardType.BURN_DEPOSIT_WEIGHTED.code, "入金加权分红", now));

        BigDecimal nodeDividend = totalDividend.multiply(new BigDecimal("0.15"));
        allRecords.addAll(toRewardRecords(record.getId(), rewardDate,
                calculateNodeWeightedDividend(nodeDividend),
                RewardType.BURN_NODE_WEIGHTED.code, "节点加权分红", now));

        BigDecimal partnerDividend = totalDividend.multiply(new BigDecimal("0.06"));
        allRecords.addAll(toRewardRecords(record.getId(), rewardDate,
                calculatePartnerEqualDividend(partnerDividend),
                RewardType.BURN_PARTNER_EQUAL.code, "合伙人平均分红", now));

        BigDecimal dynamicDividend = totalDividend.multiply(new BigDecimal("0.14"));
        allRecords.addAll(calculateDynamicLevelRewardRecords(record.getId(), rewardDate, dynamicDividend, now));

        if (allRecords.isEmpty()) {
            log.warn("无有效分红对象，跳过入库和上链: recordId={}", record.getId());
            return;
        }

        // --- 2. 分批，每批分配 batchId，全量入库（PENDING） ---
        List<List<RewardRecordDO>> batches = partition(allRecords);
        for (int i = 0; i < batches.size(); i++) {
            String batchId = generateBatchId(record, i);
            batches.get(i).forEach(r -> r.setBatchId(batchId));
        }
        rewardRecordRepository.insertBatch(allRecords);
        log.info("燃烧分红共 {} 条记录入库完成，分 {} 批上链: recordId={}",
                allRecords.size(), batches.size(), record.getId());

        // --- 3. 分批发送 ---
        for (int i = 0; i < batches.size(); i++) {
            sendDividendBatch(generateBatchId(record, i), batches.get(i), record.getId(), i + 1, batches.size());
        }
    }

    /**
     * 发送单个分红批次：发交易 → 立即落库 txHash(SENT) → 返回
     * 确认由 BurnConfirmJob.reconcileBurnDividends() 轮询处理
     */
    public void sendDividendBatch(String batchId, List<RewardRecordDO> batch,
                                   Long recordId, int idx, int total) {
        long now = System.currentTimeMillis() / 1000;
        List<RewardItem> items = toRewardItems(batch);
        if (items.isEmpty()) {
            log.warn("批次无有效地址，跳过上链: batchId={}", batchId);
            return;
        }

        String txHash;
        try {
            txHash = dividendContractService.sendRewardBatch(batchId, items);
        } catch (Exception e) {
            log.error("分红批次发送失败，保持PENDING待重发: recordId={}, batchId={}", recordId, batchId, e);
            return;
        }

        // 立即落库 SENT，进程崩溃也能被 reconcileBurnDividends 接手
        rewardRecordRepository.markBatchSent(batchId, txHash, now);
        log.info("分红批次已发送，等待 BurnConfirmJob 确认: recordId={}, batchIndex={}/{}, batchId={}, txHash={}, count={}",
                recordId, idx, total, batchId, txHash, batch.size());
    }

    /**
     * 补偿任务：轮询所有 SENT 状态的燃烧分红批次
     * 链上已处理 → 标 PAID；交易失效且链上未处理 → 重发（同batchId幂等）
     */
    public void reconcileDividends() {
        List<RewardRecordDO> sent = rewardRecordRepository.findSentByRewardTypes(
                RewardType.STAKE_DIVIDEND.code,
                RewardType.LP_MINING.code,
                RewardType.BURN_DEPOSIT_WEIGHTED.code,
                RewardType.BURN_NODE_WEIGHTED.code,
                RewardType.BURN_PARTNER_EQUAL.code,
                RewardType.BURN_DYNAMIC_LEVEL.code,
                RewardType.DEPOSIT_NODE_REWARD.code,
                RewardType.DEPOSIT_MANAGEMENT_REWARD.code);
        if (sent.isEmpty()) return;

        log.info("补偿任务：发现 {} 条SENT状态燃烧分红记录", sent.size());
        long now = System.currentTimeMillis() / 1000;

        // 按batchId分组
        Map<String, List<RewardRecordDO>> grouped = sent.stream()
                .collect(Collectors.groupingBy(RewardRecordDO::getBatchId));

        for (Map.Entry<String, List<RewardRecordDO>> entry : grouped.entrySet()) {
            String batchId = entry.getKey();
            List<RewardRecordDO> records = entry.getValue();
            String txHash = records.getFirst().getTxHash();

            // 链上批次已确认 → 标PAID
            if (dividendContractService.isBatchProcessed(batchId)) {
                rewardRecordRepository.markBatchPaid(batchId, txHash, now);
                log.info("补偿：批次链上已确认，标记PAID: batchId={}, count={}", batchId, records.size());
                continue;
            }

            // 交易仍在内存池 → 继续等待
            if (dividendContractService.isTransactionAlive(txHash)) {
                log.debug("补偿：批次交易仍在池中，保持SENT: batchId={}, txHash={}", batchId, txHash);
                continue;
            }

            // 交易已失效且链上未处理 → 重发（同batchId，链上幂等）
            log.warn("补偿：批次交易失效，重新发送: batchId={}, oldTxHash={}", batchId, txHash);
            resendDividendBatch(batchId, records);
        }
    }

    /**
     * 重发批次（补偿任务专用）：覆盖旧txHash，不等确认（下次补偿任务继续查）
     */
    private void resendDividendBatch(String batchId, List<RewardRecordDO> records) {
        List<RewardItem> items = toRewardItems(records);
        if (items.isEmpty()) return;

        String newTxHash;
        try {
            newTxHash = dividendContractService.sendRewardBatch(batchId, items);
        } catch (Exception e) {
            log.error("补偿重发失败，保持SENT待下次重试: batchId={}", batchId, e);
            return;
        }

        rewardRecordRepository.markBatchSent(batchId, newTxHash, System.currentTimeMillis() / 1000);
        log.info("补偿重发成功，更新txHash: batchId={}, newTxHash={}", batchId, newTxHash);
    }

    /** 将 RewardItem 列表转为 RewardRecordDO 列表，businessId 存燃烧记录ID */
    private List<RewardRecordDO> toRewardRecords(Long burnRecordId, int rewardDate,
                                                 List<RewardItem> items, byte rewardType,
                                                 String remark, long now) {
        return items.stream().map(item -> {
            RewardRecordDO r = new RewardRecordDO();
            r.setUserId(item.getUserId());
            r.setAmount(item.getAmount());
            r.setRewardType(rewardType);
            r.setAssetType(AssetType.TIP.code);
            r.setBusinessId(burnRecordId);
            r.setRewardDate(rewardDate);
            r.setStatus(RewardRecordDO.Status.PENDING);
            r.setRemark(remark);
            r.setCreatedDate(now);
            r.setUpdatedDate(now);
            return r;
        }).collect(Collectors.toList());
    }

    /** RewardRecordDO 列表转换为上链 RewardItem（需要用户地址，从记录中读取） */
    private List<RewardItem> toRewardItems(List<RewardRecordDO> records) {
        // 批量查地址
        Set<Long> userIds = records.stream().map(RewardRecordDO::getUserId).collect(Collectors.toSet());
        Map<Long, String> addresses = userRepository.findAddressesByUserIds(userIds);

        return records.stream()
                .filter(r -> addresses.containsKey(r.getUserId()))
                .map(r -> RewardItem.builder()
                        .userId(r.getUserId())
                        .userAddress(addresses.get(r.getUserId()))
                        .amount(r.getAmount())
                        .rewardType(r.getRewardType())
                        .assetType(r.getAssetType())
                        .businessId(r.getBusinessId())
                        .build())
                .collect(Collectors.toList());
    }

    /** 通用分页切割 */
    private <T> List<List<T>> partition(List<T> list) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += RewardService.DIVIDEND_BATCH_SIZE) {
            result.add(list.subList(i, Math.min(i + RewardService.DIVIDEND_BATCH_SIZE, list.size())));
        }
        return result;
    }

    /**
     * 计算入金加权分红（60%）
     * 直接使用入金时预计算的 weighted_amount，避免实时计算
     */
    private List<RewardItem> calculateDepositWeightedDividend(BigDecimal totalAmount) {
        List<UserDepositDO> deposits = userDepositRepository.findByStatus(UserDepositDO.Status.COMPLETED);
        if (deposits.isEmpty()) {
            return Collections.emptyList();
        }

        // 总加权金额 = sum(weighted_amount)
        BigDecimal totalWeightedAmount = deposits.stream()
                .map(d -> d.getWeightedAmount() != null ? d.getWeightedAmount()
                        : d.getAmount().multiply(d.getWeight() != null ? d.getWeight() : BigDecimal.ONE))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeightedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        // 按userId聚合加权金额（一个用户可能有多笔入金）
        Map<Long, BigDecimal> userWeightedAmounts = deposits.stream()
                .collect(Collectors.groupingBy(
                        UserDepositDO::getUserId,
                        Collectors.mapping(
                                d -> d.getWeightedAmount() != null ? d.getWeightedAmount()
                                        : d.getAmount().multiply(d.getWeight() != null ? d.getWeight() : BigDecimal.ONE),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        Map<Long, String> userAddresses = userRepository.findAddressesByUserIds(userWeightedAmounts.keySet());

        return userWeightedAmounts.entrySet().stream()
                .filter(e -> userAddresses.containsKey(e.getKey()))
                .map(entry -> {
                    BigDecimal dividend = totalAmount.multiply(entry.getValue())
                            .divide(totalWeightedAmount, 18, RoundingMode.DOWN);
                    return RewardItem.builder()
                            .userId(entry.getKey())
                            .userAddress(userAddresses.get(entry.getKey()))
                            .amount(dividend)
                            .assetType(AssetType.TIP.code)
                            .remark("入金加权分红")
                            .build();
                })
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    /**
     * 计算节点加权分红（15%）
     * 黄金:1, 钻石:3, 皇冠:10
     */
    private List<RewardItem> calculateNodeWeightedDividend(BigDecimal totalAmount) {
        // 定义节点权重
        Map<Integer, Integer> nodeWeights = new HashMap<>();
        nodeWeights.put(UserDO.Role.GOLD, 1);
        nodeWeights.put(UserDO.Role.DIAMOND, 3);
        nodeWeights.put(UserDO.Role.CROWN, 10);

        // 查询所有节点用户（假设通过UserRepository查询，需要添加相应方法）
        List<UserDO> nodes = userRepository.findByRoles(Arrays.asList(
                UserDO.Role.GOLD, UserDO.Role.DIAMOND, UserDO.Role.CROWN));

        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算总权重
        int totalWeight = nodes.stream()
                .mapToInt(u -> nodeWeights.getOrDefault(u.getRole(), 0))
                .sum();

        if (totalWeight == 0) {
            return Collections.emptyList();
        }

        // 生成分红列表
        return nodes.stream()
                .filter(u -> u.getAddress() != null && !u.getAddress().isEmpty())
                .map(user -> {
                    int weight = nodeWeights.getOrDefault(user.getRole(), 0);
                    BigDecimal dividend = totalAmount.multiply(new BigDecimal(weight))
                            .divide(new BigDecimal(totalWeight), 18, RoundingMode.DOWN);

                    return RewardItem.builder()
                            .userId(user.getId())
                            .userAddress(user.getAddress())
                            .amount(dividend)
                            .assetType(AssetType.TIP.code)
                            .remark("节点加权分红")
                            .build();
                })
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    /**
     * 计算合伙人平均分红（6%）
     */
    private List<RewardItem> calculatePartnerEqualDividend(BigDecimal totalAmount) {
        // 查询所有合伙人
        List<UserDO> partners = userRepository.findByRole(UserDO.Role.PARTNER);

        if (partners.isEmpty()) {
            return Collections.emptyList();
        }

        // 平均分配
        BigDecimal perPartnerAmount = totalAmount.divide(
                new BigDecimal(partners.size()), 18, RoundingMode.DOWN);

        return partners.stream()
                .filter(u -> u.getAddress() != null && !u.getAddress().isEmpty())
                .map(user -> RewardItem.builder()
                        .userId(user.getId())
                        .userAddress(user.getAddress())
                        .amount(perPartnerAmount)
                        .assetType(AssetType.TIP.code)
                        .remark("合伙人平均分红")
                        .build())
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    /**
     * 计算动态分币（14%，8个等级，每等级1.75%）
     * 每个等级内平均分配，返回 RewardRecord 以保留等级信息（remark）
     */
    private List<RewardRecordDO> calculateDynamicLevelRewardRecords(
            Long burnRecordId, int rewardDate, BigDecimal totalAmount, long now) {
        List<RewardRecordDO> records = new ArrayList<>();
        BigDecimal perLevelAmount = totalAmount.divide(new BigDecimal("8"), 18, RoundingMode.DOWN);

        for (int level = 1; level <= 8; level++) {
            List<Long> usersAtLevel = userRepository.findUserIdsByLevel(level);
            if (usersAtLevel.isEmpty()) continue;

            BigDecimal perUserAmount = perLevelAmount.divide(
                    new BigDecimal(usersAtLevel.size()), 18, RoundingMode.DOWN);
            if (perUserAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            for (Long userId : usersAtLevel) {
                RewardRecordDO r = new RewardRecordDO();
                r.setUserId(userId);
                r.setAmount(perUserAmount);
                r.setRewardType(RewardType.BURN_DYNAMIC_LEVEL.code);
                r.setAssetType(AssetType.TIP.code);
                r.setBusinessId(burnRecordId);
                r.setRewardDate(rewardDate);
                r.setStatus(RewardRecordDO.Status.PENDING);
                r.setRemark("动态分币-L" + level);
                r.setCreatedDate(now);
                r.setUpdatedDate(now);
                records.add(r);
            }
        }
        return records;
    }

    /**
     * 生成批次ID（含批次序号，用于幂等上链）
     */
    private String generateBatchId(TipBurnRecordDO record, int batchIndex) {
        String data = "BURN_DIVIDEND_" + record.getId() + "_" + record.getBurnDate() + "_" + batchIndex;
        byte[] hash = org.web3j.crypto.Hash.sha3(data.getBytes());
        return Numeric.toHexString(hash);
    }

    // ========== 入金分红 ==========

    /**
     * 入金分红：见点奖励(32%) + 管理奖励(42%)
     * DAO/保险池/系统运营由合约直接处理，不在此分配
     */
    @Transactional
    public void distributeDepositDividend(String userAddress, Long userId,
                                          BigDecimal toDividend, Long depositId) {
        log.info("开始分配入金分红: depositId={}, userId={}, toDividend={}", depositId, userId, toDividend);

        // 幂等：以见点奖励类型作为代表判断是否已处理
        if (!rewardRecordRepository.findByBusinessIdAndRewardType(
                depositId, RewardType.DEPOSIT_NODE_REWARD.code).isEmpty()) {
            log.warn("入金分红记录已存在，跳过重复分配: depositId={}", depositId);
            return;
        }

        if (toDividend == null || toDividend.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("入金分红金额为0，跳过: depositId={}", depositId);
            return;
        }

        long now = System.currentTimeMillis() / 1000;
        int rewardDate = Integer.parseInt(new java.text.SimpleDateFormat("yyyyMMdd")
                .format(new java.util.Date(now * 1000)));

        List<Long> ancestors = closureRepository.findAncestorIds(userId);
        List<RewardRecordDO> allRecords = new ArrayList<>();

        // --- 1. 见点奖励 (32%，最多16层，每层2%) ---
        BigDecimal perLayer = toDividend.multiply(new BigDecimal("0.02"));
        int layerCount = 0;

        for (Long ancestorId : ancestors) {
            if (layerCount >= 16) break;

            // 无效用户（无COMPLETED入金）自动跳过，不占层数
            if (userDepositRepository.countActiveOrders(ancestorId) == 0) continue;

            layerCount++;
            int maxLayers = Math.min(closureRepository.countValidDirectChildren(ancestorId) * 2, 16);
            if (layerCount <= maxLayers) {
                BigDecimal rewardAmt = perLayer.setScale(6, RoundingMode.DOWN);
                if (userRelationService.tryConsumeEnergy(ancestorId, rewardAmt, depositId,
                        "消耗能量-见点奖励-L" + layerCount)) {
                    continue;
                }
                allRecords.add(buildRewardRecord(depositId, rewardDate, ancestorId, rewardAmt,
                        RewardType.DEPOSIT_NODE_REWARD.code, AssetType.USDC.code,
                        "入金见点奖励-L" + layerCount, now));
            }
        }

        // --- 2. 管理奖励 (42%，级差制度，S1-S7) ---
        BigDecimal mgmtPool = toDividend.multiply(new BigDecimal("0.42"));
        BigDecimal maxRateBelow = BigDecimal.ZERO;
        BigDecimal totalMgmtDistributed = BigDecimal.ZERO;

        for (Long ancestorId : ancestors) {
            if (totalMgmtDistributed.compareTo(mgmtPool) >= 0) break;

            // 无效用户不参与管理奖励
            if (userDepositRepository.countActiveOrders(ancestorId) == 0) continue;

            UserPerformanceDO perf = performanceRepository.findByUserId(ancestorId);
            BigDecimal communityVol = (perf != null && perf.getCommunityVolumeUsdt() != null)
                    ? perf.getCommunityVolumeUsdt() : BigDecimal.ZERO;

            BigDecimal ownRate = getSLevelRate(communityVol);
            if (ownRate.compareTo(maxRateBelow) > 0) {
                BigDecimal differential = ownRate.subtract(maxRateBelow);
                BigDecimal earn = toDividend.multiply(differential)
                        .setScale(6, RoundingMode.DOWN);
                BigDecimal remaining = mgmtPool.subtract(totalMgmtDistributed);
                if (earn.compareTo(remaining) > 0) earn = remaining;

                if (earn.compareTo(BigDecimal.ZERO) > 0) {
                    if (userRelationService.tryConsumeEnergy(ancestorId, earn, depositId,
                            "消耗能量-管理奖励-" + getSLevelName(communityVol))) {
                        maxRateBelow = ownRate;
                        continue;
                    }
                    allRecords.add(buildRewardRecord(depositId, rewardDate, ancestorId, earn,
                            RewardType.DEPOSIT_MANAGEMENT_REWARD.code, AssetType.USDC.code,
                            "入金管理奖励-" + getSLevelName(communityVol), now));
                    totalMgmtDistributed = totalMgmtDistributed.add(earn);
                }
                maxRateBelow = ownRate;
            }
        }

        if (allRecords.isEmpty()) {
            log.warn("无有效分红对象，跳过入库和上链: depositId={}", depositId);
            return;
        }

        // --- 3. 分批，分配 batchId，全量入库（PENDING） ---
        List<List<RewardRecordDO>> batches = partition(allRecords);
        for (int i = 0; i < batches.size(); i++) {
            String batchId = generateDepositBatchId(depositId, i);
            batches.get(i).forEach(r -> r.setBatchId(batchId));
        }
        rewardRecordRepository.insertBatch(allRecords);
        log.info("入金分红共 {} 条记录入库完成，分 {} 批上链: depositId={}",
                allRecords.size(), batches.size(), depositId);

        // --- 4. 分批发送 ---
        for (int i = 0; i < batches.size(); i++) {
            sendDividendBatch(generateDepositBatchId(depositId, i), batches.get(i), depositId, i + 1, batches.size());
        }
    }

    private RewardRecordDO buildRewardRecord(Long businessId, int rewardDate, Long userId,
                                              BigDecimal amount, byte rewardType, byte assetType,
                                              String remark, long now) {
        RewardRecordDO r = new RewardRecordDO();
        r.setUserId(userId);
        r.setAmount(amount);
        r.setRewardType(rewardType);
        r.setAssetType(assetType);
        r.setBusinessId(businessId);
        r.setRewardDate(rewardDate);
        r.setStatus(RewardRecordDO.Status.PENDING);
        r.setRemark(remark);
        r.setCreatedDate(now);
        r.setUpdatedDate(now);
        return r;
    }

    /** S级管理奖励比例（基于小区业绩） */
    private BigDecimal getSLevelRate(BigDecimal communityVol) {
        if (communityVol.compareTo(new BigDecimal("3000000")) >= 0) return new BigDecimal("0.42");
        if (communityVol.compareTo(new BigDecimal("1500000")) >= 0) return new BigDecimal("0.36");
        if (communityVol.compareTo(new BigDecimal("500000")) >= 0) return new BigDecimal("0.30");
        if (communityVol.compareTo(new BigDecimal("150000")) >= 0) return new BigDecimal("0.24");
        if (communityVol.compareTo(new BigDecimal("50000")) >= 0) return new BigDecimal("0.18");
        if (communityVol.compareTo(new BigDecimal("15000")) >= 0) return new BigDecimal("0.12");
        if (communityVol.compareTo(new BigDecimal("5000")) >= 0) return new BigDecimal("0.06");
        return BigDecimal.ZERO;
    }

    public String getSLevelName(BigDecimal communityVol) {
        if (communityVol.compareTo(new BigDecimal("3000000")) >= 0) return "S7";
        if (communityVol.compareTo(new BigDecimal("1500000")) >= 0) return "S6";
        if (communityVol.compareTo(new BigDecimal("500000")) >= 0) return "S5";
        if (communityVol.compareTo(new BigDecimal("150000")) >= 0) return "S4";
        if (communityVol.compareTo(new BigDecimal("50000")) >= 0) return "S3";
        if (communityVol.compareTo(new BigDecimal("15000")) >= 0) return "S2";
        return "S1";
    }

    private String generateDepositBatchId(Long depositId, int batchIndex) {
        String data = "DEPOSIT_DIVIDEND_" + depositId + "_" + batchIndex;
        byte[] hash = org.web3j.crypto.Hash.sha3(data.getBytes());
        return Numeric.toHexString(hash);
    }

}
