package com.zmyc.service;

import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.entity.UserRelationClosureDO;
import com.zmyc.infrastructure.repository.UserDepositRepository;
import com.zmyc.infrastructure.repository.UserPerformanceRepository;
import com.zmyc.infrastructure.repository.UserRelationClosureRepository;
import com.zmyc.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRelationClosureRepository closureRepository;

    @Autowired
    private UserPerformanceRepository performanceRepository;

    @Autowired
    private UserDepositRepository depositRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;


    private static final String SIGNATURE_NONCE_PREFIX = "WALLET_SIGNATURE:";
    private static final int SIGNATURE_EXPIRATION_SECONDS = 60; // 签名有效期60秒

    /**
     * 注册新用户（公开方法，供其他服务调用）
     * 修复：先 save 拿到 snowflake id，再建立闭包表关系。
     */
    @Transactional
    public UserDO registerUser(String address, String invitedCode, String clientIp) {
        UserDO newUser = new UserDO();
        newUser.setAddress(address);
        newUser.setEnabled((byte) 1);
        newUser.setRegistrationIp(clientIp);
        newUser.setLastLoginIp(clientIp);

        String newInvitedCode = RandomStringUtils.randomAlphanumeric(8);
        newUser.setInvitedCode(newInvitedCode);

        // 先落库，IdType.ASSIGN_ID 会在此时回填 id
        userRepository.save(newUser);

        if (invitedCode != null && !invitedCode.isEmpty()) {
            UserDO inviter = userRepository.findByInvitedCodeOrNull(invitedCode);
            if (inviter != null) {
                closureRepository.insertForNewUser(newUser.getId(), inviter.getId());
            }
        }

        // 初始化业绩记录
        performanceRepository.initForNewUser(newUser.getId());

        return newUser;
    }

    /**
     * 查询某地址的绑定状态。
     * @return 该地址是否已注册且已绑定直接上级（可直接登录）
     */
    public boolean isBound(String address) {
        UserDO user = userRepository.findByAddressOrNull(address);
        if (user == null) {
            return false;
        }
        return closureRepository.hasParent(user.getId());
    }

    /**
     * 查询某地址是否已注册。
     */
    public boolean isRegistered(String address) {
        return userRepository.findByAddressOrNull(address) != null;
    }

    /**
     * 校验签名后，以「上级钱包地址」注册新用户并绑定上级。
     * 要求：签名有效、地址未注册、上级存在且不是自己。
     */
    @Transactional
    public UserDO registerWithParent(String address, String signature, String message,
                                     Long timestamp, String parentAddress, String clientIp) {
        // 1. 校验签名（verifySignature 返回 true 表示校验未通过）
        if (verifySignature(address, signature, message, timestamp)) {
            throw new BusinessException(ErrorCode.SIGNATURE_INVALID);
        }

        // 2. 地址不能已注册
        if (userRepository.findByAddressOrNull(address) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 3. 上级地址必填且必须存在
        if (parentAddress == null || parentAddress.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARENT_REQUIRED);
        }
        if (parentAddress.equalsIgnoreCase(address)) {
            throw new BusinessException(ErrorCode.PARENT_IS_SELF);
        }
        UserDO parent = userRepository.findByAddressOrNull(parentAddress);
        if (parent == null) {
            throw new BusinessException(ErrorCode.PARENT_NOT_EXISTS);
        }

        // 4. 创建用户并绑定上级
        UserDO newUser = new UserDO();
        newUser.setAddress(address);
        newUser.setEnabled((byte) 1);
        newUser.setRegistrationIp(clientIp);
        newUser.setLastLoginIp(clientIp);
        newUser.setInvitedCode(RandomStringUtils.randomAlphanumeric(8));

        userRepository.save(newUser);
        closureRepository.insertForNewUser(newUser.getId(), parent.getId());
        performanceRepository.initForNewUser(newUser.getId());

        return newUser;
    }

    public UserDO loginUser(String address, String signature, String message, Long timestamp) {
        if (verifySignature(address, signature, message, timestamp)) {
            throw new BusinessException(ErrorCode.SIGNATURE_INVALID);
        }

        UserDO user = userRepository.findByAddress(address);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        return user;
    }

    /**
     * 统计有效推荐人数（直推中拥有进行中订单的用户）
     *
     * @param userId 用户ID
     * @return 有效推荐人数
     */
    public int countValidReferrals(Long userId) {
        var children = closureRepository.findDirectChildren(userId);
        int count = 0;
        for (UserRelationClosureDO child : children) {
            if (depositRepository.countActiveOrders(child.getDescendantId()) > 0) {
                count++;
            }
        }
        return count;
    }

    public boolean verifySignature(String address, String signature, String message, Long timestamp) {
        try {
            // 1. 验证时间戳是否在有效期内
            if (timestamp == null) {
                log.warn("时间戳为空: address={}", address);
                return true;
            }

            long currentTime = System.currentTimeMillis() / 1000;
            long timeDiff = Math.abs(currentTime - timestamp);
            if (timeDiff > SIGNATURE_EXPIRATION_SECONDS) {
                log.error("签名已过期: address={}, timeDiff={}s", address, timeDiff);
                return true;
            }

            // 2. 验证签名本身
            if (!verifySignatureOnly(address, signature, message)) {
                return true;
            }

            // 3. 防重放验证：检查签名是否已被使用
            String signatureHash = generateSignatureHash(signature, message);
            String redisKey = SIGNATURE_NONCE_PREFIX + signatureHash;

            Boolean exists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                log.error("签名已被使用（重放攻击） - address: {}, signature: {}", address, signature.substring(0, 10) + "...");
                return true;
            }

            // 4. 将签名标记为已使用，过期时间为签名有效期的2倍
            redisTemplate.opsForValue().set(redisKey, "USED", SIGNATURE_EXPIRATION_SECONDS * 2, TimeUnit.SECONDS);

            log.info("签名验证成功: address={}", address);
            return false;

        } catch (Exception e) {
            log.error("签名验证异常: address={}", address, e);
            return true;
        }
    }

    /**
     * 生成签名哈希（用于防重放）
     * 使用SHA-256对签名+消息进行哈希
     */
    private String generateSignatureHash(String signature, String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = signature + "|" + message;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("生成签名哈希失败", e);
            return signature; // 降级使用原始签名
        }
    }

    private boolean verifySignatureOnly(String address, String signature, String message) {
        try {
            // 移除0x前缀
            String cleanSignature = Numeric.cleanHexPrefix(signature);

            if (cleanSignature.length() != 130) {
                log.warn("签名长度不正确: {}", cleanSignature.length());
                return false;
            }

            // 解析签名
            byte[] r = Numeric.hexStringToByteArray(cleanSignature.substring(0, 64));
            byte[] s = Numeric.hexStringToByteArray(cleanSignature.substring(64, 128));
            byte[] v = Numeric.hexStringToByteArray(cleanSignature.substring(128, 130));

            // 构造以太坊消息前缀
            String prefix = "\u0019Ethereum Signed Message:\n" + message.length();
            byte[] msgHash = org.web3j.crypto.Hash.sha3((prefix + message).getBytes(StandardCharsets.UTF_8));

            // 恢复公钥
            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            BigInteger publicKey = Sign.signedMessageHashToKey(msgHash, signatureData);

            if (publicKey == null) {
                log.warn("无法从签名恢复公钥");
                return false;
            }

            // 从公钥计算地址
            String recoveredAddress = "0x" + Keys.getAddress(publicKey);

            // 比较地址（忽略大小写）
            boolean isValid = recoveredAddress.equalsIgnoreCase(address);

            if (!isValid) {
                log.warn("签名验证失败: expected={}, recovered={}", address, recoveredAddress);
            }

            return isValid;

        } catch (Exception e) {
            log.error("签名验证异常: address={}", address, e);
            return false;
        }
    }

}