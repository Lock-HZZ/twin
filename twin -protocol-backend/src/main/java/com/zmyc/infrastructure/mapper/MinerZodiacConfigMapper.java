package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.MinerZodiacConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MinerZodiacConfigMapper extends BaseMapper<MinerZodiacConfigDO> {

    @Select("SELECT * FROM miner_zodiac_config WHERE enabled = 1 ORDER BY zodiac_type ASC")
    List<MinerZodiacConfigDO> findAllEnabled();
}
