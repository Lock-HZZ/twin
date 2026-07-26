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

}
