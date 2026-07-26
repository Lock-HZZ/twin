package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_energy")
public class UserEnergyDO extends BaseDO {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private BigDecimal energyBalance;

    private BigDecimal totalEarned;

    private BigDecimal totalConsumed;
}
