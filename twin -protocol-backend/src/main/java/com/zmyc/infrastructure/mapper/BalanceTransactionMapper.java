package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.BalanceTransactionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BalanceTransactionMapper extends BaseMapper<BalanceTransactionDO> {

    void batchInsert(@Param("list") List<BalanceTransactionDO> list);
}
