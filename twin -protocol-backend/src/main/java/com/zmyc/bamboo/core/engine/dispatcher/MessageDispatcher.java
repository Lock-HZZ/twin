package com.zmyc.bamboo.core.engine.dispatcher;

import com.zmyc.bamboo.core.engine.dispatcher.subscriber.MessageSubscriber;

/**
 * 消息分发器接口。
 *
 * <p>负责将事件发布给所有已注册的订阅者。
 * 默认实现见 {@link EventLogMemoryMessageDispatcher}（基于 JDK Flow API 的内存分发）。
 */
public interface MessageDispatcher {

    /** 判断此分发器是否支持处理指定类型的消息。 */
    boolean isSupport(Class<?> clazz);

    /** 返回此分发器处理的消息类型。 */
    Class<?> getMessageClass();

    /** 注册一个订阅者，订阅者需声明自己支持的消息类型。 */
    void addSubscriber(MessageSubscriber subscriber);

    /** 发布一条消息，分发给所有已注册的订阅者。 */
    void publish(Object message);
}
