package com.zmyc.job;

import com.zmyc.service.UserDepositService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 入金订单过期清理任务
 * 定时扫描并取消过期的 PENDING 订单，释放额度
 */
@Component
public class DepositExpirationJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(DepositExpirationJob.class);

    @Autowired
    private UserDepositService userDepositService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("开始执行入金订单过期清理任务");
            int canceledCount = userDepositService.cancelExpiredOrders();
            log.info("入金订单过期清理任务完成，已取消 {} 个订单", canceledCount);
        } catch (Exception e) {
            log.error("入金订单过期清理任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
