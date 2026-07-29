package com.zmyc.interceptor;

import com.zmyc.common.annotation.ApiKeyAuth;
import com.zmyc.common.config.AkSkConfig;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.common.util.SignatureUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AkSkInterceptor implements HandlerInterceptor {
    
    private final AkSkConfig akSkConfig;
    
    private static final String HEADER_ACCESS_KEY = "X-Access-Key";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";

    private final StringRedisTemplate redisTemplate;

    private static final String NONCE_PREFIX = "NONCE:";


    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                           @NonNull Object handler) {
        // 只处理标注了@ApiKeyAuth注解的方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        ApiKeyAuth apiKeyAuth = handlerMethod.getMethodAnnotation(ApiKeyAuth.class);
        
        if (apiKeyAuth == null) {
            apiKeyAuth = handlerMethod.getBeanType().getAnnotation(ApiKeyAuth.class);
        }
        
        if (apiKeyAuth == null || !akSkConfig.getEnabled()) {
            return true;
        }
        
        try {
            String accessKey = request.getHeader(HEADER_ACCESS_KEY);
            String timestampStr = request.getHeader(HEADER_TIMESTAMP);
            String nonce = request.getHeader(HEADER_NONCE);
            String signature = request.getHeader(HEADER_SIGNATURE);
            
            if (!StringUtils.hasText(accessKey)) {
                log.warn("Access Key缺失 - IP: {}, URI: {}", getClientIp(request), request.getRequestURI());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            
            if (!StringUtils.hasText(timestampStr)) {
                log.error("时间戳缺失 - IP: {}, URI: {}", getClientIp(request), request.getRequestURI());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            
            if (!StringUtils.hasText(nonce)) {
                log.error("Nonce缺失 - IP: {}, URI: {}", getClientIp(request), request.getRequestURI());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            
            if (!StringUtils.hasText(signature)) {
                log.error("签名缺失 - IP: {}, URI: {}", getClientIp(request), request.getRequestURI());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            
            // 使用MessageDigest.isEqual()防止时序攻击
            if (!MessageDigest.isEqual(
                    accessKey.getBytes(StandardCharsets.UTF_8),
                    akSkConfig.getAccessKey().getBytes(StandardCharsets.UTF_8))) {
                log.error("Access Key无效 - IP: {}, URI: {}, AccessKey: {}",
                        getClientIp(request), request.getRequestURI(), accessKey);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            
            Long timestamp;
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                log.error("时间戳格式错误 - IP: {}, URI: {}, Timestamp: {}",
                        getClientIp(request), request.getRequestURI(), timestampStr);
                throw new BusinessException(ErrorCode.TIMESTAMP_INVALID);
            }
            
            if (!SignatureUtil.isTimestampValid(timestamp, akSkConfig.getSignatureExpiration())) {
                log.error("时间戳已过期 - IP: {}, URI: {}, Timestamp: {}, Current: {}",
                        getClientIp(request), request.getRequestURI(), timestamp, SignatureUtil.getCurrentTimestamp());
                throw new BusinessException(ErrorCode.SIGNATURE_EXPIRED);
            }
            
            Boolean exists = redisTemplate.hasKey(NONCE_PREFIX + nonce);
            if (Boolean.TRUE.equals(exists)) {
                log.error("Nonce已使用（重放攻击） - IP: {}, URI: {}, Nonce: {}",
                        getClientIp(request), request.getRequestURI(), nonce);
                throw new BusinessException(ErrorCode.NONCE_REUSED);
            }

            // 设置Nonce过期时间，防止Redis重启后失效导致重放攻击
            // 过期时间设置为签名有效期的2倍，确保在签名有效期内Nonce不会被重用
            long expirationSeconds = akSkConfig.getSignatureExpiration() * 2;
            redisTemplate.opsForValue().set(NONCE_PREFIX + nonce, "USED", expirationSeconds, TimeUnit.SECONDS);

            String body = getRequestBody(request);
            
            //验证签名
            String secretKey = akSkConfig.getSecretKey();
            String method = request.getMethod();
            
            boolean isValid = SignatureUtil.verifySignature(
                    method, timestamp, nonce, body, signature, secretKey);
            
            if (!isValid) {
                log.error("签名验证失败 - IP: {}, URI: {}, AccessKey: {}",
                        getClientIp(request), request.getRequestURI(), accessKey);
                throw new BusinessException(ErrorCode.SIGNATURE_INVALID);
            }
            
            log.info("AK/SK认证成功 - IP: {}, URI: {}, AccessKey: {}", 
                    getClientIp(request), request.getRequestURI(), accessKey);
            
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AK/SK认证异常", e);
            throw new BusinessException(ErrorCode.AUTH_ERROR);
        }
    }
    
    /**
     * 获取请求体内容
     */
    private String getRequestBody(HttpServletRequest request) {
        // 尝试从自定义的缓存包装器获取
        if (request instanceof com.zmyc.common.config.WebConfig.CachedBodyHttpServletRequest) {
            com.zmyc.common.config.WebConfig.CachedBodyHttpServletRequest wrapper = 
                (com.zmyc.common.config.WebConfig.CachedBodyHttpServletRequest) request;
            byte[] buf = wrapper.getCachedBody();
            if (buf != null && buf.length > 0) {
                String body = new String(buf, StandardCharsets.UTF_8);
                log.debug("从CachedBodyHttpServletRequest读取到请求体: {}", body);
                return body;
            }
        }
        
        // 尝试从ContentCachingRequestWrapper获取
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                String body = new String(buf, StandardCharsets.UTF_8);
                log.debug("从ContentCachingRequestWrapper读取到请求体: {}", body);
                return body;
            }
        }
        
        log.warn("无法读取请求体，返回空字符串");
        return "";
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

