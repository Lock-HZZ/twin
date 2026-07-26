package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("blockchain_chain")
public class BlockchainChainDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 链ID(如1=以太坊主网,56=BSC) */
    private Long chainId;

    /** 链名称(Ethereum/BSC/Polygon等) */
    private String chainName;

    /** RPC节点地址 */
    private String rpcUrl;

    /** 区块浏览器地址 */
    private String explorerUrl;

    /** 原生币符号(ETH/BNB/MATIC) */
    private String nativeSymbol;

    /** 是否启用: 0-禁用, 1-启用 */
    private Boolean enabled;

    private Long createdDate;

    private Long lastUpdatedDate;
}
