package com.zmyc.service;

import com.zmyc.infrastructure.entity.EnergyTransactionDO;
import com.zmyc.infrastructure.entity.UserEnergyDO;
import com.zmyc.infrastructure.repository.SystemConfigRepository;
import com.zmyc.infrastructure.repository.EnergyTransactionRepository;
import com.zmyc.infrastructure.repository.UserEnergyRepository;
import com.zmyc.infrastructure.repository.UserPerformanceRepository;
import com.zmyc.infrastructure.repository.UserRelationClosureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
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
}
