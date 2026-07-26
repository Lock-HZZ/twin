package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("user_stake")
public class UserStakeDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 链上质押ID */
    private Long stakeId;

    /** 质押数量（TIP） */
    private BigDecimal amount;

    /** 质押套餐（天数：30/90/180/360） */
    private Integer plan;

    /** 年化收益率（例如：5.0 表示 5%） */
    private BigDecimal apy;

    /** 开始时间（秒） */
    private Long startTime;

    /** 到期时间（秒） */
    private Long endTime;

    /** 状态：0-进行中，1-已赎回 */
    private Integer status;

    /** 交易哈希 */
    private String txHash;

    /** 创建时间（秒） */
    private Long createdDate;

    /** 更新时间（秒） */
    private Long updatedDate;

    /** 状态常量 */
    public static class Status {
        public static final int ACTIVE = 0;
        public static final int WITHDRAWN = 1;
    }
}
