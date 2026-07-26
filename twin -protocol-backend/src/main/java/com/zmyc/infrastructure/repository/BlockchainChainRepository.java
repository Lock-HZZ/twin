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
        return chainMapper.findAllEnabled();
    }

    /** 根据链ID查询配置 */
    public BlockchainChainDO findByChainId(Long chainId) {
        LambdaQueryWrapper<BlockchainChainDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlockchainChainDO::getChainId, chainId);
        BlockchainChainDO chain = chainMapper.selectOne(wrapper);
        if (chain == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return chain;
    }

    /** 保存链配置 */
    public void save(BlockchainChainDO chain) {
        long now = System.currentTimeMillis() / 1000;
        if (chain.getId() == null) {
            chain.setCreatedDate(now);
            chain.setLastUpdatedDate(now);
            chainMapper.insert(chain);
        } else {
            chain.setLastUpdatedDate(now);
            chainMapper.updateById(chain);
        }
    }
}
