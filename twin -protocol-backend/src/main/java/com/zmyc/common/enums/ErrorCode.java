package com.zmyc.common.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    
    // ========== 通用错误 ==========
    SUCCESS(200, "error.success"),
    SYSTEM_ERROR(9000, "error.system.error"),
    PARAM_ERROR(9001, "error.param.error"),
    SYSTEM_BUSY(9002, "error.system.busy"),
    
    // ========== 认证相关错误 1000-1999 ==========
    AUTH_FAILED(1000, "error.auth.failed"),

    USER_NOT_EXISTS(1001, "error.user.not.exists"),

    TOKEN_NOT_FOUND(2001, "error.token.not.found"),

    INSUFFICIENT_BALANCE(3001, "error.insufficient.balance"),

    EVENT_PROCESSOR_NOT_FOUND(4001,  "error.event.processor.not.found"),

    TRANSACTION_STATUS_FAILED(4002, "error.transaction.status.failed"),

    TRANSACTION_NOT_FOUND(4003, "error.transaction.not.found"),

    // ========== 入金相关错误 5000-5999 ==========
    DEPOSIT_AMOUNT_INVALID(5001, "error.deposit.amount.invalid"),
    DEPOSIT_QUOTA_EXCEEDED(5002, "error.deposit.quota.exceeded"),
    DEPOSIT_DAILY_LIMIT_EXCEEDED(5003, "error.deposit.daily.limit.exceeded"),
    DEPOSIT_NOT_ALLOWED(5004, "error.deposit.not.allowed"),
    DEPOSIT_NONCE_USED(5005, "error.deposit.nonce.used"),
    DEPOSIT_SIGNATURE_EXPIRED(5006, "error.deposit.signature.expired"),
    DEPOSIT_SIGNATURE_INVALID(5007, "error.deposit.signature.invalid"),

    // ========== 质押相关错误 6000-6999 ==========
    STAKE_PLAN_INVALID(6001, "error.stake.plan.invalid"),
    STAKE_NOT_FOUND(6002, "error.stake.not.found"),
    STAKE_ALREADY_WITHDRAWN(6003, "error.stake.already.withdrawn"),
    STAKE_NOT_MATURED(6004, "error.stake.not.matured"),
    NO_PERMISSION(6005, "error.no.permission"),
    RECORD_NOT_FOUND(6006, "error.record.not.found");

    private final int code;
    private final String messageKey;
    
    ErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

}

