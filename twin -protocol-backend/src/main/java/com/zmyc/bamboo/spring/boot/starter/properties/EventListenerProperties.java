package com.zmyc.bamboo.spring.boot.starter.properties;

import com.zmyc.bamboo.core.model.EventDefinition;

import java.util.List;
import java.util.Map;

/**
 * 事件监听配置属性，对应 {@code bamboo.event-listener.*}。
 *
 * <p>{@code contractEventDefinitions} 是一个 Map，Key 为合约地址，Value 为该合约监听的事件定义列表。
 * 合约地址大小写不敏感（过滤器内部使用 equalsIgnoreCase 比较），
 * 但配置中不应出现仅大小写不同的重复地址，否则同一事件会被重复处理。
 */
public class EventListenerProperties {

    /** 合约地址 → 事件定义列表的映射。 */
    private Map<String, List<EventDefinition>> contractEventDefinitions;

    public EventListenerProperties() {}

    public EventListenerProperties(Map<String, List<EventDefinition>> contractEventDefinitions) {
        this.contractEventDefinitions = contractEventDefinitions;
    }

    public Map<String, List<EventDefinition>> getContractEventDefinitions() {
        return contractEventDefinitions;
    }

    public void setContractEventDefinitions(Map<String, List<EventDefinition>> contractEventDefinitions) {
        this.contractEventDefinitions = contractEventDefinitions;
    }

    @Override
    public String toString() {
        return "EventListenerProperties{contractEventDefinitions=" + contractEventDefinitions + "}";
    }
}
