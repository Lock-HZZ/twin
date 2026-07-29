package com.zmyc.infrastructure.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 资产明细统一投影 DTO（UNION ALL 查询结果映射） */
@Data
public class AssetRecordDTO {

    private String category;

    /** ENERGY / USDC / TIP */
    private String assetType;

    /** 金额（正=收入，负=支出） */
    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String remark;

    private Long relatedId;

    private Long createdDate;
}
