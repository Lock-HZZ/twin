package com.zmyc.infrastructure.repository;

import com.zmyc.infrastructure.entity.MinerZodiacConfigDO;
import com.zmyc.infrastructure.mapper.MinerZodiacConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MinerZodiacConfigRepository {

    @Autowired
    private MinerZodiacConfigMapper configMapper;

    /** 查询所有启用的星座配置 */
    public List<MinerZodiacConfigDO> findAllEnabled() {
        return configMapper.findAllEnabled();
    }

    /** 保存配置 */
    public void save(MinerZodiacConfigDO config) {
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
    }
}
