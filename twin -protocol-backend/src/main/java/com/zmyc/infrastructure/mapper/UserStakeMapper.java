package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.UserStakeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserStakeMapper extends BaseMapper<UserStakeDO> {
}
