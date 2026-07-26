package com.zmyc.interceptor;

import com.zmyc.common.context.UserContext;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.common.util.JwtUtil;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器 - 简化版
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorizationHeader = request.getHeader("Authorization");
        
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException(com.zmyc.common.enums.ErrorCode.UNAUTHENTICATED);
        }
        
        String token = authorizationHeader.substring(7);
        
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(com.zmyc.common.enums.ErrorCode.UNAUTHENTICATED);
        }
        
        try {
            // 从JWT中提取用户地址
            String address = jwtUtil.extractUsername(token);
            
            // 验证token是否过期
            if (jwtUtil.isTokenExpired(token)) {
                throw new BusinessException(com.zmyc.common.enums.ErrorCode.AUTH_EXPIRED);
            }
            
            // 从数据库查询用户
            UserDO user = userRepository.findByAddress(address);
            
            // 检查用户是否被禁用
            if (user.getEnabled() == null || user.getEnabled() != 1) {
                throw new BusinessException(com.zmyc.common.enums.ErrorCode.USER_IS_DISABLED);
            }
            
            // 设置用户上下文
            UserContext.set(new UserContext(user.getId(), user.getAddress()));
            
            log.debug("JWT认证成功: userId={}, address={}", user.getId(), user.getAddress());
            
            return true;
            
        } catch (JwtException e) {
            log.warn("JWT验证失败: {}", e.getMessage());
            throw new BusinessException(com.zmyc.common.enums.ErrorCode.AUTH_INVALID);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("JWT验证异常", e);
            throw new BusinessException(com.zmyc.common.enums.ErrorCode.AUTH_ERROR);
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除用户上下文
        UserContext.clear();
    }
}

