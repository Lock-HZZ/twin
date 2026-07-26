package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.StakeDividendRecordDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StakeDividendRecordMapper extends BaseMapper<StakeDividendRecordDO> {

    @Insert("<script>" +
            "INSERT INTO stake_dividend_record " +
            "(stake_id, user_id, amount, dividend_date, batch_id, status, created_date) VALUES " +
            "<foreach collection='list' item='d' separator=','>" +
            "(#{d.stakeId}, #{d.userId}, #{d.amount}, #{d.dividendDate}, #{d.batchId}, #{d.status}, #{d.createdDate})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<StakeDividendRecordDO> list);
}
