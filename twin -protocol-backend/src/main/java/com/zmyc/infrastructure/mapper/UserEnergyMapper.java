package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.UserEnergyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserEnergyMapper extends BaseMapper<UserEnergyDO> {

    @Update("INSERT INTO user_energy (user_id, energy_balance, total_earned, created_date, last_updated_date) " +
            "VALUES (#{userId}, #{amount}, #{amount}, #{now}, #{now}) " +
            "ON DUPLICATE KEY UPDATE " +
            "energy_balance = energy_balance + #{amount}, " +
            "total_earned = total_earned + #{amount}, " +
            "last_updated_date = #{now}")
    int addEnergy(@Param("userId") Long userId,
                  @Param("amount") BigDecimal amount,
                  @Param("now") Long now);
}
