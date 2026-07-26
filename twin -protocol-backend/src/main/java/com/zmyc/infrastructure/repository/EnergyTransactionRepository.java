package com.zmyc.infrastructure.repository;

import com.zmyc.infrastructure.entity.EnergyTransactionDO;
import com.zmyc.infrastructure.mapper.EnergyTransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class EnergyTransactionRepository {

    @Autowired
    private EnergyTransactionMapper transactionMapper;

    /** 保存能量值流水 */
    public void save(EnergyTransactionDO transaction) {
        transactionMapper.insert(transaction);
    }
}
