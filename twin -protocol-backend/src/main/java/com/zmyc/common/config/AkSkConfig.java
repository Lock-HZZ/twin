package com.zmyc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * AK/SK配置类
 * 支持多个第三方客户端
 */
@Data
@Component
@ConfigurationProperties(prefix = "aksk")
public class AkSkConfig {
    
    private Long signatureExpiration = 300L;
    
    private Boolean enabled = true;

    private String accessKey;

    private String secretKey;
    
    /**
     * 使用常量时间比较防止时序攻击
     */
    public boolean hasAccessKey(String accessKey) {
        return MessageDigest.isEqual(
                this.accessKey.getBytes(StandardCharsets.UTF_8),
                accessKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}

