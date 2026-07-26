package com.zmyc.service;

import com.zmyc.common.util.TimeUtils;
import com.zmyc.infrastructure.entity.*;
import com.zmyc.infrastructure.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Service
public class DepositService {

    private static final Logger log = LoggerFactory.getLogger(DepositService.class);

    @Autowired
    private UserDepositRepository depositRepository;

    @Autowired
    private UserMinerRepository minerRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private MinerZodiacConfigRepository zodiacConfigRepository;

    @Autowired
    private UserRelationService userRelationService;

    private final Random random = new Random();

    /**
     * 处理链上入金事件
     *
     * @param userId 用户ID
     * @param amount 入金数量（USDC）
     * @param txHash 交易哈希
     */
    @Transactional
    public void processDeposit(Long userId, BigDecimal amount, String txHash) {

        // 检查交易是否已处理
        UserDepositDO existingDeposit = depositRepository.findByTxHash(txHash);
        if (existingDeposit != null) {
            log.warn("入金交易已存在，跳过处理: txHash={}", txHash);
            return;
        }

        // 获取矿机单价和能量倍率
        BigDecimal minerPrice = systemConfigRepository.getMinerCardPrice();
        BigDecimal energyMultiplier = systemConfigRepository.getDepositEnergyMultiplier();

        // 计算可购买的矿机数量
        int minerCount = amount.divide(minerPrice, 0, RoundingMode.DOWN).intValue();

        // 计算能量值
        BigDecimal energyEarned = amount.multiply(energyMultiplier);

        // 创建入金记录
        UserDepositDO deposit = new UserDepositDO();
        deposit.setUserId(userId);
        deposit.setAmount(amount);
        deposit.setTxHash(txHash);
        deposit.setMinerCount(minerCount);
        deposit.setMinerPriceUSDC(minerPrice);
        deposit.setEnergyEarned(energyEarned);
        deposit.setEnergyMultiplier(energyMultiplier);
        depositRepository.save(deposit);

        log.info("创建入金记录: userId={}, amount={} USDC, minerCount={}, energyEarned={}",
                userId, amount, minerCount, energyEarned);

        // 抽取星座矿机
        if (minerCount > 0) {
            drawZodiacMiners(userId, deposit.getId(), minerCount, minerPrice);
        }

        // 触发业绩统计和能量值增加
        userRelationService.onDeposit(userId, amount, deposit.getId());

        log.info("入金处理完成: userId={}, txHash={}", userId, txHash);
    }

    /**
     * 抽取星座矿机（按概率）
     */
    private void drawZodiacMiners(Long userId, Long depositId, int count, BigDecimal pricePerMiner) {
        long now = System.currentTimeMillis() / 1000;

        // 查询所有启用的星座配置
        List<MinerZodiacConfigDO> zodiacConfigs = zodiacConfigRepository.findAllEnabled();
        if (zodiacConfigs.isEmpty()) {
            log.error("未找到启用的星座配置，无法抽取矿机");
            return;
        }

        // 获取挖矿天数配置
        int miningDays = systemConfigRepository.getMinerMiningDays();

        long activatedDate = TimeUtils.getTodayEndTimestamp();
        long expiredDate = activatedDate + (miningDays * 24 * 60 * 60L);

        // 构建概率区间
        BigDecimal totalRate = BigDecimal.ZERO;
        for (MinerZodiacConfigDO config : zodiacConfigs) {
            totalRate = totalRate.add(config.getDropRate());
        }

        for (int i = 0; i < count; i++) {
            // 生成随机数 [0, 100)
            BigDecimal randomValue = BigDecimal.valueOf(random.nextDouble() * 100);

            // 根据概率区间选择星座
            BigDecimal accumulated = BigDecimal.ZERO;
            MinerZodiacConfigDO selectedZodiac = null;

            for (MinerZodiacConfigDO config : zodiacConfigs) {
                accumulated = accumulated.add(config.getDropRate());
                if (randomValue.compareTo(accumulated) < 0) {
                    selectedZodiac = config;
                    break;
                }
            }

            if (selectedZodiac == null) {
                selectedZodiac = zodiacConfigs.getFirst();
            }

            // 创建矿机
            UserMinerDO miner = new UserMinerDO();
            miner.setUserId(userId);
            miner.setMinerType(selectedZodiac.getZodiacType());
            miner.setPurchasePrice(pricePerMiner);
            miner.setDepositId(depositId);
            miner.setIsSynthesized(false);
            miner.setStatus(UserMinerDO.Status.MINING);
            miner.setActivatedDate(activatedDate);
            miner.setExpiredDate(expiredDate);
            miner.setTotalMined(BigDecimal.ZERO);
            minerRepository.save(miner);

            log.info("抽取矿机: userId={}, zodiacType={}, zodiacName={}, activatedDate={}, expiredDate={}, depositId={}",
                    userId, selectedZodiac.getZodiacType(), selectedZodiac.getZodiacName(), activatedDate, expiredDate, depositId);
        }
    }

}
