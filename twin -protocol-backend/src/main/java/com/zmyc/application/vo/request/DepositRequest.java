package com.zmyc.application.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "入金签名请求")
@Data
public class DepositRequest {

    @Schema(description = "入金金额（USDC），须满足后端配置的最低金额与步长", example = "100")
    @NotNull(message = "入金金额不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "入金金额必须大于0")
    private BigDecimal amount;
}
