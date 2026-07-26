package com.zmyc.job;

import com.zmyc.service.UserStakeService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 质押分红补偿任务（高频，每几分钟）。
 * 职责：只复查已有 txHash 的 SENT 记录，按链上结果补最终状态。
 *   - 链上批次已处理 → 标 PAID
 *   - 交易仍 pending → 保持 SENT 继续等
 *   - 交易失效且链上未处理 → 退回 PENDING，交主任务重发
 * 注意：补偿任务绝不发送新交易，也不碰无 txHash 的 PENDING 记录，
 *       发送是主任务(StakeScheduledTasks)的专属职责，避免并发重发。
 */
@Component
@Slf4j
public class StakeDividendReconcileJob implements Job {

    @Autowired
    private UserStakeService userStakeService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            userStakeService.reconcileSentDividends();
        } catch (Exception e) {
            log.error("质押分红补偿任务执行失败", e);
        }
    }
}
