package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_dividend")
public class DailyDividendDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate dividendDate;

    private BigDecimal totalTipAmount;

    private Integer totalMinerCount;

    private BigDecimal perMinerAmount;

    private Byte status;

    private Long distributedDate;

    public static class Status {
        public static final byte PENDING = 1;
        public static final byte DISTRIBUTED = 2;
        public static final byte CANCELLED = 3;
    }
}
