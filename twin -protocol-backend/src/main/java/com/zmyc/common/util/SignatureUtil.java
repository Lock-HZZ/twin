package com.zmyc.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 签名工具类
 * 用于AK/SK认证的签名生成和验证
 */
@Slf4j
public class SignatureUtil {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    /**
     * 生成签名
     * 
     * @param method HTTP方法
     * @param timestamp 时间戳（秒）
     * @param nonce 随机数
     * @param body 请求体（可为空）
     * @param secretKey 密钥
     * @return 签名字符串
     */
    public static String generateSignature(String method, Long timestamp,
                                          String nonce, String body, String secretKey) {
        try {
            String stringToSign = buildStringToSign(method, timestamp, nonce, body);
            
            log.debug("待签名字符串: {}", stringToSign);
            
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
            byte[] signatureBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            return bytesToHex(signatureBytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("签名生成失败", e);
        }
    }
    
    /**
     * 验证签名
     * 
     * @param method HTTP方法
     * @param timestamp 时间戳（秒）
     * @param nonce 随机数
     * @param body 请求体（可为空）
     * @param signature 待验证的签名
     * @param secretKey 密钥
     * @return 是否验证通过
     */
    public static boolean verifySignature(String method, Long timestamp,
                                         String nonce, String body, String signature, String secretKey) {
        try {
            String calculatedSignature = generateSignature(method, timestamp, nonce, body, secretKey);
            // 使用MessageDigest.isEqual()进行常量时间比较，防止时序攻击
            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("验证签名失败", e);
            return false;
        }
    }
    
    /**
     * 构建待签名字符串
     * 格式: METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY
     */
    private static String buildStringToSign(String method, Long timestamp,
                                           String nonce, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.toUpperCase()).append("\n");
        sb.append(timestamp).append("\n");
        sb.append(nonce).append("\n");
        sb.append(body != null ? body : "");
        return sb.toString();
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * 生成随机Nonce
     */
    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 获取当前时间戳（秒）
     */
    public static Long getCurrentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }
    
    /**
     * 验证时间戳是否在有效期内
     * 
     * @param timestamp 时间戳（秒）
     * @param expirationSeconds 过期时间（秒）
     * @return 是否有效
     */
    public static boolean isTimestampValid(Long timestamp, Long expirationSeconds) {
        if (timestamp == null) {
            return false;
        }
        Long currentTimestamp = getCurrentTimestamp();
        Long diff = Math.abs(currentTimestamp - timestamp);
        return diff <= expirationSeconds;
    }
}

