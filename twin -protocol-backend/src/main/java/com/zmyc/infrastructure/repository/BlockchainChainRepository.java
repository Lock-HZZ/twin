package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.infrastructure.entity.BlockchainChainDO;
import com.zmyc.infrastructure.mapper.BlockchainChainMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BlockchainChainRepository {

    @Autowired
    private BlockchainChainMapper chainMapper;

    /** 查询所有启用的链配置 */
    public List<BlockchainChainDO> findAllEnabled() {
        LambdaQueryWrapper<BlockchainChainDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlockchainChainDO::getEnabled, true);
        return chainMapper.selectList(wrapper);
    }
}
