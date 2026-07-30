package com.zmyc.job;

import com.zmyc.service.LpRewardReleaseService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * LP挖矿奖励释放任务
 * 每天凌晨2点执行，将当日应释放的LP奖励写入reward_record并发送上链
 */
@Component
@Slf4j
public class LpRewardReleaseJob implements Job {

    @Autowired
    private LpRewardReleaseService lpRewardReleaseService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("开始执行LP奖励释放任务");
            lpRewardReleaseService.releaseTodayRewards();
            log.info("LP奖励释放任务完成");
        } catch (Exception e) {
            log.error("LP奖励释放任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
