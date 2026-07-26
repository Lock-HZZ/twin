package com.zmyc.bamboo.core.engine.filter;

import com.zmyc.bamboo.core.model.EventDefinition;
import com.zmyc.bamboo.core.model.EventLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 事件日志过滤器默认实现。
 *
 * <p>过滤逻辑：
 * <ol>
 *   <li>按合约地址匹配（忽略大小写）</li>
 *   <li>按事件签名哈希匹配（比对 topics[0]，忽略大小写）</li>
 * </ol>
 *
 * <p><b>注意：</b>配置的合约地址不应存在大小写重复项，否则同一条日志会被多次添加到结果中。
 * 过滤器本身不做去重，去重依赖 {@code IndexerTask} 中的 DB 存在性检查和 {@code INSERT IGNORE}。
 */
public class DefaultEventLogFilter implements EventLogFilter {

    @Override
    public List<EventLog> filter(Map<String, List<EventDefinition>> contractEventDefinitionsMap,
                                 List<EventLog> logs) {
        List<EventLog> result = new ArrayList<>();

        for (Map.Entry<String, List<EventDefinition>> entry : contractEventDefinitionsMap.entrySet()) {
            String contractAddress = entry.getKey();
            List<EventDefinition> eventDefinitions = entry.getValue();

            // 第一步：按合约地址过滤（equalsIgnoreCase，兼容链上地址大小写不统一的情况）
            List<EventLog> addressMatchedLogs = logs.stream()
                    .filter(log -> log.getAddress().equalsIgnoreCase(contractAddress))
                    .toList();

            for (EventDefinition definition : eventDefinitions) {
                String signatureHash = definition.getSignatureHash();
                // 第二步：按事件签名哈希过滤（topics[0] 是事件签名哈希）
                List<EventLog> matched = addressMatchedLogs.stream()
                        .filter(log -> {
                            String[] topics = log.getTopics().split(",");
                            if (topics.length == 0) return false;
                            return signatureHash.equalsIgnoreCase(topics[0]);
                        })
                        .toList();
                result.addAll(matched);
            }
        }

        return result;
    }
}
