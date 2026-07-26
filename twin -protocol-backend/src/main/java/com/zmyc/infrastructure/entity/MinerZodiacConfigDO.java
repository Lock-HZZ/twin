package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("miner_zodiac_config")
public class MinerZodiacConfigDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String zodiacType;

    private String zodiacName;

    private BigDecimal dropRate;

    private Boolean enabled;
}
