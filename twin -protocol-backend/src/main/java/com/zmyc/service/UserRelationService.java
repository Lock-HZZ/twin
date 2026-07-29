package com.zmyc.service;

import com.zmyc.infrastructure.entity.EnergyTransactionDO;
import com.zmyc.infrastructure.entity.UserEnergyDO;
import com.zmyc.infrastructure.repository.SystemConfigRepository;
import com.zmyc.infrastructure.repository.EnergyTransactionRepository;
import com.zmyc.infrastructure.repository.UserEnergyRepository;
import com.zmyc.infrastructure.repository.UserPerformanceRepository;
import com.zmyc.infrastructure.repository.UserRelationClosureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class UserRelationService {

    @Autowired
    private UserPerformanceRepository performanceRepository;

    @Autowired
    private UserRelationClosureRepository closureRepository;

    @Autowired
    private UserEnergyRepository energyRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private EnergyTransactionRepository energyTransactionRepository;

    /**
     * 用户入金时调用：
     *   1. 入金者自身：仅累加个人业绩
     *   2. 所有祖先：批量累加团队业绩（团队 = 纯下级，不含自身）
     *   3. 所有祖先：批量重算小区业绩缓存
     *   4. 入金者自身：增加能量值（能量值 = 入金金额 * 倍率）
     */
    @Transactional
    public void onDeposit(Long userId, BigDecimal amount, Long depositId) {
        performanceRepository.addPersonalVolume(userId, amount);
        performanceRepository.batchAddTeamVolume(userId, amount);
        performanceRepository.batchUpdateCommunityVolume(userId);

        // 增加能量值
        BigDecimal multiplier = systemConfigRepository.getDepositEnergyMultiplier();
        BigDecimal energyAmount = amount.multiply(multiplier);

        UserEnergyDO beforeEnergy = energyRepository.findByUserId(userId);
        BigDecimal balanceBefore = beforeEnergy != null ? beforeEnergy.getEnergyBalance() : BigDecimal.ZERO;

        energyRepository.addEnergy(userId, energyAmount);

        // 记录流水
        EnergyTransactionDO transaction = new EnergyTransactionDO();
        transaction.setUserId(userId);
        transaction.setTransactionType(EnergyTransactionDO.TransactionType.DEPOSIT_EARN);
        transaction.setAmount(energyAmount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceBefore.add(energyAmount));
        transaction.setRelatedId(depositId);
        transaction.setRemark("入金获得能量值: " + amount + " USDT * " + multiplier);
        energyTransactionRepository.save(transaction);
    }

    /**
     * 尝试消耗能量值（动态USDC奖励发放前调用）
     * 原子扣减：余额不足时不扣减，返回 false，调用方应跳过发奖
     *
     * @param userId    接收奖励的用户
     * @param amount    等值 USDC 的能量消耗量
     * @param relatedId 关联业务ID（如 depositId）
     * @param remark    流水备注
     * @return true = 扣减成功，可以发奖；false = 余额不足，跳过发奖
     */
    public boolean tryConsumeEnergy(Long userId, BigDecimal amount, Long relatedId, String remark) {
        UserEnergyDO before = energyRepository.findByUserId(userId);
        BigDecimal balanceBefore = (before != null && before.getEnergyBalance() != null)
                ? before.getEnergyBalance() : BigDecimal.ZERO;

        if (!energyRepository.deductEnergy(userId, amount)) {
            log.debug("能量值不足，跳过发奖: userId={}, required={}, balance={}", userId, amount, balanceBefore);
            return false;
        }

        EnergyTransactionDO tx = new EnergyTransactionDO();
        tx.setUserId(userId);
        tx.setTransactionType(EnergyTransactionDO.TransactionType.CONSUME);
        tx.setAmount(amount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceBefore.subtract(amount));
        tx.setRelatedId(relatedId);
        tx.setRemark(remark);
        energyTransactionRepository.save(tx);
        return true;
    }
}
