package com.zmyc.application.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "登录请求")
@Data
public class LoginRequest {
    
    @Schema(description = "钱包地址")
    @NotBlank(message = "地址不能为空")
    private String address;

    @Schema(description = "时间戳")
    @NotNull(message = "时间戳不能为空")
    private Long timestamp;

    @Schema(description = "钱包签名" )
    @NotBlank(message = "签名不能为空")
    private String signature;

    @Schema(description = "邀请码" )
    private String invitedCode;

    public String buildSignMessage() {
        return address + "|login by wallet|" + timestamp;
    }
}