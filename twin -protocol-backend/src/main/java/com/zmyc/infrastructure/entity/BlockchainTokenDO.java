package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("blockchain_token")
public class BlockchainTokenDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属链ID */
    private Long chainId;

    /** 合约地址(原生币则为0x0或空) */
    private String contractAddress;

    /** 代币符号(USDT/USDC/BOT等) */
    private String symbol;

    /** 代币全称 */
    private String name;

    /** 精度位数 */
    private Integer decimals;

    /** 是否原生币: 0-否, 1-是 */
    private Boolean isNative;

    /** 对USDT汇率(1 TOKEN = ? USDT) */
    private BigDecimal usdtRate;

    /** 是否允许入金: 0-禁用, 1-启用 */
    private Boolean depositEnabled;

    /** 是否启用: 0-禁用, 1-启用 */
    private Boolean enabled;

    private Long createdDate;

    private Long lastUpdatedDate;
}
