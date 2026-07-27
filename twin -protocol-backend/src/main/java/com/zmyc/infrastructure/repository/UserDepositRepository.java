package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zmyc.infrastructure.entity.UserDepositDO;
import com.zmyc.infrastructure.mapper.UserDepositMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class UserDepositRepository {

    @Autowired
    private UserDepositMapper depositMapper;

    /** 根据交易哈希查询入金记录 */
    public UserDepositDO findByTxHash(String txHash) {
        return depositMapper.findByTxHash(txHash);
    }

    /** 查询用户所有入金记录 */
    public List<UserDepositDO> findByUserId(Long userId) {
        LambdaQueryWrapper<UserDepositDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDepositDO::getUserId, userId)
               .orderByDesc(UserDepositDO::getCreatedDate);
        return depositMapper.selectList(wrapper);
    }

    /** 查询用户进行中的订单数量（有效用户判断） */
    public int countActiveOrders(Long userId) {
        LambdaQueryWrapper<UserDepositDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDepositDO::getUserId, userId)
               .eq(UserDepositDO::getStatus, UserDepositDO.Status.COMPLETED);
        return depositMapper.selectCount(wrapper).intValue();
    }

    /** 查询全局当日已占用额度（PENDING + COMPLETED，用于每日总额度校验） */
    public BigDecimal getGlobalDailyOccupiedAmount(Long todayStart, Long todayEnd) {
        LambdaQueryWrapper<UserDepositDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(UserDepositDO::getStatus, UserDepositDO.Status.PENDING, UserDepositDO.Status.COMPLETED)
               .ge(UserDepositDO::getCreatedDate, todayStart)
               .lt(UserDepositDO::getCreatedDate, todayEnd);

        List<UserDepositDO> deposits = depositMapper.selectList(wrapper);
        return deposits.stream()
                .map(UserDepositDO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 查询用户已占用的入金额度（PENDING + COMPLETED） */
    public BigDecimal getOccupiedAmount(Long userId) {
        LambdaQueryWrapper<UserDepositDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDepositDO::getUserId, userId)
               .in(UserDepositDO::getStatus, UserDepositDO.Status.PENDING, UserDepositDO.Status.COMPLETED);

        List<UserDepositDO> deposits = depositMapper.selectList(wrapper);
        return deposits.stream()
                .map(UserDepositDO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 保存入金记录 */
    public void save(UserDepositDO deposit) {
        if (deposit.getId() == null) {
            depositMapper.insert(deposit);
        } else {
            depositMapper.updateById(deposit);
        }
    }

    /** 根据ID查询 */
    public UserDepositDO findById(Long id) {
        return depositMapper.selectById(id);
    }

    /**
     * 原子操作：将所有过期的 PENDING 订单置为 EXPIRED
     *
     * @param now 当前时间戳（秒）
     * @return 受影响的订单数量
     */
    public int expirePendingOrders(long now) {
        LambdaUpdateWrapper<UserDepositDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserDepositDO::getStatus, UserDepositDO.Status.PENDING)
               .lt(UserDepositDO::getExpiresAt, now)
               .set(UserDepositDO::getStatus, UserDepositDO.Status.EXPIRED);
        return depositMapper.update(null, wrapper);
    }

    public UserDepositDO findByNonce(Long nonce) {
        LambdaQueryWrapper<UserDepositDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDepositDO::getNonce, nonce)
                .eq(UserDepositDO::getStatus, UserDepositDO.Status.PENDING);
        return depositMapper.selectOne(wrapper);
    }
}
