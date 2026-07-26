package com.zmyc.bamboo.core.engine.filter;

import com.zmyc.bamboo.core.model.EventDefinition;
import com.zmyc.bamboo.core.model.EventLog;

import java.util.List;
import java.util.Map;

/**
 * 事件日志过滤器接口。
 *
 * <p>从原始链上日志中，筛选出与配置的合约地址和事件签名匹配的日志。
 * 默认实现见 {@link DefaultEventLogFilter}。
 */
public interface EventLogFilter {

    /**
     * 过滤日志。
     *
     * @param contractEventDefinitionsMap 合约地址 → 事件定义列表的映射（来自配置）
     * @param logs                        从链上拉取的原始日志列表
     * @return 符合配置的事件日志列表
     */
    List<EventLog> filter(Map<String, List<EventDefinition>> contractEventDefinitionsMap,
                          List<EventLog> logs);
}
