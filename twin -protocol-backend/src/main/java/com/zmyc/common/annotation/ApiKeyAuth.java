package com.zmyc.common.annotation;

import java.lang.annotation.*;

/**
 * API Key认证注解
 * 用于标记需要第三方API Key认证的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiKeyAuth {
}

