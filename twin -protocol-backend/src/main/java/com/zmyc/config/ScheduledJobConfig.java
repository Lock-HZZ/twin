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
        initRemoveLiquidityConfirmJob();
        initDailyBurnJob();
        initBurnConfirmJob();
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

    /**
     * 初始化移除LP回执确认任务
     * 每30秒执行一次：轮询 REMOVING 订单回执，确认或重置状态
     */
    private void initRemoveLiquidityConfirmJob() {
        try {
            String jobName = "removeLiquidityConfirmJob";
            String jobGroup = "depositGroup";
            String triggerName = "removeLiquidityConfirmTrigger";
            String triggerGroup = "depositGroup";
            String jobClass = "RemoveLiquidityConfirmJob";
            String cron = "0/30 * * * * ?"; // 每30秒执行一次

            if (!quartzJobService.checkJobExists(jobName, jobGroup)) {
                quartzJobService.addJob(jobName, jobGroup, triggerName, triggerGroup, jobClass, cron);
                log.info("移除LP回执确认任务注册成功: cron={}", cron);
            } else {
                log.info("移除LP回执确认任务已存在，跳过注册");
            }
        } catch (Exception e) {
            log.error("移除LP回执确认任务注册失败", e);
        }
    }

    /**
     * 初始化每日TIP燃烧任务
     * 每天23:00执行：计算燃烧比例并调用合约燃烧TIP
     */
    private void initDailyBurnJob() {
        try {
            String jobName = "dailyBurnJob";
            String jobGroup = "burnGroup";
            String triggerName = "dailyBurnTrigger";
            String triggerGroup = "burnGroup";
            String jobClass = "DailyBurnJob";
            String cron = "0 0 23 * * ?"; // 每天23:00执行

            if (!quartzJobService.checkJobExists(jobName, jobGroup)) {
                quartzJobService.addJob(jobName, jobGroup, triggerName, triggerGroup, jobClass, cron);
                log.info("每日TIP燃烧任务注册成功: cron={}", cron);
            } else {
                log.info("每日TIP燃烧任务已存在，跳过注册");
            }
        } catch (Exception e) {
            log.error("每日TIP燃烧任务注册失败", e);
        }
    }

    /**
     * 初始化燃烧确认补偿任务
     * 每5分钟执行一次：监控和补偿未完成的燃烧记录
     */
    private void initBurnConfirmJob() {
        try {
            String jobName = "burnConfirmJob";
            String jobGroup = "burnGroup";
            String triggerName = "burnConfirmTrigger";
            String triggerGroup = "burnGroup";
            String jobClass = "BurnConfirmJob";
            String cron = "0 */5 * * * ?"; // 每5分钟执行一次

            if (!quartzJobService.checkJobExists(jobName, jobGroup)) {
                quartzJobService.addJob(jobName, jobGroup, triggerName, triggerGroup, jobClass, cron);
                log.info("TIP燃烧补偿任务注册成功: cron={}", cron);
            } else {
                log.info("TIP燃烧补偿任务已存在，跳过注册");
            }
        } catch (Exception e) {
            log.error("TIP燃烧补偿任务注册失败", e);
        }
    }
}
