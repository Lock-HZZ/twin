package com.zmyc.application.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户入金额度信息响应
 */
@Data
@Schema(description = "用户入金额度信息")
public class QuotaInfoResponse {

    @Schema(description = "用户总额度（USDC）")
    private BigDecimal totalQuota;

    @Schema(description = "已占用额度（USDC）")
    private BigDecimal usedQuota;

    @Schema(description = "可用额度（USDC）")
    private BigDecimal availableQuota;

    @Schema(description = "每日最大入金总额度（USDC）")
    private BigDecimal dailyMaxDeposit;

    @Schema(description = "今日已用总额度（USDC）")
    private BigDecimal dailyUsed;

    @Schema(description = "今日剩余总额度（USDC）")
    private BigDecimal dailyRemaining;
}
