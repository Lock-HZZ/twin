package com.zmyc.application.controller;

import com.zmyc.application.vo.response.ApiResponse;
import com.zmyc.application.vo.response.UserInfoResponse;
import com.zmyc.common.context.UserContext;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.repository.UserRepository;
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

}