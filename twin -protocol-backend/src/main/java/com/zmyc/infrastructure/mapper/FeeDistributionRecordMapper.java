package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.FeeDistributionRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FeeDistributionRecordMapper extends BaseMapper<FeeDistributionRecordDO> {

    @Select("SELECT * FROM fee_distribution_record WHERE withdraw_tx_hash = #{withdrawTxHash} LIMIT 1")
    FeeDistributionRecordDO findByWithdrawTxHash(String withdrawTxHash);
}
