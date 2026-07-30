package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * LP挖矿奖励释放记录
 * 每次移除LP后生成60条记录（对应60天），每天释放 totalAmount / 60
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lp_reward_release")
public class LpRewardReleaseDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联的入金订单ID（businessId，对应 user_deposit.id） */
    private Long depositId;

    /** 移除LP的交易哈希（业务幂等键，同一笔removeLiquidity只生成一次60条记录） */
    private String removeTxHash;

    /** 总奖励金额（TIP，60天全部释放后的总额） */
    private BigDecimal totalAmount;

    /** 每日释放金额（totalAmount / 60） */
    private BigDecimal dailyAmount;

    /** 释放日期（yyyyMMdd格式，如 20260729） */
    private Integer releaseDate;

    /** 已释放金额（通常为 0 或 dailyAmount） */
    private BigDecimal releasedAmount;

    /** 状态：0-待释放，1-已释放到reward_record */
    private Integer status;

    /** 关联的 reward_record.batch_id（释放后填充） */
    private String batchId;

    public static class Status {
        public static final Integer PENDING = 0;
        public static final Integer RELEASED = 1;
    }
}
