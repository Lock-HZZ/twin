package com.zmyc.application.vo.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AssetRecordResponse {

    /** 记录来源类型：ENERGY_EARN / ENERGY_CONSUME / USDC_REWARD / USDC_DEPOSIT / TIP_STAKE / TIP_REWARD */
    private String category;

    /** 资产类型：ENERGY / USDC / TIP */
    private String assetType;

    /** 金额（正数=收入，负数=支出） */
    private BigDecimal amount;

    /** 变动前余额（能量明细专用，其余为null） */
    private BigDecimal balanceBefore;

    /** 变动后余额（能量明细专用，其余为null） */
    private BigDecimal balanceAfter;

    /** 备注/来源说明 */
    private String remark;

    /** 关联记录ID */
    private Long relatedId;

    /** 创建时间（秒级时间戳） */
    private Long createdDate;
}
