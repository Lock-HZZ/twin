package com.zmyc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Trade合约配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "trade")
public class TradeConfig {

    /** 操作员私钥（有权限调用 removeLiquidity 的地址） */
    private String operatorPrivateKey;

    /** Trade合约地址 */
    private String contractAddress;

    /** 链ID */
    private Long chainId;

    /** Gas Price（可选，不配置则使用网络当前价格） */
    private Long gasPrice;

    /** Gas Limit */
    private Long gasLimit = 500000L;
}
