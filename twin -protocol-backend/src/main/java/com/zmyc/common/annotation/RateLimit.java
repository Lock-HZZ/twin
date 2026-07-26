package com.zmyc.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记需要限流的接口
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 限流key的前缀
     */
    String key() default "rate_limit";
    
    /**
     * 时间窗口（秒）
     */
    int timeWindow() default 60;
    
    /**
     * 时间窗口内最大请求次数
     */
    int maxRequests() default 10;
    
    /**
     * 限流类型：IP或USER
     */
    LimitType limitType() default LimitType.IP;
    
    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 基于IP地址限流
         */
        IP,
        /**
         * 基于用户ID限流
         */
        USER,
        /**
         * 基于IP和用户ID组合限流
         */
        IP_AND_USER
    }
}

