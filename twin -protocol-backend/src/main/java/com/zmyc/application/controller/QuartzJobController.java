package com.zmyc.application.controller;

import com.zmyc.common.annotation.ApiKeyAuth;
import com.zmyc.job.QuartzJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Quartz定时任务管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/quartz")
@RequiredArgsConstructor
public class QuartzJobController {

    private final QuartzJobService quartzJobService;

    @ApiKeyAuth
    @PostMapping("/job")
    public Map<String, Object> addJob(@RequestBody Map<String, Object> params) {
        try {
            String jobName = (String) params.get("jobName");
            String jobGroup = (String) params.get("jobGroup");
            String triggerName = (String) params.get("triggerName");
            String triggerGroup = (String) params.get("triggerGroup");
            String jobClass = (String) params.get("jobClass");
            String cron = (String) params.get("cron");

            if (jobName == null || jobGroup == null || triggerName == null ||
                    triggerGroup == null || jobClass == null || cron == null) {
                return Map.of(
                        "code", 400,
                        "message", "参数不完整，必需参数: jobName, jobGroup, triggerName, triggerGroup, jobClass, cron"
                );
            }

            // 检查任务是否已存在
            if (quartzJobService.checkJobExists(jobName, jobGroup)) {
                return Map.of(
                        "code", 400,
                        "message", "任务已存在: " + jobName + "." + jobGroup
                );
            }

            Map<String, Object> jobParams = (Map<String, Object>) params.get("params");

            if (jobParams != null && !jobParams.isEmpty()) {
                quartzJobService.addJob(jobName, jobGroup, triggerName, triggerGroup, jobClass, cron, jobParams);
            } else {
                quartzJobService.addJob(jobName, jobGroup, triggerName, triggerGroup, jobClass, cron);
            }

            return Map.of(
                    "code", 200,
                    "message", "任务添加成功",
                    "data", Map.of(
                            "jobName", jobName,
                            "jobGroup", jobGroup
                    )
            );
        } catch (Exception e) {
            log.error("添加任务失败", e);
            return Map.of(
                    "code", 500,
                    "message", "添加任务失败: " + e.getMessage()
            );
        }
    }

    @ApiKeyAuth
    @DeleteMapping("/job")
    public Map<String, Object> deleteJob(
            @RequestParam String jobName,
            @RequestParam String jobGroup,
            @RequestParam String triggerName,
            @RequestParam String triggerGroup) {
        try {
            if (!quartzJobService.checkJobExists(jobName, jobGroup)) {
                return Map.of(
                        "code", 404,
                        "message", "任务不存在: " + jobName + "." + jobGroup
                );
            }

            quartzJobService.deleteJob(jobName, jobGroup, triggerName, triggerGroup);
            return Map.of(
                    "code", 200,
                    "message", "任务删除成功"
            );
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return Map.of(
                    "code", 500,
                    "message", "删除任务失败: " + e.getMessage()
            );
        }
    }

    @ApiKeyAuth
    @PutMapping("/job/cron")
    public Map<String, Object> rescheduleJob(
            @RequestParam String triggerName,
            @RequestParam String triggerGroup,
            @RequestParam String cron) {
        try {
            if (!quartzJobService.checkTriggerExists(triggerName, triggerGroup)) {
                return Map.of(
                        "code", 404,
                        "message", "触发器不存在: " + triggerName + "." + triggerGroup
                );
            }

            quartzJobService.rescheduleJob(triggerName, triggerGroup, cron);
            return Map.of(
                    "code", 200,
                    "message", "任务Cron表达式修改成功",
                    "data", Map.of(
                            "triggerName", triggerName,
                            "triggerGroup", triggerGroup,
                            "newCron", cron
                    )
            );
        } catch (Exception e) {
            log.error("修改任务Cron表达式失败", e);
            return Map.of(
                    "code", 500,
                    "message", "修改Cron表达式失败: " + e.getMessage()
            );
        }
    }

    @ApiKeyAuth
    @PostMapping("/job/trigger")
    public Map<String, Object> triggerJob(
            @RequestParam String jobName,
            @RequestParam String jobGroup,
            @RequestBody(required = false) Map<String, Object> params) {
        try {
            if (!quartzJobService.checkJobExists(jobName, jobGroup)) {
                return Map.of(
                        "code", 404,
                        "message", "任务不存在: " + jobName + "." + jobGroup
                );
            }

            if (params != null && !params.isEmpty()) {
                quartzJobService.triggerJob(jobName, jobGroup, params);
            } else {
                quartzJobService.triggerJob(jobName, jobGroup);
            }

            return Map.of(
                    "code", 200,
                    "message", "任务已触发执行"
            );
        } catch (Exception e) {
            log.error("触发任务失败", e);
            return Map.of(
                    "code", 500,
                    "message", "触发任务失败: " + e.getMessage()
            );
        }
    }

}

