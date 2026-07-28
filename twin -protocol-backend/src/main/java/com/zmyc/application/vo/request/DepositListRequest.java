package com.zmyc.application.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "入金列表查询请求")
public class DepositListRequest {

    @Schema(description = "页码，从1开始", example = "1")
    private Integer page = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "状态筛选：1-持仓中(COMPLETED)，4-已出局")
    private Integer statusFilter;
}
