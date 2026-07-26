package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_miner")
public class UserMinerDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String minerType;

    private BigDecimal purchasePrice;

    private Long depositId;

    private Boolean isSynthesized;

    private Byte status;

    private Long activatedDate;

    private Long expiredDate;

    private BigDecimal totalMined;

    public static class Status {
        public static final byte MINING = 1;
        public static final byte EXPIRED = 2;
        public static final byte SYNTHESIZED = 3;
    }
}
