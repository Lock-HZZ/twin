package com.zmyc.application.controller;

import com.zmyc.application.vo.response.ApiResponse;
import com.zmyc.application.vo.response.PageResponse;
import com.zmyc.application.vo.response.ShareInfoResponse;
import com.zmyc.application.vo.response.ShareListItemResponse;
import com.zmyc.application.vo.response.UserInfoResponse;
import com.zmyc.common.context.UserContext;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.repository.UserRepository;
import com.zmyc.service.ShareInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "用户信息相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ShareInfoService shareInfoService;

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> getUserProfile() {
        Long currentUserId = UserContext.getCurrentUserId();
        UserDO user = userRepository.findById(currentUserId);

        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setAddress(user.getAddress());
        response.setEmail(user.getEmail());
        response.setEnabled(user.getEnabled());
        response.setInvitedCode(user.getInvitedCode());

        return ApiResponse.success("获取用户信息成功", response);
    }

    @Operation(summary = "获取分享信息", description = "S/D等级、直推团队人数及业绩、小区业绩等")
    @GetMapping("/share-info")
    public ApiResponse<ShareInfoResponse> getShareInfo() {
        Long currentUserId = UserContext.getCurrentUserId();
        return ApiResponse.success("获取分享信息成功", shareInfoService.getShareInfo(currentUserId));
    }

    @Operation(summary = "获取直推下级列表", description = "分页查询直推用户的等级、团队数据等信息")
    @GetMapping("/share-list")
    public ApiResponse<PageResponse<ShareListItemResponse>> getShareList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long currentUserId = UserContext.getCurrentUserId();
        return ApiResponse.success("获取分享列表成功",
                shareInfoService.getShareList(currentUserId, page, pageSize));
    }
}
