package com.zmyc.application.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "移除LP响应")
@Data
public class RemoveLiquidityResponse {

    @Schema(description = "交易哈希")
    private String txHash;

    @Schema(description = "获得的USDC数量")
    private BigDecimal usdcOut;

    @Schema(description = "转入分红池的TIP数量")
    private BigDecimal tipToDividend;

    @Schema(description = "交易状态: PENDING-待确认, SUCCESS-成功, FAILED-失败")
    private String status;
}
