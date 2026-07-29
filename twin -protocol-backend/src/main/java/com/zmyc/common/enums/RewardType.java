package com.zmyc.common.enums;

import com.zmyc.service.DividendContractService;

public enum RewardType {

    STAKE_DIVIDEND((byte) 1),
    REFERRAL_REWARD((byte) 2),
    LP_MINING((byte) 3),
    BURN_DEPOSIT_WEIGHTED((byte) 4),   // 燃烧分红-入金加权60%
    BURN_NODE_WEIGHTED((byte) 5),       // 燃烧分红-节点加权15%
    BURN_PARTNER_EQUAL((byte) 6),       // 燃烧分红-合伙人平均6%
    BURN_DYNAMIC_LEVEL((byte) 7),       // 燃烧分红-动态分币14%
    DEPOSIT_NODE_REWARD((byte) 8),      // 入金分红-见点奖励32%
    DEPOSIT_MANAGEMENT_REWARD((byte) 9); // 入金分红-管理奖励42%

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
