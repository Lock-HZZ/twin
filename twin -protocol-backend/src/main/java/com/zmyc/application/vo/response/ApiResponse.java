package com.zmyc.application.vo.response;

import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.util.I18nUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "统一响应结果")
@Data
public class ApiResponse<T> {
    
    @Schema(description = "响应码，0表示成功", example = "0")
    private int code;
    
    @Schema(description = "响应消息", example = "操作成功")
    private String message;
    
    @Schema(description = "响应数据")
    private T data;
    
    public ApiResponse() {}
    
    public ApiResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // ========== 成功响应 ==========
    
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), I18nUtil.getMessage(ErrorCode.SUCCESS));
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), I18nUtil.getMessage(ErrorCode.SUCCESS), data);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), message, data);
    }
    
    // ========== 错误响应 ==========
    
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), I18nUtil.getMessage(errorCode));
    }
    
    public static <T> ApiResponse<T> error(ErrorCode errorCode, Object... args) {
        return new ApiResponse<>(errorCode.getCode(), I18nUtil.getMessage(errorCode, args));
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message);
    }
    
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), message);
    }
    
    // ========== 便捷方法：判断是否成功 ==========
    
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }
    
}