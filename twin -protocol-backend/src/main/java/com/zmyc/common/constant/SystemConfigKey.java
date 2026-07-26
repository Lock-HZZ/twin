package com.zmyc.common.constant;

public class SystemConfigKey {

    /** 入金能量倍率：能量值 = 入金金额(USDC) * 倍率 */
    public static final String DEPOSIT_ENERGY_MULTIPLIER = "deposit.energy.multiplier";

    /** 卡牌矿机单价（USDC） */
    public static final String MINER_CARD_PRICE = "miner.card.price";

    /** 矿机挖矿天数（到期后可合成） */
    public static final String MINER_MINING_DAYS = "miner.mining.days";

    /** 每日最大入金额度（USDC） */
    public static final String DAILY_MAX_DEPOSIT = "daily.max.deposit";

    /** 允许入金的用户角色（逗号分隔：0-普通用户,1-黄金节点,2-钻石节点,3-皇冠节点,4-合伙人） */
    public static final String ALLOWED_DEPOSIT_LEVELS = "allowed.deposit.levels";

    /** 权重增长率（每天） */
    public static final String WEIGHT_GROWTH_RATE = "weight.growth.rate";

    /** 质押年化收益率配置前缀（stake.apy.30, stake.apy.90, stake.apy.180, stake.apy.360） */
    public static final String STAKE_APY_PREFIX = "stake.apy.";
    public static final String STAKE_APY_30 = "stake.apy.30";
    public static final String STAKE_APY_90 = "stake.apy.90";
    public static final String STAKE_APY_180 = "stake.apy.180";
    public static final String STAKE_APY_360 = "stake.apy.360";

    private SystemConfigKey() {
    }
}
