package com.zmyc.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 奖励项通用DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardItem {
    /** 用户ID */
    private Long userId;

    /** 用户钱包地址 */
    private String userAddress;

    /** 奖励金额（业务精度，如 100.5 TIP） */
    private BigDecimal amount;

    /** 奖励类型：1-质押分红, 2-推荐奖励, 3-LP挖矿 */
    private Byte rewardType;

    /** 资产类型：0-USDC, 1-TIP */
    private Byte assetType;

    /** 业务关联ID（如质押记录ID、推荐记录ID等） */
    private Long businessId;

    /** 备注信息 */
    private String remark;
}
