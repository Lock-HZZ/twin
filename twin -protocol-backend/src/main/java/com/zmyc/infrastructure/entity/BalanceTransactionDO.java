package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("balance_transaction")
public class BalanceTransactionDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long tokenId;

    private Byte txType;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private Long relatedId;

    private String remark;

    public static class TxType {
        public static final byte DIVIDEND_IN = 1;
        public static final byte WITHDRAW_OUT = 2;
        public static final byte FREEZE = 3;
        public static final byte UNFREEZE = 4;
    }
}
