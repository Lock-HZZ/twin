package com.zmyc.domain.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户入金额度信息 BO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuotaInfo {

    /**
     * 用户总额度（USDC）
     */
    private BigDecimal totalQuota;

    /**
     * 已占用额度（USDC）
     */
    private BigDecimal usedQuota;

    /**
     * 可用额度（USDC）
     */
    private BigDecimal availableQuota;

    /**
     * 每日最大入金总额度（USDC）
     */
    private BigDecimal dailyMaxDeposit;

    /**
     * 今日已用总额度（USDC）
     */
    private BigDecimal dailyUsed;

    /**
     * 今日剩余总额度（USDC）
     */
    private BigDecimal dailyRemaining;
}
