package com.zmyc.application.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "入金签名请求")
@Data
public class DepositRequest {

    @Schema(description = "入金金额（USDC），必须是100的整数倍", example = "100")
    @NotNull(message = "入金金额不能为空")
    @Min(value = 100, message = "最低投资金额为100 USDC")
    private BigDecimal amount;
}
