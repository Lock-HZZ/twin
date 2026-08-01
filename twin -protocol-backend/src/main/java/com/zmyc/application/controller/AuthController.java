package com.zmyc.application.controller;

import com.zmyc.application.vo.response.ApiResponse;
import com.zmyc.application.vo.response.BindStatusResponse;
import com.zmyc.application.vo.response.JwtResponse;
import com.zmyc.application.vo.request.LoginRequest;
import com.zmyc.application.vo.request.RegisterRequest;
import com.zmyc.common.annotation.RateLimit;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.common.util.EthereumAddressValidator;
import com.zmyc.common.util.IpUtil;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.common.util.JwtUtil;
import com.zmyc.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "用户登录认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Operation(summary = "钱包登录")
    @RateLimit(key = "login", limitType = RateLimit.LimitType.IP)
    @PostMapping("/login")
    public ApiResponse<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        if (EthereumAddressValidator.validateAddress(loginRequest.getAddress())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "钱包地址格式无效");
        }
        
        //String clientIp = IpUtil.getClientIp(request);
        UserDO user = userService.loginUser(
                loginRequest.getAddress(),
                loginRequest.getSignature(),
                loginRequest.buildSignMessage(),
                loginRequest.getTimestamp()
        );

        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.USER_IS_DISABLED);
        }
        
        String jwt = jwtUtil.generateToken(user.getAddress());
        
        JwtResponse jwtResponse = JwtResponse.builder()
                .token("Bearer " + jwt)
                .id(user.getId())
                .username(user.getAddress())
                .build();
        
        return ApiResponse.success(jwtResponse);
    }

    @Operation(summary = "查询绑定状态", description = "根据钱包地址返回是否已注册及是否已绑定上级；前端据此决定走登录还是绑定上级注册")
    @RateLimit(key = "bind-status", limitType = RateLimit.LimitType.IP)
    @GetMapping("/bind-status")
    public ApiResponse<BindStatusResponse> bindStatus(@RequestParam("address") String address) {
        if (EthereumAddressValidator.validateAddress(address)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "钱包地址格式无效");
        }

        boolean bound = userService.isBound(address);
        boolean registered = userService.isRegistered(address);

        BindStatusResponse response = BindStatusResponse.builder()
                .address(address)
                .registered(registered)
                .bound(bound)
                .build();

        return ApiResponse.success(response);
    }


}