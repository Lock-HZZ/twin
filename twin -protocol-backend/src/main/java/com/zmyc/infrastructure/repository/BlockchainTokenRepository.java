package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.infrastructure.entity.BlockchainTokenDO;
import com.zmyc.infrastructure.mapper.BlockchainTokenMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BlockchainTokenRepository {

    @Autowired
    private BlockchainTokenMapper tokenMapper;

    /** 查询指定链的所有启用代币 */
    public List<BlockchainTokenDO> findByChainIdEnabled(Long chainId) {
        LambdaUpdateWrapper<BlockchainTokenDO> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(BlockchainTokenDO::getChainId, chainId)
                .eq(BlockchainTokenDO::getEnabled, true)
                .orderByAsc(BlockchainTokenDO::getSymbol);
        return tokenMapper.selectList(queryWrapper);
    }

    /** 根据代币符号查询启用的代币 */
    public BlockchainTokenDO findBySymbol(String symbol) {
        LambdaUpdateWrapper<BlockchainTokenDO> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(BlockchainTokenDO::getSymbol, symbol)
                .eq(BlockchainTokenDO::getEnabled, true)
                .last("LIMIT 1");
        BlockchainTokenDO token = tokenMapper.selectOne(queryWrapper);
        if (token == null) {
            throw new BusinessException(ErrorCode.TOKEN_NOT_FOUND, "代币未找到或未启用: " + symbol);
        }
        return token;
    }

}
