package com.zmyc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 入金签名配置
 * 对应合约 Deposit.sol 的 EIP-712 签名域参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "deposit")
public class DepositConfig {

    /** signer 私钥（合约中 signer 地址对应的私钥） */
    private String signerPrivateKey;

    /** 入金合约地址（EIP-712 verifyingContract） */
    private String contractAddress;

    /** 链ID */
    private Long chainId;

    /** EIP-712 domain name，合约固定为 TwinProtocolDeposit */
    private String domainName = "TwinProtocolDeposit";

    /** EIP-712 domain version，合约固定为 1 */
    private String domainVersion = "1";

    /** 签名有效期（秒），前端凭签名调用合约的 deadline 窗口 */
    private Long signatureTtlSeconds = 600L;

    /** 入金订单过期时间（秒），PENDING 状态超过此时间自动取消并释放额度，默认30分钟 */
    private Long orderExpirationSeconds = 1800L;

    /** 最低入金金额（USDC），默认 100 */
    private BigDecimal minAmount = new BigDecimal("100");

    /** 入金金额步长（USDC），入金金额必须是该值的整数倍，默认 100 */
    private BigDecimal stepAmount = new BigDecimal("100");
}
