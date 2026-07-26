package com.zmyc.service;

import com.zmyc.common.constant.SystemConfigKey;
import com.zmyc.common.util.BatchUtils;
import com.zmyc.common.util.TimeUtils;
import com.zmyc.infrastructure.entity.BalanceTransactionDO;
import com.zmyc.infrastructure.entity.BlockchainTokenDO;
import com.zmyc.infrastructure.entity.DailyDividendDO;
import com.zmyc.infrastructure.entity.UserBalanceDO;
import com.zmyc.infrastructure.entity.UserDividendDetailDO;
import com.zmyc.infrastructure.entity.UserMinerDO;
import com.zmyc.infrastructure.mapper.UserBalanceMapper;
import com.zmyc.infrastructure.mapper.UserMinerMapper;
import com.zmyc.infrastructure.repository.BalanceTransactionRepository;
import com.zmyc.infrastructure.repository.BlockchainTokenRepository;
import com.zmyc.infrastructure.repository.DailyDividendRepository;
import com.zmyc.infrastructure.repository.SystemConfigRepository;
import com.zmyc.infrastructure.repository.UserBalanceRepository;
import com.zmyc.infrastructure.repository.UserDividendDetailRepository;
import com.zmyc.infrastructure.repository.UserMinerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DividendService {

    @Autowired
    private UserMinerMapper minerMapper;

    @Autowired
    private UserMinerRepository minerRepository;

    @Autowired
    private DailyDividendRepository dividendRepository;

    @Autowired
    private UserDividendDetailRepository detailRepository;

    /**
     * 每日分红任务，每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void dailyDividendTask() {
        LocalDate today = LocalDate.now();
        log.info("开始执行每日分红任务，分红日期: {}", today);
        try {
            executeDailyDividend(today);
            log.info("每日分红任务执行成功，分红日期: {}", today);
        } catch (Exception e) {
            log.error("每日分红任务执行失败，分红日期: {}", today, e);
            throw e;
        }
    }

    /**
     * 执行每日分红
     */
    @Transactional
    public void executeDailyDividend(LocalDate dividendDate) {
        // 检查是否已执行过
        DailyDividendDO existingDividend = dividendRepository.findByDate(dividendDate);
        if (existingDividend != null) {
            log.error("分红记录已存在，跳过执行: date={}", dividendDate);
            return;
        }

        // 从代币表获取 TIP 代币信息
        // BlockchainTokenDO tipToken = tokenRepository.findBySymbol("TIP");
        // Long tokenId = tipToken.getId();

        // 获取每日分红 TIP 总量
        BigDecimal totalTipAmount = getDailyDividendTipAmount();

        // 统计所有挖矿中的矿机（按用户分组）
        List<UserMinerMapper.UserMinerCountDTO> userMinerCounts = minerMapper.countByUserIdForMining(TimeUtils.getTodayZeroTimestamp());
        int totalMinerCount = userMinerCounts.stream().mapToInt(c -> c.minerCount).sum();
        if (totalMinerCount == 0) {
            log.error("没有挖矿中的矿机，跳过分红: date={}", dividendDate);
            return;
        }

        // 计算每张矿机分红金额
        BigDecimal perMinerAmount = totalTipAmount.divide(
                BigDecimal.valueOf(totalMinerCount), 8, RoundingMode.DOWN);

        // 创建分红汇总记录
        DailyDividendDO dividend = new DailyDividendDO();
        dividend.setDividendDate(dividendDate);
        dividend.setTotalTipAmount(totalTipAmount);
        dividend.setTotalMinerCount(totalMinerCount);
        dividend.setPerMinerAmount(perMinerAmount);
        dividend.setStatus(DailyDividendDO.Status.PENDING);
        dividendRepository.save(dividend);

        long now = System.currentTimeMillis() / 1000;

        // 批量查询所有参与分红用户的当前余额（用于 balance_before）
        List<Long> userIds = userMinerCounts.stream().map(c -> c.userId).toList();
      /*  Map<Long, BigDecimal> balanceBeforeMap = balanceRepository
                .findByTokenIdAndUserIds(tokenId, userIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        UserBalanceDO::getUserId, UserBalanceDO::getBalance));*/

        // 纯内存计算，构建三张表的数据
        List<UserDividendDetailDO> details = new ArrayList<>();
     /*   List<UserBalanceDO> balanceUpdates = new ArrayList<>();
        List<BalanceTransactionDO> txList = new ArrayList<>();*/

        for (UserMinerMapper.UserMinerCountDTO countDTO : userMinerCounts) {
            BigDecimal userTipAmount = perMinerAmount.multiply(BigDecimal.valueOf(countDTO.minerCount));
            // BigDecimal balanceBefore = balanceBeforeMap.getOrDefault(countDTO.userId, BigDecimal.ZERO);

            UserDividendDetailDO detail = new UserDividendDetailDO();
            detail.setDividendId(dividend.getId());
            detail.setUserId(countDTO.userId);
            detail.setMinerCount(countDTO.minerCount);
            detail.setTipAmount(userTipAmount);
            detail.setStatus(UserDividendDetailDO.Status.CLAIMED);
            detail.setCreatedDate(now);
            detail.setLastUpdatedDate(now);
            details.add(detail);
/*
            UserBalanceDO balanceUpdate = new UserBalanceDO();
            balanceUpdate.setUserId(countDTO.userId);
            balanceUpdate.setTokenId(tokenId);
            balanceUpdate.setBalance(userTipAmount);
            balanceUpdates.add(balanceUpdate);

            BalanceTransactionDO tx = new BalanceTransactionDO();
            tx.setUserId(countDTO.userId);
            tx.setTokenId(tokenId);
            tx.setTxType(BalanceTransactionDO.TxType.DIVIDEND_IN);
            tx.setAmount(userTipAmount);
            tx.setBalanceBefore(balanceBefore);
            tx.setBalanceAfter(balanceBefore.add(userTipAmount));
            tx.setRelatedId(dividend.getId());
            tx.setRemark("每日挖矿分红: " + dividendDate + ", " + countDTO.minerCount + "张矿机");
            tx.setCreatedDate(now);
            tx.setLastUpdatedDate(now);
            txList.add(tx);*/
        }

        // 批量写库：全部分批执行，每批500条
        BatchUtils.execute(details, detailRepository::batchSave);
       /* BatchUtils.execute(balanceUpdates, batch -> balanceMapper.batchAddBalance(batch, tokenId, now));
        BatchUtils.execute(txList, txRepository::batchSave);*/

        // 更新分红汇总状态为已发放
        dividend.setStatus(DailyDividendDO.Status.DISTRIBUTED);
        dividend.setDistributedDate(now);
        dividendRepository.save(dividend);

        log.info("分红发放完成: date={}, totalMiners={}, totalUsers={}, perMiner={}",
                dividendDate, totalMinerCount, userMinerCounts.size(), perMinerAmount);
    }

    /**
     * 矿机到期状态更新，每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void updateExpiredMinersTask() {
        long now = System.currentTimeMillis() / 1000;
        log.info("开始执行矿机到期状态更新任务");

        List<UserMinerDO> expiredMiners = minerRepository.findMiningAndExpired(now);
        for (UserMinerDO miner : expiredMiners) {
            miner.setStatus(UserMinerDO.Status.EXPIRED);
            minerRepository.save(miner);
            log.info("矿机已到期: id={}, userId={}, type={}", miner.getId(), miner.getUserId(), miner.getMinerType());
        }

        log.info("矿机到期状态更新完成，更新数量: {}", expiredMiners.size());
    }

    private BigDecimal getDailyDividendTipAmount() {
        return BigDecimal.valueOf(10000.0); //TODO: 这里可以从系统配置中获取每日分红 TIP 总量，暂时写死为10000
    }
}
