package com.zmyc.application.controller;

import com.zmyc.application.vo.response.ApiResponse;
import com.zmyc.infrastructure.entity.RewardRecordDO;
import com.zmyc.infrastructure.entity.UserStakeDO;
import com.zmyc.service.UserStakeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stake")
public class StakeController {

    @Autowired
    private UserStakeService stakeService;

    /**
     * 查询用户所有质押记录
     */
    @GetMapping("/list")
    public ApiResponse<List<UserStakeDO>> getUserStakes() {
        List<UserStakeDO> stakes = stakeService.getCurrentUserStakes();
        return ApiResponse.success(stakes);
    }

    /**
     * 查询用户分红记录
     */
    @GetMapping("/dividends")
    public ApiResponse<List<RewardRecordDO>> getUserDividends() {
        List<RewardRecordDO> dividends = stakeService.getCurrentUserDividends();
        return ApiResponse.success(dividends);
    }
}
