package com.zmyc.infrastructure.repository;

import com.zmyc.infrastructure.entity.UserBalanceDO;
import com.zmyc.infrastructure.mapper.UserBalanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class UserBalanceRepository {

    @Autowired
    private UserBalanceMapper balanceMapper;

    public UserBalanceDO findByUserToken(Long userId, Long tokenId) {
        return balanceMapper.findByUserToken(userId, tokenId);
    }

    public UserBalanceDO findForUpdate(Long userId, Long tokenId) {
        return balanceMapper.findForUpdate(userId, tokenId);
    }

    public List<UserBalanceDO> findByTokenIdAndUserIds(Long tokenId, List<Long> userIds) {
        return balanceMapper.findByTokenIdAndUserIds(tokenId, userIds);
    }

    public int addBalanceOrCreate(Long userId, Long tokenId, BigDecimal amount) {
        long now = System.currentTimeMillis() / 1000;
        return balanceMapper.addBalanceOrCreate(userId, tokenId, amount, now);
    }

    public int deductBalance(Long userId, Long tokenId, BigDecimal amount) {
        long now = System.currentTimeMillis() / 1000;
        return balanceMapper.deductBalance(userId, tokenId, amount, now);
    }
}
