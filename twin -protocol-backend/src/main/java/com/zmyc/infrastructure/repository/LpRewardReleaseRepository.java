package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmyc.infrastructure.entity.LpRewardReleaseDO;
import com.zmyc.infrastructure.mapper.LpRewardReleaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LpRewardReleaseRepository {

    @Autowired
    private LpRewardReleaseMapper mapper;

    /** 批量插入释放计划 */
    public void insertBatch(List<LpRewardReleaseDO> releases) {
        if (releases == null || releases.isEmpty()) return;
        releases.forEach(mapper::insert);
    }

    /** 幂等检查：该移除LP交易是否已生成释放计划 */
    public boolean existsByRemoveTxHash(String removeTxHash) {
        LambdaQueryWrapper<LpRewardReleaseDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LpRewardReleaseDO::getRemoveTxHash, removeTxHash).last("LIMIT 1");
        return mapper.selectCount(wrapper) > 0;
    }

    /** 查询指定日期待释放的记录 */
    public List<LpRewardReleaseDO> findPendingByReleaseDate(Integer releaseDate) {
        LambdaQueryWrapper<LpRewardReleaseDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LpRewardReleaseDO::getReleaseDate, releaseDate)
               .eq(LpRewardReleaseDO::getStatus, LpRewardReleaseDO.Status.PENDING);
        return mapper.selectList(wrapper);
    }

    /** 批量更新状态 */
    public void updateStatus(List<Long> ids, Integer status, String batchId) {
        if (ids == null || ids.isEmpty()) return;
        long now = System.currentTimeMillis() / 1000;
        mapper.updateStatusByIds(ids, status, batchId, now);
    }

    /** 查询用户的LP释放记录 */
    public List<LpRewardReleaseDO> findByUserId(Long userId) {
        LambdaQueryWrapper<LpRewardReleaseDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LpRewardReleaseDO::getUserId, userId)
               .orderByDesc(LpRewardReleaseDO::getReleaseDate);
        return mapper.selectList(wrapper);
    }

    /**
     * 查询某入金订单的锁仓TIP总额（60条释放记录的 totalAmount 相同，取任意一条即可）。
     * 无释放计划时返回 null。
     */
    public java.math.BigDecimal findTotalAmountByDepositId(Long depositId) {
        LambdaQueryWrapper<LpRewardReleaseDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LpRewardReleaseDO::getDepositId, depositId).last("LIMIT 1");
        LpRewardReleaseDO one = mapper.selectOne(wrapper);
        return one != null ? one.getTotalAmount() : null;
    }
}
