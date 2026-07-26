package com.zmyc.bamboo.core.engine.dispatcher;

import com.zmyc.bamboo.core.engine.dispatcher.subscriber.MessageSubscriber;
import com.zmyc.bamboo.core.model.EventLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SubmissionPublisher;

public class EventLogMemoryMessageDispatcher implements MessageDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogMemoryMessageDispatcher.class);

    private final SubmissionPublisher<EventLog> publisher = new SubmissionPublisher<>();

    @Override
    public boolean isSupport(Class<?> clazz) {
        return EventLog.class.isAssignableFrom(clazz);
    }

    @Override
    public Class<?> getMessageClass() {
        return EventLog.class;
    }

    @Override
    public void addSubscriber(MessageSubscriber subscriber) {
        if (subscriber == null) {
            throw new IllegalArgumentException("subscriber cannot be null");
        }
        if (subscriber.isSupport(EventLog.class)) {
            //noinspection unchecked
            publisher.subscribe((java.util.concurrent.Flow.Subscriber<EventLog>)
                    (java.util.concurrent.Flow.Subscriber<?>) subscriber.getInnerSubscriber());
        }
    }

    @Override
    public void publish(Object message) {
        if (message == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        // 使用 submit() 而非 offer()：
        // - offer() 超时后直接丢弃事件，有数据丢失风险
        // - submit() 会阻塞等待直到消费者缓冲区有空间，保证事件不丢
        // 代价是当消费者处理慢时会拖慢扫链节奏，属于预期的背压行为
        publisher.submit((EventLog) message);
    }
}
