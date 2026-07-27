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

    private Integer minerCount;

    private BigDecimal energyEarned;

    private BigDecimal energyMultiplier;

    /** 权重（根据注册天数计算） */
    private BigDecimal weight;

    /** 状态：0-待处理，1-已完成，2-失败 */
    private Integer status;

    /** 随机数 */
    private Long nonce;

    /** 过期时间（10位时间戳），PENDING 状态超过此时间自动变为 EXPIRED */
    private Long expiresAt;

    public static class Status {
        public static final Integer PENDING = 0;
        public static final Integer COMPLETED = 1;
        public static final Integer FAILED = 2;
        public static final Integer EXPIRED = 3;  // 超时未完成
    }
}
