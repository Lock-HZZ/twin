package com.zmyc.service;

import com.zmyc.application.vo.response.DepositResponse;
import com.zmyc.application.vo.response.PageResponse;
import com.zmyc.application.vo.response.UserDepositResponse;
import com.zmyc.common.config.DepositConfig;
import com.zmyc.common.context.UserContext;
import com.zmyc.common.enums.Decimals;
import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.common.util.Eip712DepositSigner;
import com.zmyc.common.util.TimeUtils;
import com.zmyc.domain.bo.UserQuotaInfo;
import com.zmyc.infrastructure.entity.*;
import com.zmyc.infrastructure.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserDepositService {

    /** 合约功能类型：入金固定为 2 */
    private static final int FUNC_DEPOSIT = 2;

    private static final BigDecimal MIN_DEPOSIT = BigDecimal.valueOf(100);
    private static final BigDecimal DEPOSIT_STEP = BigDecimal.valueOf(100);
    private static final BigDecimal UNLIMITED_QUOTA = BigDecimal.valueOf(999999999L);


    @Autowired
    private UserDepositRepository depositRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DepositConfig depositConfig;

    @Autowired
    private UserRelationService userRelationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${system.start.timestamp}")
    private long systemStartTimestamp;

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
        BigDecimal weight = calculateWeight();

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
        BigInteger amountWei = amount.toBigInteger().multiply(Decimals.USDC.value.toBigInteger()); // 转换为最小单位（USDC 6位小数）

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
     * 处理链上入金事件
     *
     * @param nonce 入金订单的随机数
     * @param amount 入金数量（USDC）
     * @param txHash 交易哈希
     */
    @Transactional
    public void processDeposit(Long nonce, BigDecimal liquidity, BigDecimal amount, String txHash) {

        // 检查交易是否已处理
        UserDepositDO existingDeposit = depositRepository.findByTxHash(txHash);
        if (existingDeposit != null) {
            log.warn("入金交易已存在，跳过处理: txHash={}", txHash);
            return;
        }

        UserDepositDO deposit = depositRepository.findByNonce(nonce);

        // 获取能量倍率
        BigDecimal energyMultiplier = systemConfigRepository.getDepositEnergyMultiplier();

        // 计算能量值
        BigDecimal energyEarned = amount.multiply(energyMultiplier);

        // 更新入金记录
        deposit.setTxHash(txHash);
        deposit.setLiquidity(liquidity);
        deposit.setEnergyEarned(energyEarned);
        deposit.setEnergyMultiplier(energyMultiplier);
        deposit.setStatus(UserDepositDO.Status.COMPLETED);
        depositRepository.save(deposit);

        log.info("创建入金记录: nonce={}, amount={} USDC, energyEarned={}",
                nonce, amount, energyEarned);

        // 触发业绩统计和能量值增加
        userRelationService.onDeposit(deposit.getUserId(), amount, deposit.getId());

        log.info("入金处理完成: userId={}, txHash={}, nonce={}", deposit.getUserId(), txHash, nonce);
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
    private BigDecimal calculateWeight() {
        long now = System.currentTimeMillis() / 1000;
        long days = Math.max(0, (now - systemStartTimestamp) / (24 * 60 * 60));
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
        String key = "deposit:nonce:" + userId;
        Long nonce = redisTemplate.opsForValue().increment(key);
        return BigInteger.valueOf(nonce);
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
        long now = TimeUtils.now();
        return Math.max(0, (now - createdDate) / (24 * 60 * 60));
    }

    @Transactional
    public int cancelExpiredOrders() {
        long now = TimeUtils.now();
        int count = depositRepository.expirePendingOrders(now);
        if (count > 0) {
            log.info("入金订单过期清理：已取消 {} 个订单并释放额度", count);
        }
        return count;
    }

    /**
     * 分页查询用户入金列表
     *
     * @param userId 用户ID
     * @param statusFilter 状态筛选：1-持仓中(COMPLETED)，3-已出局(EXPIRED/FAILED)，null-全部
     * @param page 页码，从1开始
     * @param pageSize 每页数量
     * @return 分页结果
     */
    public PageResponse<UserDepositResponse> getUserDepositList(Long userId, Integer statusFilter, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;

        List<UserDepositDO> deposits = depositRepository.findByUserIdWithPage(userId, statusFilter, offset, pageSize);
        long total = depositRepository.countByUserId(userId, statusFilter);

        long now = System.currentTimeMillis() / 1000;

        List<UserDepositResponse> responseList = deposits.stream().map(deposit -> {
            UserDepositResponse response = new UserDepositResponse();
            response.setId(deposit.getId());
            response.setLiquidity(deposit.getLiquidity());
            response.setAmount(deposit.getAmount());
            response.setWeight(deposit.getWeight());

            // 订单总权重 = 权重参数 * 订单价值
            BigDecimal totalWeight = deposit.getWeight() != null && deposit.getAmount() != null
                ? deposit.getWeight().multiply(deposit.getAmount())
                : BigDecimal.ZERO;
            response.setTotalWeight(totalWeight);

            // 持仓天数 = (当前时间 - 创建时间) / 86400
            long holdingDays = 0;
            if (deposit.getCreatedDate() != null) {
                holdingDays = (now - deposit.getCreatedDate()) / (24 * 60 * 60);
            }
            response.setHoldingDays(holdingDays);

            response.setTxHash(deposit.getTxHash());
            response.setStatus(deposit.getStatus());
            response.setCreatedDate(deposit.getCreatedDate());

            return response;
        }).collect(Collectors.toList());

        return new PageResponse<>(responseList, total, page, pageSize);
    }
}
