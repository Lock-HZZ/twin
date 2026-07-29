package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提现手续费二次分配记录
 * 每条 Dividend.Withdraw(USDC) 事件触发一次分配，以 withdrawTxHash 幂等
 */
@Data
@TableName("fee_distribution_record")
public class FeeDistributionRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 触发来源：用户提现 txHash（唯一幂等键） */
    private String withdrawTxHash;

    /** 链上幂等批次ID（32字节hex） */
    private String batchId;

    /** 本次二次分配的手续费金额（USDC，业务精度） */
    private BigDecimal feeAmount;

    /** 分配状态：0-PENDING, 1-SENT, 2-CONFIRMED */
    private Byte status;

    /** 调用 FeeDividend.addRewards 的交易哈希 */
    private String distributeTxHash;

    /** 创建时间（秒级时间戳） */
    private Long createdDate;

    /** 更新时间（秒级时间戳） */
    private Long updatedDate;

    public static class Status {
        public static final byte PENDING = 0;
        public static final byte SENT = 1;
        public static final byte CONFIRMED = 2;
    }
}
