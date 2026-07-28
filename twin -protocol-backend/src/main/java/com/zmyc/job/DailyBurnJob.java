package com.zmyc.job;

import com.zmyc.service.TipBurnService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 每日TIP燃烧任务
 * 每天23:00执行，计算燃烧比例并调用合约燃烧TIP
 */
@Component
@Slf4j
public class DailyBurnJob implements Job {

    @Autowired
    private TipBurnService tipBurnService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("开始执行每日TIP燃烧任务");
            tipBurnService.executeDailyBurn();
            log.info("每日TIP燃烧任务执行完成");
        } catch (Exception e) {
            log.error("每日TIP燃烧任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
