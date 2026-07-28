package com.zmyc.job;

import com.zmyc.service.TradeContractService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 移除LP回执确认任务
 * 轮询 REMOVING 状态的订单，根据链上回执决定最终状态：
 * - 回执成功 → REMOVED
 * - 回执失败或超时 → 重置为 COMPLETED，允许用户重试
 */
@Component
public class RemoveLiquidityConfirmJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(RemoveLiquidityConfirmJob.class);

    @Autowired
    private TradeContractService tradeContractService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("开始执行移除LP回执确认任务");
            tradeContractService.pollRemovingOrders();
            log.info("移除LP回执确认任务完成");
        } catch (Exception e) {
            log.error("移除LP回执确认任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
