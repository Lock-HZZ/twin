package com.zmyc.bamboo.core.engine.dispatcher.subscriber;

import java.util.concurrent.Flow;

/**
 * 消息订阅者接口。
 *
 * <p>业务方实现此接口（推荐继承 {@link AbstractMessageSubscriber}）来消费事件。
 * 每个订阅者声明自己支持的消息类型，框架在分发时按类型路由。
 *
 * <p><b>使用方式：</b>
 * <pre>
 * {@literal @}Component
 * public class MySubscriber extends AbstractMessageSubscriber {
 *     {@literal @}Override
 *     public boolean isSupport(Class<?> clazz) {
 *         return EventLog.class.isAssignableFrom(clazz);
 *     }
 *     {@literal @}Override
 *     public void consume(Object message) {
 *         myService.process((EventLog) message); // 委托给 @Transactional Service
 *     }
 * }
 * </pre>
 */
public interface MessageSubscriber {

    /** 判断此订阅者是否支持处理指定类型的消息。 */
    boolean isSupport(Class<?> clazz);

    /** 返回内部的 JDK Flow 订阅者实例，由框架使用，业务方无需关心。 */
    Flow.Subscriber<Object> getInnerSubscriber();

    /**
     * 消费一条消息，由框架异步回调。
     *
     * <p><b>注意：</b>此方法由框架内部直接调用，无法被 Spring AOP 代理拦截，
     * 因此不能在此方法上直接使用 {@code @Transactional}。
     * 若需要事务支持，请将业务逻辑委托给一个独立的 Spring Bean 方法。
     */
    void consume(Object message);
}
