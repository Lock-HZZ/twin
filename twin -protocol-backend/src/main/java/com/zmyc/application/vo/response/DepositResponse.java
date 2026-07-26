package com.zmyc.application.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "入金签名响应，前端凭此调用合约 depositWithSig")
@Data
public class DepositResponse {

    @Schema(description = "用户钱包地址")
    private String user;

    @Schema(description = "入金金额（USDC 最小单位，6位精度）", example = "100000000")
    private String amount;

    @Schema(description = "随机数（uint256）")
    private String nonce;

    @Schema(description = "签名有效期截止时间戳（秒）")
    private Long deadline;

    @Schema(description = "功能类型，固定为 2")
    private Integer functionType;

    @Schema(description = "后端 signer 生成的 EIP-712 签名（0x开头65字节）")
    private String signature;
}
