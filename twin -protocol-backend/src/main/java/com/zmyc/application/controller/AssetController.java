package com.zmyc.application.controller;

import com.zmyc.application.vo.request.AssetRecordRequest;
import com.zmyc.application.vo.response.ApiResponse;
import com.zmyc.application.vo.response.AssetRecordResponse;
import com.zmyc.application.vo.response.PageResponse;
import com.zmyc.common.context.UserContext;
import com.zmyc.service.AssetRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "资产明细", description = "查询能量、USDC、TIP 资产流水明细")
@RestController
@RequestMapping("/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetRecordService assetRecordService;

    /**
     * 查询资产明细
     *
     * @param request assetType=ENERGY|USDC|TIP（可选，为空返回全部），page，pageSize
     */
    @Operation(summary = "资产明细列表",
            description = "assetType 可选：ENERGY（能量）/ USDC / TIP，不传则返回全部类型明细，按时间倒序分页")
    @GetMapping("/records")
    public ApiResponse<PageResponse<AssetRecordResponse>> getRecords(AssetRecordRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(assetRecordService.queryRecords(userId, request));
    }
}
