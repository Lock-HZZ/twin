package com.zmyc.application.controller;

import com.zmyc.application.vo.request.DepositRequest;
import com.zmyc.application.vo.response.ApiResponse;
import com.zmyc.application.vo.response.DepositResponse;
import com.zmyc.application.vo.response.QuotaInfoResponse;
import com.zmyc.common.context.UserContext;
import com.zmyc.domain.bo.UserQuotaInfo;
import com.zmyc.service.UserDepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "入金管理", description = "用户入金签名接口")
@RestController
@RequestMapping("/deposit")
@RequiredArgsConstructor
public class DepositController {

    private final UserDepositService userDepositService;

    @Operation(summary = "获取入金签名", description = "校验额度后生成 EIP-712 签名，前端凭签名调用合约 depositWithSig")
    @PostMapping("/sign")
    public ApiResponse<DepositResponse> getDepositSignature(@Valid @RequestBody DepositRequest request) {
        DepositResponse response = userDepositService.createDepositSignature(request.getAmount());
        return ApiResponse.success("签名生成成功", response);
    }

    @Operation(summary = "获取用户入金额度信息")
    @GetMapping("/quota")
    public ApiResponse<QuotaInfoResponse> getQuotaInfo() {
        Long userId = UserContext.getCurrentUserId();
        UserQuotaInfo quotaInfo = userDepositService.getUserQuotaInfo(userId);

        QuotaInfoResponse response = new QuotaInfoResponse();
        response.setTotalQuota(quotaInfo.getTotalQuota());
        response.setUsedQuota(quotaInfo.getUsedQuota());
        response.setAvailableQuota(quotaInfo.getAvailableQuota());
        response.setDailyMaxDeposit(quotaInfo.getDailyMaxDeposit());
        response.setDailyUsed(quotaInfo.getDailyUsed());
        response.setDailyRemaining(quotaInfo.getDailyRemaining());

        return ApiResponse.success("获取额度信息成功", response);
    }
}
