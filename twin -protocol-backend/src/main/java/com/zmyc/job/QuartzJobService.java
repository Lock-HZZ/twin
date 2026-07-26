package com.zmyc.job;

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuartzJobService {
    @Autowired
    private Scheduler scheduler;

    /**
     * 新增定时任务
     */
    public void addJob(String jobName, String jobGroup, String triggerName, String triggerGroup, String jobClass, String cron) throws Exception {
        Class<? extends Job> jobClazz = (Class<? extends Job>) Class.forName("com.zmyc.job." + jobClass);
        JobDetail jobDetail = JobBuilder.newJob(jobClazz)
                .withIdentity(jobName, jobGroup)
                .storeDurably()
                .build();

        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerName, triggerGroup)
                .startNow()
                .withSchedule(
                        CronScheduleBuilder
                                .cronSchedule(cron)
                                .withMisfireHandlingInstructionDoNothing()
                )
                .build();

        scheduler.scheduleJob(jobDetail, cronTrigger);
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }
    
    /**
     * 新增定时任务（带参数）
     */
    public void addJob(String jobName, String jobGroup, String triggerName, String triggerGroup, String jobClass, String cron, Map<String, Object> params) throws Exception {
        Class<? extends Job> jobClazz = (Class<? extends Job>) Class.forName("com.zmyc.job." + jobClass);
        JobDetail jobDetail = JobBuilder.newJob(jobClazz)
                .withIdentity(jobName, jobGroup)
                .storeDurably()
                .usingJobData(new JobDataMap(params))
                .build();

        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerName, triggerGroup)
                .startNow()
                .withSchedule(
                        CronScheduleBuilder
                                .cronSchedule(cron)
                                .withMisfireHandlingInstructionDoNothing()
                )
                .build();

        scheduler.scheduleJob(jobDetail, cronTrigger);
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }

    /**
     * 删除定时任务
     */
    public void deleteJob(String jobName, String jobGroup, String triggerName, String triggerGroup) throws Exception {
        scheduler.pauseTrigger(TriggerKey.triggerKey(triggerName, triggerGroup));
        scheduler.unscheduleJob(TriggerKey.triggerKey(triggerName, triggerGroup));
        scheduler.deleteJob(JobKey.jobKey(jobName, jobGroup));
    }

    /**
     * 重新设置定时任务
     */
    public void rescheduleJob(String triggerName, String triggerGroup, String cron) throws Exception {
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);
        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
        scheduler.rescheduleJob(triggerKey, cronTrigger);
    }

    /**
     * 暂停定时任务
     */
    public void pauseJob(String jobName, String jobGroup) throws Exception {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.pauseJob(jobKey);
    }

    /**
     * 恢复定时任务
     */
    public void resumeJob(String jobName, String jobGroup) throws Exception {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.resumeJob(jobKey);
    }

    /**
     * 立即执行一次定时任务
     */
    public void triggerJob(String jobName, String jobGroup) throws Exception {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.triggerJob(jobKey);
    }

    /**
     * 立即执行一次定时任务（带参数）
     */
    public void triggerJob(String jobName, String jobGroup, Map<String, Object> params) throws Exception {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        JobDataMap dataMap = new JobDataMap(params);
        scheduler.triggerJob(jobKey, dataMap);
    }

    /**
     * 获取所有定时任务
     */
    public List<Map<String, Object>> getAllJobs() throws Exception {
        List<Map<String, Object>> jobList = new ArrayList<>();
        
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                String jobName = jobKey.getName();
                String jobGroup = jobKey.getGroup();
                
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                for (Trigger trigger : triggers) {
                    Map<String, Object> jobInfo = new HashMap<>();
                    jobInfo.put("jobName", jobName);
                    jobInfo.put("jobGroup", jobGroup);
                    jobInfo.put("triggerName", trigger.getKey().getName());
                    jobInfo.put("triggerGroup", trigger.getKey().getGroup());
                    jobInfo.put("jobStatus", scheduler.getTriggerState(trigger.getKey()).name());
                    
                    if (trigger instanceof CronTrigger) {
                        CronTrigger cronTrigger = (CronTrigger) trigger;
                        jobInfo.put("cronExpression", cronTrigger.getCronExpression());
                        jobInfo.put("timeZone", cronTrigger.getTimeZone().getID());
                    }
                    
                    jobInfo.put("nextFireTime", trigger.getNextFireTime());
                    jobInfo.put("prevFireTime", trigger.getPreviousFireTime());
                    
                    jobList.add(jobInfo);
                }
            }
        }
        
        return jobList;
    }


    /**
     * 检查任务是否存在
     */
    public boolean checkJobExists(String jobName, String jobGroup) throws Exception {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        return scheduler.checkExists(jobKey);
    }

    /**
     * 检查触发器是否存在
     */
    public boolean checkTriggerExists(String triggerName, String triggerGroup) throws Exception {
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);
        return scheduler.checkExists(triggerKey);
    }

    /**
     * 获取触发器状态
     */
    public String getTriggerState(String triggerName, String triggerGroup) throws Exception {
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);
        Trigger.TriggerState triggerState = scheduler.getTriggerState(triggerKey);
        return triggerState.name();
    }

    /**
     * 获取任务的下一次执行时间
     */
    public Date getNextFireTime(String triggerName, String triggerGroup) throws Exception {
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);
        Trigger trigger = scheduler.getTrigger(triggerKey);
        if (trigger != null) {
            return trigger.getNextFireTime();
        }
        return null;
    }

    /**
     * 暂停所有任务
     */
    public void pauseAll() throws Exception {
        scheduler.pauseAll();
    }

    /**
     * 恢复所有任务
     */
    public void resumeAll() throws Exception {
        scheduler.resumeAll();
    }

    /**
     * 启动调度器
     */
    public void startScheduler() throws Exception {
        if (!scheduler.isStarted()) {
            scheduler.start();
        }
    }

    /**
     * 关闭调度器
     */
    public void shutdownScheduler() throws Exception {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
