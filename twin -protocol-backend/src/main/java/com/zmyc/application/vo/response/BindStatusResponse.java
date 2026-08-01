package com.zmyc.application.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "绑定状态响应")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindStatusResponse {

    @Schema(description = "钱包地址")
    private String address;

    @Schema(description = "是否已注册")
    private Boolean registered;

    @Schema(description = "是否已绑定上级（true 走登录，false 走绑定上级注册）")
    private Boolean bound;
}
