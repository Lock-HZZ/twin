package com.zmyc.infrastructure.repository;

import com.zmyc.infrastructure.entity.DepositNonceDO;
import com.zmyc.infrastructure.mapper.DepositNonceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class DepositNonceRepository {

    @Autowired
    private DepositNonceMapper depositNonceMapper;

    public DepositNonceDO findByUserIdAndNonce(Long userId, Long nonce) {
        return depositNonceMapper.findByUserIdAndNonce(userId, nonce);
    }

    public void save(DepositNonceDO depositNonce) {
        depositNonceMapper.insert(depositNonce);
    }

    public boolean isNonceUsed(Long userId, Long nonce) {
        return findByUserIdAndNonce(userId, nonce) != null;
    }
}
