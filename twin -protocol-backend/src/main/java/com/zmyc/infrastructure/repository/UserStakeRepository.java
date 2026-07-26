package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmyc.infrastructure.entity.UserStakeDO;
import com.zmyc.infrastructure.mapper.UserStakeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserStakeRepository {

    @Autowired
    private UserStakeMapper stakeMapper;

    /** 根据链上质押ID查询 */
    public UserStakeDO findByStakeId(Long stakeId) {
        LambdaQueryWrapper<UserStakeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStakeDO::getStakeId, stakeId);
        return stakeMapper.selectOne(wrapper);
    }

    /** 查询用户所有质押记录 */
    public List<UserStakeDO> findByUserId(Long userId) {
        LambdaQueryWrapper<UserStakeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStakeDO::getUserId, userId)
               .orderByDesc(UserStakeDO::getCreatedDate);
        return stakeMapper.selectList(wrapper);
    }

    /** 查询用户进行中的质押记录 */
    public List<UserStakeDO> findActiveStakesByUserId(Long userId) {
        LambdaQueryWrapper<UserStakeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStakeDO::getUserId, userId)
               .eq(UserStakeDO::getStatus, UserStakeDO.Status.ACTIVE)
               .orderByDesc(UserStakeDO::getCreatedDate);
        return stakeMapper.selectList(wrapper);
    }

    /** 查询所有进行中的质押记录 */
    public List<UserStakeDO> findAllActiveStakes() {
        LambdaQueryWrapper<UserStakeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStakeDO::getStatus, UserStakeDO.Status.ACTIVE);
        return stakeMapper.selectList(wrapper);
    }

    /** 保存质押记录 */
    public void save(UserStakeDO stake) {
        if (stake.getId() == null) {
            stakeMapper.insert(stake);
        } else {
            stakeMapper.updateById(stake);
        }
    }

    /** 根据ID查询 */
    public UserStakeDO findById(Long id) {
        return stakeMapper.selectById(id);
    }
}
