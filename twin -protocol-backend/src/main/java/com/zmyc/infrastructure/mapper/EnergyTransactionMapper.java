package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.EnergyTransactionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EnergyTransactionMapper extends BaseMapper<EnergyTransactionDO> {
}
