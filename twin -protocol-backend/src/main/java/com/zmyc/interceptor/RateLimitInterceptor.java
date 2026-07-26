package com.zmyc.interceptor;

import com.zmyc.common.annotation.RateLimit;
import com.zmyc.common.context.UserContext;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器
 * 基于Redis实现分布式限流
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final StringRedisTemplate redisTemplate;
    
    private static final String RATE_LIMIT_PREFIX = "RATE_LIMIT:";
    
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                           @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return true;
        }
        
        String limitKey = buildLimitKey(request, rateLimit);
        
        if (isRateLimited(limitKey, rateLimit.timeWindow(), rateLimit.maxRequests())) {
            log.error("请求被限流 - Key: {}, IP: {}, URI: {}",
                    limitKey, IpUtil.getClientIp(request), request.getRequestURI());
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        
        return true;
    }
    
    /**
     * 构建限流key
     */
    private String buildLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder(RATE_LIMIT_PREFIX);
        keyBuilder.append(rateLimit.key()).append(":");
        
        switch (rateLimit.limitType()) {
            case IP:
                keyBuilder.append(IpUtil.getClientIp(request));
                break;
            case USER:
                Long userId = UserContext.getCurrentUserId();
                keyBuilder.append(userId != null ? userId : "anonymous");
                break;
            case IP_AND_USER:
                Long uid = UserContext.getCurrentUserId();
                keyBuilder.append(IpUtil.getClientIp(request))
                         .append(":")
                         .append(uid != null ? uid : "anonymous");
                break;
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 检查是否被限流
     * 使用Redis的INCR和EXPIRE实现滑动窗口限流
     */
    private boolean isRateLimited(String key, int timeWindow, int maxRequests) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            
            if (count == null) {
                return false;
            }
            
            if (count == 1) {
                redisTemplate.expire(key, timeWindow, TimeUnit.SECONDS);
            }
            
            return count > maxRequests;
            
        } catch (Exception e) {
            log.error("限流检查异常", e);
            return false;
        }
    }
}

