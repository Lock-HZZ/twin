package com.zmyc.job;

import com.zmyc.service.RewardService;
import com.zmyc.service.TipBurnService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TIP燃烧确认任务
 * 每5分钟执行一次，监控和补偿未完成的燃烧记录
 */
@Component
@Slf4j
public class RewardConfirmJob implements Job {

    @Autowired
    private RewardService rewardService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            rewardService.reconcileDividends();
        } catch (Exception e) {
            log.error("奖励上链任务失败", e);
            throw new JobExecutionException(e);
        }
    }
}
