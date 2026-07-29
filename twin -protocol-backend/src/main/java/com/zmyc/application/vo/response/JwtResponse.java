package com.zmyc.application.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "JWT登录响应")
@Data
@Builder
public class JwtResponse {
    
    @Schema(description = "JWT Token", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "用户名（钱包地址）", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String username;
    
    @Schema(description = "用户ID", example = "1")
    private Long id;

}