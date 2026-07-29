package com.zmyc.infrastructure.repository;

import com.zmyc.infrastructure.entity.FeeDistributionRecordDO;
import com.zmyc.infrastructure.mapper.FeeDistributionRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FeeDistributionRecordRepository {

    @Autowired
    private FeeDistributionRecordMapper mapper;

    public FeeDistributionRecordDO findByWithdrawTxHash(String withdrawTxHash) {
        return mapper.findByWithdrawTxHash(withdrawTxHash);
    }

    public void save(FeeDistributionRecordDO record) {
        if (record.getId() == null) {
            mapper.insert(record);
        } else {
            mapper.updateById(record);
        }
    }
}
