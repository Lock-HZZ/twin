package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmyc.infrastructure.entity.RewardRecordDO;
import com.zmyc.infrastructure.mapper.RewardRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RewardRecordRepository {

    @Autowired
    private RewardRecordMapper mapper;

    /** 批量插入奖励记录 */
    public void saveBatch(List<RewardRecordDO> records) {
        if (records == null || records.isEmpty()) return;
        mapper.insertBatch(records);
    }

    /** 根据业务ID和奖励类型查询 */
    public List<RewardRecordDO> findByBusinessIdAndRewardType(Long businessId, byte rewardType) {
        LambdaQueryWrapper<RewardRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardRecordDO::getBusinessId, businessId)
               .eq(RewardRecordDO::getRewardType, rewardType);
        return mapper.selectList(wrapper);
    }

    /** 查询指定多个奖励类型的所有 SENT 记录（供补偿任务轮询） */
    public List<RewardRecordDO> findSentByRewardTypes(byte... rewardTypes) {
        List<Byte> types = new java.util.ArrayList<>();
        for (byte t : rewardTypes) types.add(t);
        LambdaQueryWrapper<RewardRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RewardRecordDO::getRewardType, types)
               .eq(RewardRecordDO::getStatus, RewardRecordDO.Status.SENT);
        return mapper.selectList(wrapper);
    }

    /** 按ID列表设置batchId和SENT状态 */
    public void updateBatchIdAndStatus(String batchId, List<Long> ids, int status, long sentAt) {
        long now = System.currentTimeMillis() / 1000;
        mapper.updateBatchIdAndStatus(batchId, status, sentAt, ids, now);
    }

    /** 拿到txHash后立即标记SENT（保证hash落库） */
    public void markBatchSent(String batchId, String txHash, long sentAt) {
        mapper.updateStatusByBatchId(batchId, RewardRecordDO.Status.SENT.getValue(),
                txHash, null, sentAt);
    }

    /** 链上确认成功后标记PAID */
    public void markBatchPaid(String batchId, String txHash, long paidAt) {
        mapper.updateStatusByBatchId(batchId, RewardRecordDO.Status.PAID.getValue(),
                txHash, paidAt, paidAt);
    }

    /** 查询指定业务ID下待重试的记录 */
    public List<RewardRecordDO> findPendingByBusinessId(Long businessId, byte rewardType) {
        LambdaQueryWrapper<RewardRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardRecordDO::getBusinessId, businessId)
               .eq(RewardRecordDO::getRewardType, rewardType)
               .in(RewardRecordDO::getStatus,
                   RewardRecordDO.Status.PENDING,
                   RewardRecordDO.Status.FAILED);
        return mapper.selectList(wrapper);
    }

    /** 幂等检查：该业务ID下是否已有任意分红记录 */
    public boolean existsByBusinessId(Long businessId) {
        LambdaQueryWrapper<RewardRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardRecordDO::getBusinessId, businessId).last("LIMIT 1");
        return mapper.selectCount(wrapper) > 0;
    }
}
