package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_dividend_detail")
public class UserDividendDetailDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dividendId;

    private Long userId;

    private Integer minerCount;

    private BigDecimal tipAmount;

    private Byte status;

    private String claimTxHash;

    private Long claimDate;

    public static class Status {
        public static final byte PENDING = 1;
        public static final byte CLAIMED = 2;
    }
}
