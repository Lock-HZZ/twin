package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmyc.infrastructure.entity.UserEnergyDO;
import com.zmyc.infrastructure.mapper.UserEnergyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class UserEnergyRepository {

    @Autowired
    private UserEnergyMapper energyMapper;

    /** 增加用户能量值 */
    public void addEnergy(Long userId, BigDecimal amount) {
        long now = System.currentTimeMillis() / 1000;
        energyMapper.addEnergy(userId, amount, now);
    }

    /** 查询用户能量值 */
    public UserEnergyDO findByUserId(Long userId) {
        LambdaQueryWrapper<UserEnergyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEnergyDO::getUserId, userId);
        return energyMapper.selectOne(wrapper);
    }
}
