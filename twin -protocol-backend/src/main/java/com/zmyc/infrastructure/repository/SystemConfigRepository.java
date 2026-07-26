package com.zmyc.infrastructure.repository;

import com.zmyc.common.constant.SystemConfigKey;
import com.zmyc.infrastructure.entity.SystemConfigDO;
import com.zmyc.infrastructure.mapper.SystemConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class SystemConfigRepository {

    @Autowired
    private SystemConfigMapper configMapper;

    /** 获取入金能量倍率 */
    public BigDecimal getDepositEnergyMultiplier() {
        SystemConfigDO config = configMapper.findByKey(SystemConfigKey.DEPOSIT_ENERGY_MULTIPLIER);
        if (config == null) {
            return BigDecimal.valueOf(3.0); // 默认倍率3
        }
        try {
            return new BigDecimal(config.getConfigValue());
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(3.0);
        }
    }

    /** 获取矿机单价（USDC） */
    public BigDecimal getMinerCardPrice() {
        SystemConfigDO config = configMapper.findByKey(SystemConfigKey.MINER_CARD_PRICE);
        if (config == null) {
            return BigDecimal.valueOf(100.0);
        }
        try {
            return new BigDecimal(config.getConfigValue());
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(100.0);
        }
    }

    /** 获取矿机挖矿天数 */
    public int getMinerMiningDays() {
        SystemConfigDO config = configMapper.findByKey(SystemConfigKey.MINER_MINING_DAYS);
        if (config == null) {
            return 30; // 默认30天
        }
        try {
            return Integer.parseInt(config.getConfigValue());
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    /** 获取每日最大入金额度（USDC） */
    public BigDecimal getDailyMaxDeposit() {
        SystemConfigDO config = configMapper.findByKey(SystemConfigKey.DAILY_MAX_DEPOSIT);
        if (config == null) {
            return BigDecimal.valueOf(50000); // 默认50000
        }
        try {
            return new BigDecimal(config.getConfigValue());
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(50000);
        }
    }

    /** 获取允许入金的用户等级列表 */
    public String getAllowedDepositLevels() {
        SystemConfigDO config = configMapper.findByKey(SystemConfigKey.ALLOWED_DEPOSIT_LEVELS);
        if (config == null) {
            return "0,1,2,3,4"; // 默认所有等级
        }
        return config.getConfigValue();
    }

    /** 获取权重增长率 */
    public BigDecimal getWeightGrowthRate() {
        SystemConfigDO config = configMapper.findByKey(SystemConfigKey.WEIGHT_GROWTH_RATE);
        if (config == null) {
            return BigDecimal.valueOf(0.01); // 默认1%
        }
        try {
            return new BigDecimal(config.getConfigValue());
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(0.01);
        }
    }

    /** 根据key查询配置值并转换为BigDecimal，找不到或解析失败时返回defaultValue */
    public BigDecimal getConfigValueAsBigDecimal(String configKey, BigDecimal defaultValue) {
        SystemConfigDO config = configMapper.findByKey(configKey);
        if (config == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(config.getConfigValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 根据key查询配置 */
    public SystemConfigDO findByKey(String configKey) {
        return configMapper.findByKey(configKey);
    }

    /** 保存配置 */
    public void save(SystemConfigDO config) {
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
    }
}
