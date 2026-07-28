package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * TIP燃烧记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tip_burn_record")
public class TipBurnRecordDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 燃烧日期（10位时间戳，当天零点） */
    private Long burnDate;

    /** 燃烧比例（基点，例如8000表示80%） */
    private Integer burnRate;

    /** 燃烧的TIP数量 */
    private BigDecimal burnAmount;

    /** 进入分红池的TIP数量（剩余部分） */
    private BigDecimal dividendAmount;

    /** 交易哈希 */
    private String txHash;

    /** 状态：0-待执行，1-执行中，2-成功，3-失败 */
    private Integer status;

    /** 重试次数 */
    private Integer retryCount;

    /** 失败原因 */
    private String failReason;

    public static class Status {
        public static final Integer PENDING = 0;
        public static final Integer PROCESSING = 1;
        public static final Integer SUCCESS = 2;
        public static final Integer FAILED = 3;
    }
}
