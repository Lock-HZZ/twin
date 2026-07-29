package com.zmyc.infrastructure.repository;

import com.zmyc.infrastructure.entity.UserPerformanceDO;
import com.zmyc.infrastructure.mapper.UserPerformanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class UserPerformanceRepository {

    @Autowired
    private UserPerformanceMapper performanceMapper;

    /** 用户注册时初始化业绩记录，所有值归零 */
    public void initForNewUser(Long userId) {
        long now = System.currentTimeMillis() / 1000;
        UserPerformanceDO p = new UserPerformanceDO();
        p.setUserId(userId);
        p.setPersonalVolumeUsdt(BigDecimal.ZERO);
        p.setTeamVolumeUsdt(BigDecimal.ZERO);
        p.setCommunityVolumeUsdt(BigDecimal.ZERO);
        p.setCreatedDate(now);
        p.setLastUpdatedDate(now);
        performanceMapper.insert(p);
    }

    public void addPersonalVolume(Long userId, BigDecimal amount) {
        long now = System.currentTimeMillis() / 1000;
        performanceMapper.addPersonalVolume(userId, amount, now);
    }

    public void batchAddTeamVolume(Long userId, BigDecimal amount) {
        long now = System.currentTimeMillis() / 1000;
        performanceMapper.batchAddTeamVolume(userId, amount, now);
    }

    public void batchUpdateCommunityVolume(Long userId) {
        long now = System.currentTimeMillis() / 1000;
        performanceMapper.batchUpdateCommunityVolume(userId, now);
    }

    /**
     * 计算并返回小区业绩：
     * 取所有直推线的团队业绩列表，去掉最大值，其余求和。
     */
    public BigDecimal calcCommunityVolume(Long userId) {
        List<BigDecimal> volumes = performanceMapper.findDirectLineVolumes(userId);
        if (volumes == null || volumes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // 已按降序排列，第一个是大区，其余相加
        return volumes.stream()
                .skip(1)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 查询用户业绩记录（用于获取communityVolumeUsdt等缓存字段） */
    public UserPerformanceDO findByUserId(Long userId) {
        return performanceMapper.selectById(userId);
    }
}
