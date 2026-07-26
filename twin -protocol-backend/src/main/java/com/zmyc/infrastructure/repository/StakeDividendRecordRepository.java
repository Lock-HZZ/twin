package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zmyc.common.util.BatchUtils;
import com.zmyc.infrastructure.entity.StakeDividendRecordDO;
import com.zmyc.infrastructure.mapper.StakeDividendRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class StakeDividendRecordRepository {

    @Autowired
    private StakeDividendRecordMapper dividendMapper;

    /**
     * 查询指定日期已存在的质押ID集合（用于批量过滤，避免 N 次单条查询）
     */
    public Set<Long> findExistingStakeIdsByDate(Long dividendDate) {
        LambdaQueryWrapper<StakeDividendRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StakeDividendRecordDO::getDividendDate, dividendDate)
               .select(StakeDividendRecordDO::getStakeId);
        return dividendMapper.selectList(wrapper)
                .stream()
                .map(StakeDividendRecordDO::getStakeId)
                .collect(Collectors.toSet());
    }

    /**
     * 根据用户ID查询分红记录
     */
    public List<StakeDividendRecordDO> findByUserId(Long userId) {
        LambdaQueryWrapper<StakeDividendRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StakeDividendRecordDO::getUserId, userId)
               .orderByDesc(StakeDividendRecordDO::getCreatedDate);
        return dividendMapper.selectList(wrapper);
    }

    /**
     * 查询指定状态的分红记录
     */
    public List<StakeDividendRecordDO> findByStatus(int status) {
        LambdaQueryWrapper<StakeDividendRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StakeDividendRecordDO::getStatus, status)
               .orderByAsc(StakeDividendRecordDO::getId);
        return dividendMapper.selectList(wrapper);
    }

    /**
     * 保存分红记录
     */
    public void save(StakeDividendRecordDO dividend) {
        if (dividend.getId() == null) {
            dividendMapper.insert(dividend);
        } else {
            dividendMapper.updateById(dividend);
        }
    }

    /**
     * 真批量插入，每500条一条 INSERT SQL，整体在一个事务内。
     */
    @Transactional
    public void insertBatch(List<StakeDividendRecordDO> dividends) {
        BatchUtils.execute(dividends, dividendMapper::insertBatch);
    }

    /**
     * 按 batchId 批量更新为 SENT + txHash（一条 SQL）
     */
    public int markBatchSent(String batchId, String txHash, long now) {
        LambdaUpdateWrapper<StakeDividendRecordDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(StakeDividendRecordDO::getBatchId, batchId)
               .set(StakeDividendRecordDO::getStatus, StakeDividendRecordDO.Status.SENT)
               .set(StakeDividendRecordDO::getTxHash, txHash)
               .set(StakeDividendRecordDO::getUpdatedDate, now);
        return dividendMapper.update(null, wrapper);
    }

    /**
     * 按 batchId 批量更新为 PAID（一条 SQL）
     */
    public int markBatchPaid(String batchId, String txHash, long now) {
        LambdaUpdateWrapper<StakeDividendRecordDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(StakeDividendRecordDO::getBatchId, batchId)
                .eq(StakeDividendRecordDO::getStatus, StakeDividendRecordDO.Status.SENT)
               .set(StakeDividendRecordDO::getStatus, StakeDividendRecordDO.Status.PAID)
               .set(StakeDividendRecordDO::getPaidTime, now)
               .set(StakeDividendRecordDO::getUpdatedDate, now);
        if (txHash != null && !txHash.isEmpty()) {
            wrapper.set(StakeDividendRecordDO::getTxHash, txHash);
        }
        return dividendMapper.update(null, wrapper);
    }
}

