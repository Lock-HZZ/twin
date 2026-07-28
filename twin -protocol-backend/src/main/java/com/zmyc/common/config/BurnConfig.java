package com.zmyc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TIP燃烧配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "burn")
public class BurnConfig {

    /** 操作员私钥（有权限调用合约销毁的地址） */
    private String operatorPrivateKey;

    /** Trade合约地址（TIP token 合约） */
    private String tradeContractAddress;

    /** 分红合约地址（TIP.dividendAddress，接收燃烧分红的合约） */
    private String dividendContractAddress;

    /** 链ID */
    private Long chainId;

    /** 初始销毁比例（基点，8000 = 80%） */
    private Integer initialBurnRate = 8000;

    /** 每日降低比例（基点，50 = 0.5%） */
    private Integer dailyDecreaseRate = 50;

    /** 最低销毁比例（基点，3000 = 30%） */
    private Integer minBurnRate = 3000;

    /** Gas Limit */
    private Long gasLimit = 500000L;
}
