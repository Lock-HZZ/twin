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
    USER_IS_DISABLED(1002, "error.user.is.disabled"),
    USER_ALREADY_EXISTS(1003, "error.user.already.exists"),
    PARENT_NOT_EXISTS(1004, "error.parent.not.exists"),
    PARENT_IS_SELF(1005, "error.parent.is.self"),
    PARENT_REQUIRED(1006, "error.parent.required"),

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
    DEPOSIT_NOT_FOUND(5008, "error.deposit.not.found"),
    DEPOSIT_NOT_BELONG_TO_USER(5009, "error.deposit.not.belong.to.user"),
    DEPOSIT_NOT_COMPLETED(5010, "error.deposit.not.completed"),
    WITHDRAW_AMOUNT_INVALID(5011, "error.withdraw.amount.invalid"),
    WITHDRAW_AMOUNT_EXCEEDS_LIQUIDITY(5012, "error.withdraw.amount.exceeds.liquidity"),
    DEPOSIT_ALREADY_PROCESSING(5013, "error.deposit.already.processing"),
    CONTRACT_CALL_FAILED(5014, "error.contract.call.failed"),

    // ========== 质押相关错误 6000-6999 ==========
    STAKE_PLAN_INVALID(6001, "error.stake.plan.invalid"),
    STAKE_NOT_FOUND(6002, "error.stake.not.found"),
    STAKE_ALREADY_WITHDRAWN(6003, "error.stake.already.withdrawn"),
    STAKE_NOT_MATURED(6004, "error.stake.not.matured"),
    NO_PERMISSION(6005, "error.no.permission"),
    RECORD_NOT_FOUND(6006, "error.record.not.found"),

    SIGNATURE_INVALID(7001, "error.signature.invalid"),
    NONCE_REUSED(7002, "error.nonce.reused"),
    TIMESTAMP_INVALID(7003, "error.timestamp.invalid"),
    RATE_LIMIT_EXCEEDED(7004, "error.rate.limit.exceeded");

    private final int code;
    private final String messageKey;
    
    ErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

}

