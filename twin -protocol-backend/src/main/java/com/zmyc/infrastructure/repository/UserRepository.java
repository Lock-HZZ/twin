package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    @Autowired
    private UserMapper userMapper;


    public UserDO findByAddress(String address) {
        QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("address", address);
        UserDO user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        return user;
    }

    /**
     * 根据地址查询用户，不存在时返回 null（不抛异常）
     * 用于登录前判断用户是否已注册/绑定上级
     */
    public UserDO findByAddressOrNull(String address) {
        QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("address", address);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 根据邀请码查询用户，不存在时返回 null（不抛异常）
     */
    public UserDO findByInvitedCodeOrNull(String invitedCode) {
        QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("invited_code", invitedCode);
        return userMapper.selectOne(queryWrapper);
    }

    public void save(UserDO user) {
        userMapper.insert(user);
    }

    public UserDO findByInvitedCode(String invitedCode) {
        QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("invited_code", invitedCode);
        UserDO user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        return user;
    }

    /**
     * 根据ID查询用户
     */
    public UserDO findById(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        return user;
    }

    /**
     * 更新用户
     */
    public void update(UserDO user) {
        userMapper.updateById(user);
    }

    /**
     * 批量查询用户地址（用于分红发放时收集地址，避免 N 次单条查询）
     * @return Map<userId, address>，不存在的 userId 会被过滤
     */
    public Map<Long, String> findAddressesByUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(UserDO::getId, userIds)
               .select(UserDO::getId, UserDO::getAddress);
        return userMapper.selectList(wrapper)
                .stream()
                .filter(u -> u.getAddress() != null && !u.getAddress().isEmpty())
                .collect(Collectors.toMap(UserDO::getId, UserDO::getAddress));
    }

    /**
     * 根据角色列表查询用户
     */
    public List<UserDO> findByRoles(List<Integer> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(UserDO::getRole, roles);
        return userMapper.selectList(wrapper);
    }

    /**
     * 根据单个角色查询用户
     */
    public List<UserDO> findByRole(Integer role) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getRole, role);
        return userMapper.selectList(wrapper);
    }

    /**
     * 根据等级查询用户ID列表（用于动态分币）
     * 直接查询 user.level 字段，等级由 UserLevelService 在入金/移除LP时维护
     */
    public List<Long> findUserIdsByLevel(int level) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getLevel, level)
               .select(UserDO::getId);
        return userMapper.selectList(wrapper).stream()
                .map(UserDO::getId)
                .collect(Collectors.toList());
    }

    public Long findUserIdByAddress(String userAddress) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getAddress, userAddress)
               .select(UserDO::getId);
        UserDO user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        return user.getId();
    }
}
