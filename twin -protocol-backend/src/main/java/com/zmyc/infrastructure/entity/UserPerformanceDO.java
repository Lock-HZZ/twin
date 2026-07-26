package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("user_performance")
public class UserPerformanceDO {

    @TableId(type = IdType.INPUT)
    private Long userId;

    /** 个人业绩（自身入金总额） */
    private BigDecimal personalVolumeUsdt;

    /** 团队业绩（所有下级入金总额，不含自身） */
    private BigDecimal teamVolumeUsdt;

    /** 小区业绩 = 所有直推线业绩之和 - 大区业绩（缓存值，入金后更新） */
    private BigDecimal communityVolumeUsdt;

    private Long createdDate;

    private Long lastUpdatedDate;
}
