package com.zmyc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 分红合约配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "dividend.contract")
public class DividendContractConfig {

    /** 合约地址 */
    private String contractAddress;

    /** RPC 节点地址 */
    private String rpcUrl;

    /** 操作员私钥（用于签名交易） */
    private String operatorPrivateKey;

    /** Gas 价格（Gwei），为空则使用网络建议值 */
    private Long gasPrice;

    /** Gas 限制 */
    private Long gasLimit = 3000000L;

    /** 链 ID（Polygon 主网：137，Mumbai 测试网：80001） */
    private Long chainId = 137L;

    /** 每个分红批次的最大用户数（分片大小），防止单笔交易 gas 超限 */
    private Integer batchSize = 200;
}
