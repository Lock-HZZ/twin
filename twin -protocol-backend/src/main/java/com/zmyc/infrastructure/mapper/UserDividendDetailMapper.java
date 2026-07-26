package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.UserDividendDetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserDividendDetailMapper extends BaseMapper<UserDividendDetailDO> {

    void batchInsert(@Param("list") List<UserDividendDetailDO> list);
}
