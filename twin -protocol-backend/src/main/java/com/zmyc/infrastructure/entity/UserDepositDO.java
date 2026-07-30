package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_deposit")
public class UserDepositDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private BigDecimal amount;

    private String txHash;

    private BigDecimal energyEarned;

    private BigDecimal energyMultiplier;

    private BigDecimal liquidity;

    /** 权重（根据注册天数计算） */
    private BigDecimal weight;

    /** 加权金额 = amount * weight（入金时预计算，供分佣直接使用） */
    private BigDecimal weightedAmount;

    /** 状态：0-待处理，1-已完成，2-失败 */
    private Integer status;

    /** 随机数 */
    private Long nonce;

    /** 过期时间（10位时间戳），PENDING 状态超过此时间自动变为 EXPIRED */
    private Long expiresAt;

    /** 移除LP的交易哈希（REMOVING状态后填充，用于回执轮询） */
    private String withdrawTxHash;

    public static class Status {
        public static final Integer PENDING = 0;
        public static final Integer COMPLETED = 1;
        public static final Integer EXPIRED = 2;
        public static final Integer REMOVING = 3;
        public static final Integer REMOVED = 4;
    }
}
