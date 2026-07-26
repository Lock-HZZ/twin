package com.zmyc.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户上下文信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 钱包地址
     */
    private String address;
    
    /**
     * ThreadLocal存储当前用户信息
     */
    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置当前用户信息
     */
    public static void set(UserContext context) {
        CONTEXT_HOLDER.set(context);
    }
    
    /**
     * 获取当前用户信息
     */
    public static UserContext get() {
        return CONTEXT_HOLDER.get();
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        UserContext context = get();
        return context != null ? context.getUserId() : null;
    }
    
    /**
     * 获取当前用户地址
     */
    public static String getCurrentAddress() {
        UserContext context = get();
        return context != null ? context.getAddress() : null;
    }

    /**
     * 获取当前用户地址（别名方法）
     */
    public static String getCurrentUserAddress() {
        return getCurrentAddress();
    }

    /**
     * 清除当前用户信息
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}

