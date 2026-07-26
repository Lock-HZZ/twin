package com.zmyc.common.exception;

import com.zmyc.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final Object[] args;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args = null;
    }
    
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args = args;
    }
    
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessageKey(), cause);
        this.errorCode = errorCode;
        this.args = null;
    }
    
    public BusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.getMessageKey(), cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}

