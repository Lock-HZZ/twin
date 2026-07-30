package com.zmyc.service;

import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.repository.UserRelationClosureRepository;
import com.zmyc.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户等级服务
 * 负责计算和更新动态分币等级（D1-D8）
 */
@Slf4j
@Service
public class UserLevelService {

    @Autowired
    private UserRelationClosureRepository closureRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 计算用户当前等级（基于有效直推人数）
     */
    public int calculateLevel(Long userId) {
        int validDirectCount = closureRepository.countValidDirectChildren(userId);
        return mapCountToLevel(validDirectCount);
    }

    /**
     * 更新指定用户的等级
     */
    @Transactional
    public void updateUserLevel(Long userId) {
        int newLevel = calculateLevel(userId);
        UserDO user = userRepository.findById(userId);

        if (user.getLevel() == null || user.getLevel() != newLevel) {
            user.setLevel(newLevel);
            userRepository.update(user);
            log.info("用户等级已更新: userId={}, oldLevel={}, newLevel={}",
                    userId, user.getLevel(), newLevel);
        }
    }

    /**
     * 更新用户及其所有祖先的等级（入金/移除LP时调用）
     * 因为下级入金会影响上级的有效直推数
     */
    @Transactional
    public void updateUserAndAncestorsLevel(Long userId) {
        // 更新自己
        updateUserLevel(userId);

        // 更新所有祖先（因为他们的有效直推数可能变化）
        List<Long> ancestors = closureRepository.findAncestorIds(userId);
        for (Long ancestorId : ancestors) {
            updateUserLevel(ancestorId);
        }

        log.info("已更新用户及祖先等级: userId={}, ancestorCount={}", userId, ancestors.size());
    }

    /**
     * 人数映射到等级
     */
    private int mapCountToLevel(int count) {
        if (count >= 30000) return UserDO.Level.D8;
        if (count >= 15000) return UserDO.Level.D7;
        if (count >= 10000) return UserDO.Level.D6;
        if (count >= 4000) return UserDO.Level.D5;
        if (count >= 1000) return UserDO.Level.D4;
        if (count >= 360) return UserDO.Level.D3;
        if (count >= 120) return UserDO.Level.D2;
        if (count >= 30) return UserDO.Level.D1;
        return UserDO.Level.NONE;
    }
}
