package com.zmyc.service;

import com.zmyc.application.vo.response.DepositResponse;
import com.zmyc.common.config.DepositConfig;
import com.zmyc.common.context.UserContext;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.common.util.Eip712DepositSigner;
import com.zmyc.common.util.TimeUtils;
import com.zmyc.domain.bo.UserQuotaInfo;
import com.zmyc.infrastructure.entity.*;
import com.zmyc.infrastructure.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserDepositService {

    private static final Logger log = LoggerFactory.getLogger(UserDepositService.class);

    /** USDC 精度：6 位小数 */
    private static final BigInteger USDC_DECIMALS = BigInteger.TEN.pow(6);

    /** 合约功能类型：入金固定为 2 */
    private static final int FUNC_DEPOSIT = 2;

    private static final BigDecimal MIN_DEPOSIT = BigDecimal.valueOf(100);
    private static final BigDecimal DEPOSIT_STEP = BigDecimal.valueOf(100);
    private static final BigDecimal UNLIMITED_QUOTA = BigDecimal.valueOf(999999999L);

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private UserDepositRepository depositRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepositNonceRepository depositNonceRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DepositConfig depositConfig;

    /**
     * 校验入金规则并生成 EIP-712 签名，前端凭签名调用合约 depositWithSig。
     *
     * @param amount 入金金额（USDC，必须是100整数倍）
     * @return 签名参数
     */
    @Transactional
    public DepositResponse createDepositSignature(BigDecimal amount) {
        Long userId = UserContext.getCurrentUserId();
        String userAddress = UserContext.getCurrentAddress();
        if (userAddress == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }

        UserDO user = userRepository.findById(userId);

        // 1. 校验入金金额
        validateDepositAmount(amount);

        // 2. 校验用户角色准入
        validateUserRole(user);

        // 3. 校验每日总额度（最高优先级）
        validateDailyLimit(amount);

        // 4. 校验用户个人额度
        BigDecimal userQuota = calculateUserQuota(user);
        BigDecimal used = getUserOccupiedQuota(userId);
        BigDecimal available = userQuota.subtract(used);
        if (amount.compareTo(available) > 0) {
            log.error("用户入金额度不足: userId={}, amount={}, available={}", userId, amount, available);
            throw new BusinessException(ErrorCode.DEPOSIT_QUOTA_EXCEEDED);
        }

        // 5. 生成唯一 nonce 并入库（防重放）
        BigInteger nonce = generateUniqueNonce(userId);

        // 6. 计算权重
        BigDecimal weight = calculateWeight(user);

        // 7. 写入 PENDING 入金记录，占用额度（链上事件确认后置为 COMPLETED）
        UserDepositDO deposit = new UserDepositDO();
        deposit.setUserId(userId);
        deposit.setAmount(amount);
        deposit.setWeight(weight);
        deposit.setStatus(UserDepositDO.Status.PENDING);
        deposit.setNonce(nonce.longValueExact());
        deposit.setExpiresAt(System.currentTimeMillis() / 1000 + depositConfig.getOrderExpirationSeconds());
        depositRepository.save(deposit);

        // 8. 生成 EIP-712 签名
        long deadline = System.currentTimeMillis() / 1000 + depositConfig.getSignatureTtlSeconds();
        BigInteger amountWei = amount.toBigInteger().multiply(USDC_DECIMALS);

        String signature = Eip712DepositSigner.sign(
                depositConfig.getSignerPrivateKey(),
                depositConfig.getDomainName(),
                depositConfig.getDomainVersion(),
                depositConfig.getChainId(),
                depositConfig.getContractAddress(),
                userAddress,
                amountWei,
                nonce,
                BigInteger.valueOf(deadline),
                FUNC_DEPOSIT
        );

        log.info("生成入金签名: userId={}, amount={}, nonce={}, deadline={}, depositId={}",
                userId, amount, nonce, deadline, deposit.getId());

        DepositResponse response = new DepositResponse();
        response.setUser(userAddress);
        response.setAmount(amountWei.toString());
        response.setNonce(nonce.toString());
        response.setDeadline(deadline);
        response.setFunctionType(FUNC_DEPOSIT);
        response.setSignature(signature);
        return response;
    }

    /**
     * 校验入金金额：最低100 USDC，且必须是100的整数倍
     */
    private void validateDepositAmount(BigDecimal amount) {
        if (amount.compareTo(MIN_DEPOSIT) < 0) {
            throw new BusinessException(ErrorCode.DEPOSIT_AMOUNT_INVALID);
        }
        if (amount.remainder(DEPOSIT_STEP).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ErrorCode.DEPOSIT_AMOUNT_INVALID);
        }
    }

    /**
     * 校验用户角色是否允许入金
     */
    private void validateUserRole(UserDO user) {
        String allowedRoles = systemConfigRepository.getAllowedDepositLevels();
        Set<Integer> allowedSet = new HashSet<>();
        for (String role : allowedRoles.split(",")) {
            if (!role.isBlank()) {
                allowedSet.add(Integer.parseInt(role.trim()));
            }
        }

        int currentRole = (user.getRole() != null) ? user.getRole() : UserDO.Role.NORMAL;

        if (!allowedSet.contains(currentRole)) {
            log.warn("用户角色不允许入金: userId={}, role={}, allowed={}", user.getId(), currentRole, allowedRoles);
            throw new BusinessException(ErrorCode.DEPOSIT_NOT_ALLOWED);
        }
    }

    /**
     * 校验每日总额度（Daily Max Deposit，最高优先级）
     * 每日已用额度按当天时间范围实时聚合 PENDING + COMPLETED 订单，过期订单自动排除
     */
    private void validateDailyLimit(BigDecimal amount) {
        BigDecimal dailyMax = systemConfigRepository.getDailyMaxDeposit();
        long todayStart = TimeUtils.getTodayZeroTimestamp();
        long todayEnd = TimeUtils.getTodayEndTimestamp();
        BigDecimal todayTotal = depositRepository.getGlobalDailyOccupiedAmount(todayStart, todayEnd);

        if (todayTotal.add(amount).compareTo(dailyMax) > 0) {
            log.warn("超过每日总额度: todayTotal={}, amount={}, limit={}", todayTotal, amount, dailyMax);
            throw new BusinessException(ErrorCode.DEPOSIT_DAILY_LIMIT_EXCEEDED);
        }
    }

    /**
     * 计算用户个人额度（基于注册天数 + 推荐解锁）
     */
    private BigDecimal calculateUserQuota(UserDO user) {
        long days = daysSinceRegistration(user);

        BigDecimal baseQuota;
        if (days < 15) {
            baseQuota = BigDecimal.valueOf(100);
        } else if (days < 30) {
            baseQuota = BigDecimal.valueOf(1000);
        } else {
            baseQuota = BigDecimal.valueOf(10000);
        }

        int validReferrals = userService.countValidReferrals(user.getId());

        // 推荐满10人解除限制
        if (validReferrals >= 10) {
            return UNLIMITED_QUOTA;
        }

        return baseQuota.add(BigDecimal.valueOf(validReferrals * 1000L));

    }

    /**
     * 计算权重：1 + 注册天数 × 增长率
     */
    private BigDecimal calculateWeight(UserDO user) {
        long days = daysSinceRegistration(user);
        BigDecimal growthRate = systemConfigRepository.getWeightGrowthRate();
        return BigDecimal.ONE
                .add(growthRate.multiply(BigDecimal.valueOf(days)))
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 用户已占用额度（PENDING + COMPLETED 的入金总额）
     */
    private BigDecimal getUserOccupiedQuota(Long userId) {
        return depositRepository.getOccupiedAmount(userId);
    }

    /**
     * 生成唯一 nonce 并入库
     */
    private BigInteger generateUniqueNonce(Long userId) {
        for (int i = 0; i < 5; i++) {
            long candidate = Math.abs(secureRandom.nextLong() % 1_000_000_000_000_000L)
                    + System.currentTimeMillis();
            if (!depositNonceRepository.isNonceUsed(userId, candidate)) {
                DepositNonceDO nonceDO = new DepositNonceDO();
                nonceDO.setUserId(userId);
                nonceDO.setNonce(candidate);
                nonceDO.setUsedAt(System.currentTimeMillis() / 1000);
                depositNonceRepository.save(nonceDO);
                return BigInteger.valueOf(candidate);
            }
        }
        throw new BusinessException(ErrorCode.DEPOSIT_NONCE_USED);
    }

    /**
     * 获取用户额度信息
     */
    public UserQuotaInfo getUserQuotaInfo(Long userId) {
        UserDO user = userRepository.findById(userId);
        BigDecimal totalQuota = calculateUserQuota(user);
        BigDecimal used = getUserOccupiedQuota(userId);
        BigDecimal available = totalQuota.subtract(used);

        BigDecimal dailyMax = systemConfigRepository.getDailyMaxDeposit();
        long todayStart = TimeUtils.getTodayZeroTimestamp();
        long todayEnd = TimeUtils.getTodayEndTimestamp();
        BigDecimal todayTotal = depositRepository.getGlobalDailyOccupiedAmount(todayStart, todayEnd);

        UserQuotaInfo info = new UserQuotaInfo();
        info.setTotalQuota(totalQuota);
        info.setUsedQuota(used);
        info.setAvailableQuota(available.max(BigDecimal.ZERO));
        info.setDailyMaxDeposit(dailyMax);
        info.setDailyUsed(todayTotal);
        info.setDailyRemaining(dailyMax.subtract(todayTotal).max(BigDecimal.ZERO));
        return info;
    }

    private long daysSinceRegistration(UserDO user) {
        Long createdDate = user.getCreatedDate();
        if (createdDate == null) {
            return 0;
        }
        long now = System.currentTimeMillis() / 1000;
        return Math.max(0, (now - createdDate) / (24 * 60 * 60));
    }

    @Transactional
    public int cancelExpiredOrders() {
        long now = System.currentTimeMillis() / 1000;
        int count = depositRepository.expirePendingOrders(now);
        if (count > 0) {
            log.info("入金订单过期清理：已取消 {} 个订单并释放额度", count);
        }
        return count;
    }
}
