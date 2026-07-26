package com.zmyc.infrastructure.repository;

import com.zmyc.infrastructure.entity.MinerSynthesisLogDO;
import com.zmyc.infrastructure.mapper.MinerSynthesisLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MinerSynthesisLogRepository {

    @Autowired
    private MinerSynthesisLogMapper logMapper;

    /** 保存合成记录 */
    public void save(MinerSynthesisLogDO log) {
        logMapper.insert(log);
    }
}
