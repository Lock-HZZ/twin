package com.zmyc.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmyc.infrastructure.entity.FeeDistributionRecordDO;
import com.zmyc.infrastructure.mapper.FeeDistributionRecordMapper;
import com.zmyc.service.FeeDividendContractService;
import com.zmyc.service.FeeDividendService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 手续费分配确认补偿任务
 * 轮询所有 SENT/PENDING 状态的分配记录：
 *   链上批次已确认 → 标记 CONFIRMED
 *   交易仍在内存池 → 继续等待
 *   交易失效且链上未处理 → 重发（batchId 幂等，安全重发）
 */
@Component
@Slf4j
public class FeeDistributionConfirmJob implements Job {

    @Autowired
    private FeeDistributionRecordMapper mapper;

    @Autowired
    private FeeDividendContractService feeDividendContractService;

    @Autowired
    private FeeDividendService feeDividendService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        reconcile();
    }

    public void reconcile() {
        // 处理 SENT 状态（已发送，等待确认或重发）
        List<FeeDistributionRecordDO> sentRecords = mapper.selectList(
                new LambdaQueryWrapper<FeeDistributionRecordDO>()
                        .eq(FeeDistributionRecordDO::getStatus, FeeDistributionRecordDO.Status.SENT));

        if (!sentRecords.isEmpty()) {
            log.info("手续费分配确认任务：发现 {} 条SENT记录", sentRecords.size());
            long now = System.currentTimeMillis() / 1000;

            for (FeeDistributionRecordDO record : sentRecords) {
                try {
                    String batchId = record.getBatchId();
                    String txHash = record.getDistributeTxHash();

                    // 链上批次已确认 → 标记 CONFIRMED
                    if (feeDividendContractService.isBatchProcessed(batchId)) {
                        record.setStatus(FeeDistributionRecordDO.Status.CONFIRMED);
                        record.setUpdatedDate(now);
                        mapper.updateById(record);
                        log.info("手续费分配已确认: id={}, batchId={}", record.getId(), batchId);
                        continue;
                    }

                    // 交易仍在内存池 → 继续等待
                    if (feeDividendContractService.isTransactionAlive(txHash)) {
                        log.debug("手续费分配交易仍在池中，保持SENT: batchId={}, txHash={}", batchId, txHash);
                        continue;
                    }

                    // 交易失效且链上未处理 → 重发（batchId 幂等，安全重发）
                    log.warn("手续费分配交易失效，重新发送: batchId={}, oldTxHash={}", batchId, txHash);
                    feeDividendService.resendBatch(record);

                } catch (Exception e) {
                    log.error("处理SENT手续费分配记录失败: id={}, error={}", record.getId(), e.getMessage());
                }
            }
        }

        // 处理 PENDING 状态（首次发送失败，未落 txHash）
        List<FeeDistributionRecordDO> pendingRecords = mapper.selectList(
                new LambdaQueryWrapper<FeeDistributionRecordDO>()
                        .eq(FeeDistributionRecordDO::getStatus, FeeDistributionRecordDO.Status.PENDING));

        if (!pendingRecords.isEmpty()) {
            log.info("手续费分配确认任务：发现 {} 条PENDING记录，尝试重发", pendingRecords.size());
            for (FeeDistributionRecordDO record : pendingRecords) {
                try {
                    // batchId 幂等，即使链上已处理也不会重复入账
                    if (feeDividendContractService.isBatchProcessed(record.getBatchId())) {
                        record.setStatus(FeeDistributionRecordDO.Status.CONFIRMED);
                        record.setUpdatedDate(System.currentTimeMillis() / 1000);
                        mapper.updateById(record);
                        log.info("PENDING记录链上已处理，标记CONFIRMED: id={}", record.getId());
                        continue;
                    }
                    feeDividendService.resendBatch(record);
                } catch (Exception e) {
                    log.error("重发PENDING手续费分配记录失败: id={}, error={}", record.getId(), e.getMessage());
                }
            }
        }
    }
}
