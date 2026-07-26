package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.SystemConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigDO> {

    @Select("SELECT * FROM system_config WHERE config_key = #{configKey} AND enabled = 1")
    SystemConfigDO findByKey(@Param("configKey") String configKey);
}
