package com.zmyc.infrastructure.repository;

import com.zmyc.common.util.BatchUtils;
import com.zmyc.infrastructure.entity.BalanceTransactionDO;
import com.zmyc.infrastructure.mapper.BalanceTransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BalanceTransactionRepository {

    @Autowired
    private BalanceTransactionMapper transactionMapper;

    /** 保存单条流水 */
    public void save(BalanceTransactionDO transaction) {
        transactionMapper.insert(transaction);
    }

    /** 分批批量保存流水 */
    public void batchSave(List<BalanceTransactionDO> transactions) {
        BatchUtils.execute(transactions, transactionMapper::batchInsert);
    }
}
