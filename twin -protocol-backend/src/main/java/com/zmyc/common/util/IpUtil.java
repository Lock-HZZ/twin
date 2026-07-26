package com.zmyc.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * IP地址工具类
 */
@Slf4j
public class IpUtil {
    
    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    /**
     * 获取客户端真实IP地址
     * 支持通过代理、负载均衡等方式访问的场景
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            log.warn("HttpServletRequest is null, cannot get client IP");
            return UNKNOWN;
        }
        
        String ip = null;
        
        // 1. X-Forwarded-For: 代理服务器传递的原始客户端IP
        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index);
            }
            return ip.trim();
        }
        
        // 2. X-Real-IP: Nginx等反向代理使用
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        
        // 3. Proxy-Client-IP: Apache服务器使用
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        
        // 4. WL-Proxy-Client-IP: WebLogic服务器使用
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        
        // 5. HTTP_CLIENT_IP
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        
        // 6. HTTP_X_FORWARDED_FOR
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        
        // 7. 如果以上都没有，使用request.getRemoteAddr()
        ip = request.getRemoteAddr();
        
        if (LOCALHOST_IPV6.equals(ip)) {
            ip = LOCALHOST_IPV4;
        }
        
        return ip != null ? ip.trim() : UNKNOWN;
    }
    
    /**
     * 验证IP地址是否有效
     * 
     * @param ip IP地址
     * @return true表示有效，false表示无效
     */
    private static boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        
        if (UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 判断IP地址是否为内网IP
     * 
     * @param ip IP地址
     * @return true表示是内网IP，false表示不是
     */
    public static boolean isInternalIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        
        if (LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip)) {
            return true;
        }
        
        String[] ipParts = ip.split("\\.");
        if (ipParts.length != 4) {
            return false;
        }
        
        try {
            int firstPart = Integer.parseInt(ipParts[0]);
            int secondPart = Integer.parseInt(ipParts[1]);
            
            // 10.x.x.x
            if (firstPart == 10) {
                return true;
            }
            
            // 172.16.x.x - 172.31.x.x
            if (firstPart == 172 && secondPart >= 16 && secondPart <= 31) {
                return true;
            }
            
            // 192.168.x.x
            if (firstPart == 192 && secondPart == 168) {
                return true;
            }
            
        } catch (NumberFormatException e) {
            log.warn("Invalid IP format: {}", ip);
            return false;
        }
        
        return false;
    }
}

