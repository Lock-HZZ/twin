package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("stake_dividend_record")
public class StakeDividendRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 质押记录ID */
    private Long stakeId;

    /** 用户ID */
    private Long userId;

    /** 分红金额（TIP） */
    private BigDecimal amount;

    /** 分红日期（yyyyMMdd格式） */
    private Long dividendDate;

    /** 批次ID（链上幂等去重用，同批次共享） */
    private String batchId;

    /** 状态：0-待发放，1-已发送待确认，2-已发放，3-失败 */
    private Integer status;

    /** 发放时间（秒） */
    private Long paidTime;

    /** 交易哈希 */
    private String txHash;

    /** 创建时间（秒） */
    private Long createdDate;

    /** 更新时间（秒） */
    private Long updatedDate;

    /** 状态常量 */
    public static class Status {
        public static final int PENDING = 0;  // 已计算，待发交易
        public static final int SENT = 1;     // 交易已发出，待链上确认
        public static final int PAID = 2;     // 链上确认成功
        public static final int FAILED = 3;   // 链上失败/revert
    }
}
