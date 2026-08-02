package com.zmyc.application.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "用户入金记录")
public class UserDepositResponse {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "LP值")
    private BigDecimal liquidity;

    @Schema(description = "订单价值(USDC)")
    private BigDecimal amount;

    @Schema(description = "订单权重参数")
    private BigDecimal weight;

    @Schema(description = "订单总权重")
    private BigDecimal totalWeight;

    @Schema(description = "持仓天数")
    private Long holdingDays;

    @Schema(description = "铸造Hash")
    private String txHash;

    @Schema(description = "状态：0-待处理，1-已完成，2-失败，3-移除中，4-已出局")
    private Integer status;

    @Schema(description = "创建时间")
    private Long createdDate;

    @Schema(description = "移除LP的交易哈希（已出局订单）")
    private String withdrawTxHash;

    @Schema(description = "移除LP时间（10位时间戳，已出局订单）")
    private Long removedDate;

    @Schema(description = "锁仓TIP总额（已出局订单，60天线性释放）")
    private BigDecimal lockedTip;
}
