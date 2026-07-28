package com.zmyc.application.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "移除LP请求")
@Data
public class RemoveLiquidityRequest {

    @Schema(description = "入金订单ID", required = true)
    @NotNull(message = "订单ID不能为空")
    private Long depositId;
}
