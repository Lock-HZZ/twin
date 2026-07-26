package com.zmyc.config;

import com.zmyc.job.QuartzJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 定时任务初始化配置
 * 系统启动时自动注册必要的定时任务
 */
@Component
public class ScheduledJobConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobConfig.class);

    @Autowired
    private QuartzJobService quartzJobService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initDepositExpirationJob();
        initStakeDividendJob();
        initStakeDividendReconcileJob();
    }

    /**
     * 初始化入金订单过期清理任务
     * 每5分钟执行一次
     */
    private void initDepositExpirationJob() {
        try {
            String jobName = "depositExpirationJob";
            String jobGroup = "depositGroup";
            String triggerName = "depositExpirationTrigger";
            String triggerGroup = "depositGroup";
            String jobClass = "DepositExpirationJob";
            String cron = "0 */5 * * * ?"; // 每5分钟执行一次

            // 检查任务是否已存在
            if (!quartzJobService.checkJobExists(jobName, jobGroup)) {
                quartzJobService.addJob(jobName, jobGroup, triggerName, triggerGroup, jobClass, cron);
                log.info("入金订单过期清理任务注册成功: cron={}", cron);
            } else {
                log.info("入金订单过期清理任务已存在，跳过注册");
            }
        } catch (Exception e) {
            log.error("入金订单过期清理任务注册失败", e);
        }
    }

    /**
     * 初始化质押分红发放任务
     * 每天凌晨 1:00 执行：计算今日分红并发放
     */
    private void initStakeDividendJob() {
        try {
            String jobName = "stakeDividendJob";
            String jobGroup = "stakeGroup";
            String triggerName = "stakeDividendTrigger";
            String triggerGroup = "stakeGroup";
            String jobClass = "StakeScheduledTasks";
            String cron = "0 0 1 * * ?"; // 每天凌晨1点

            if (!quartzJobService.checkJobExists(jobName, jobGroup)) {
                quartzJobService.addJob(jobName, jobGroup, triggerName, triggerGroup, jobClass, cron);
                log.info("质押分红发放任务注册成功: cron={}", cron);
            } else {
                log.info("质押分红发放任务已存在，跳过注册");
            }
        } catch (Exception e) {
            log.error("质押分红发放任务注册失败", e);
        }
    }

    /**
     * 初始化质押分红补偿任务
     * 每5分钟执行一次：复查未确认（SENT/PENDING）的分红记录，兜底超时未确认的交易
     */
    private void initStakeDividendReconcileJob() {
        try {
            String jobName = "stakeDividendReconcileJob";
            String jobGroup = "stakeGroup";
            String triggerName = "stakeDividendReconcileTrigger";
            String triggerGroup = "stakeGroup";
            String jobClass = "StakeDividendReconcileJob";
            String cron = "0 */5 * * * ?"; // 每5分钟执行一次

            if (!quartzJobService.checkJobExists(jobName, jobGroup)) {
                quartzJobService.addJob(jobName, jobGroup, triggerName, triggerGroup, jobClass, cron);
                log.info("质押分红补偿任务注册成功: cron={}", cron);
            } else {
                log.info("质押分红补偿任务已存在，跳过注册");
            }
        } catch (Exception e) {
            log.error("质押分红补偿任务注册失败", e);
        }
    }
}
