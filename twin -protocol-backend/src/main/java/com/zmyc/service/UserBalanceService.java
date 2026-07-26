package com.zmyc.service;

import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.infrastructure.entity.BalanceTransactionDO;
import com.zmyc.infrastructure.entity.UserBalanceDO;
import com.zmyc.infrastructure.mapper.UserBalanceMapper;
import com.zmyc.infrastructure.repository.BalanceTransactionRepository;
import com.zmyc.infrastructure.repository.UserBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserBalanceService {

    private static final Logger log = LoggerFactory.getLogger(UserBalanceService.class);

    private static final int MAX_RETRY = 3;

    @Autowired
    private UserBalanceRepository balanceRepository;

    @Autowired
    private UserBalanceMapper balanceMapper;

    @Autowired
    private BalanceTransactionRepository txRepository;

    /**
     * 增加余额（入金初始化场景，INSERT ON DUPLICATE KEY UPDATE）
     */
    @Transactional
    public void addBalance(Long userId, Long tokenId, BigDecimal amount,
                           byte txType, Long relatedId, String remark) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        UserBalanceDO before = balanceRepository.findByUserToken(userId, tokenId);
        BigDecimal balanceBefore = before != null ? before.getBalance() : BigDecimal.ZERO;

        balanceRepository.addBalanceOrCreate(userId, tokenId, amount);

        saveTransaction(userId, tokenId, txType, amount,
                balanceBefore, balanceBefore.add(amount), relatedId, remark);

        log.info("余额增加成功: userId={}, tokenId={}, amount={}, before={}, after={}",
                userId, tokenId, amount, balanceBefore, balanceBefore.add(amount));
    }

    /**
     * 扣减余额（提现等场景，行锁 + 条件 WHERE 防超扣，失败重试）
     */
    @Transactional
    public void deductBalance(Long userId, Long tokenId, BigDecimal amount,
                              byte txType, Long relatedId, String remark) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            UserBalanceDO balance = balanceRepository.findForUpdate(userId, tokenId);

            if (balance == null || balance.getBalance().compareTo(amount) < 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
            }

            BigDecimal balanceBefore = balance.getBalance();
            long now = System.currentTimeMillis() / 1000;
            int rows = balanceMapper.deductBalance(userId, tokenId, amount, now);

            if (rows == 1) {
                saveTransaction(userId, tokenId, txType, amount.negate(),
                        balanceBefore, balanceBefore.subtract(amount), relatedId, remark);

                log.info("余额扣减成功: userId={}, tokenId={}, amount={}, before={}, after={}, attempt={}",
                        userId, tokenId, amount, balanceBefore, balanceBefore.subtract(amount), attempt);
                return;
            }

            log.warn("余额扣减未命中，重试 {}/{}: userId={}, tokenId={}", attempt, MAX_RETRY, userId, tokenId);
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }

    private void saveTransaction(Long userId, Long tokenId, byte txType,
                                 BigDecimal amount, BigDecimal balanceBefore,
                                 BigDecimal balanceAfter, Long relatedId, String remark) {
        BalanceTransactionDO tx = new BalanceTransactionDO();
        tx.setUserId(userId);
        tx.setTokenId(tokenId);
        tx.setTxType(txType);
        tx.setAmount(amount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setRelatedId(relatedId);
        tx.setRemark(remark);
        txRepository.save(tx);
    }
}
