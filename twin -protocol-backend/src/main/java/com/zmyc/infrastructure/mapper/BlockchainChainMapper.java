package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.BlockchainChainDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BlockchainChainMapper extends BaseMapper<BlockchainChainDO> {

    /** 查询所有启用的链配置 */
    @Select("SELECT * FROM blockchain_chain WHERE enabled = 1 ORDER BY chain_id ASC")
    List<BlockchainChainDO> findAllEnabled();
}
