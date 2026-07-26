package com.zmyc.job;

import com.zmyc.service.UserStakeService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 质押分红发放任务（每天凌晨1点执行）
 */
@Component
@Slf4j
public class StakeScheduledTasks implements Job {


    @Autowired
    private UserStakeService userStakeService;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行质押分红定时任务");
        try {
            userStakeService.generateAndDistributeDailyDividends();
            log.info("质押分红定时任务执行成功");
        } catch (Exception e) {
            log.error("质押分红定时任务执行失败", e);
        }
    }
}
