package com.zmyc.bamboo.spring.boot.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Bamboo 框架配置属性，对应 {@code application.yml} 中 {@code bamboo.*} 前缀的配置项。
 *
 * <p>示例配置：
 * <pre>
 * bamboo:
 *   auto-initialize-schema: true       # 启动时自动建表
 *   auto-initialize-check-point: true  # 首次启动自动写入当前最新块作为起点
 *   auto-start-engine: true            # 自动启动扫链引擎
 *   schedule-initial-seconds: 10       # 启动后延迟多少秒开始扫描
 *   schedule-delay-seconds: 5          # 每轮扫描完成后等待多少秒
 *   blockchain:
 *     id: 56
 *     rpc-node-url: https://...
 *   event-listener:
 *     contract-event-definitions:
 *       "0xABC...":
 *         - signature: "Transfer(address,address,uint256)"
 *           signature-hash: "0xddf2..."
 *           description: "转账"
 * </pre>
 */
@ConfigurationProperties(prefix = "bamboo")
public class BambooProperties {

    /** 是否启用 bamboo 框架；设为 false 时所有功能均不生效。默认 true。 */
    private Boolean isOpen = true;

    /** 是否在启动时自动建表（bamboo_check_point / bamboo_event_log）。默认 true。 */
    private Boolean autoInitializeSchema = true;

    /** 是否在首次启动时自动查询最新块高并写入检查点，已有记录时不覆盖。默认 false。 */
    private Boolean autoInitializeCheckPoint = false;

    /** 是否自动启动扫链引擎；设为 false 可手动调用 IndexerEngine.start()。默认 true。 */
    private Boolean autoStartEngine = true;

    /** 引擎启动后延迟多少秒开始第一次扫描。默认 5 秒。 */
    private Integer scheduleInitialSeconds = 5;

    /** 每轮扫描完成后等待多少秒再开始下一轮（scheduleWithFixedDelay 语义）。默认 3 秒。 */
    private Integer scheduleDelaySeconds = 3;

    /** 超过多少秒仍 unprocessed 的事件将被重新投递。默认 60 秒。 */
    private Integer unprocessedRetrySeconds = 60;

    private Map<String, BlockchainProperties> blockchains;
    private EventListenerProperties eventListener;

    public BambooProperties() {}

    public BambooProperties(Boolean autoInitializeSchema, Boolean autoInitializeCheckPoint,
                            Boolean autoStartEngine, Integer scheduleInitialSeconds,
                            Integer scheduleDelaySeconds, Map<String, BlockchainProperties> blockchains,
                            EventListenerProperties eventListener) {
        this.autoInitializeSchema = autoInitializeSchema;
        this.autoInitializeCheckPoint = autoInitializeCheckPoint;
        this.autoStartEngine = autoStartEngine;
        this.scheduleInitialSeconds = scheduleInitialSeconds;
        this.scheduleDelaySeconds = scheduleDelaySeconds;
        this.blockchains = blockchains;
        this.eventListener = eventListener;
    }

    public Boolean getIsOpen() { return isOpen; }
    public void setIsOpen(Boolean isOpen) { this.isOpen = isOpen; }

    public Boolean getAutoInitializeSchema() { return autoInitializeSchema; }
    public void setAutoInitializeSchema(Boolean autoInitializeSchema) { this.autoInitializeSchema = autoInitializeSchema; }

    public Boolean getAutoInitializeCheckPoint() { return autoInitializeCheckPoint; }
    public void setAutoInitializeCheckPoint(Boolean autoInitializeCheckPoint) { this.autoInitializeCheckPoint = autoInitializeCheckPoint; }

    public Boolean getAutoStartEngine() { return autoStartEngine; }
    public void setAutoStartEngine(Boolean autoStartEngine) { this.autoStartEngine = autoStartEngine; }

    public Integer getScheduleInitialSeconds() { return scheduleInitialSeconds; }
    public void setScheduleInitialSeconds(Integer scheduleInitialSeconds) { this.scheduleInitialSeconds = scheduleInitialSeconds; }

    public Integer getScheduleDelaySeconds() { return scheduleDelaySeconds; }
    public void setScheduleDelaySeconds(Integer scheduleDelaySeconds) { this.scheduleDelaySeconds = scheduleDelaySeconds; }

    public Map<String, BlockchainProperties> getBlockchains() { return blockchains; }
    public void setBlockchains(Map<String, BlockchainProperties> blockchains) { this.blockchains = blockchains; }

    public EventListenerProperties getEventListener() { return eventListener; }
    public void setEventListener(EventListenerProperties eventListener) { this.eventListener = eventListener; }

    public Integer getUnprocessedRetrySeconds() { return unprocessedRetrySeconds; }
    public void setUnprocessedRetrySeconds(Integer unprocessedRetrySeconds) { this.unprocessedRetrySeconds = unprocessedRetrySeconds; }

    @Override
    public String toString() {
        return "BambooProperties{autoInitializeSchema=" + autoInitializeSchema +
               ", autoStartEngine=" + autoStartEngine +
               ", scheduleInitialSeconds=" + scheduleInitialSeconds +
               ", scheduleDelaySeconds=" + scheduleDelaySeconds + "}";
    }
}
