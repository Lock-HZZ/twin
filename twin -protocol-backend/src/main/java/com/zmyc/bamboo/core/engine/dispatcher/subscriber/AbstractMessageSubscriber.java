package com.zmyc.bamboo.core.engine.dispatcher.subscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;

/**
 * 消息订阅者抽象基类。
 *
 * <p>基于 JDK {@link Flow} API 实现背压感知的异步消费。
 * 业务方继承此类并实现 {@link #consume(Object)} 即可。
 *
 * <p><b>消费语义：</b>
 * <ul>
 *   <li>每次消费一条消息（request(1) 模式），处理完成后自动请求下一条</li>
 *   <li>{@link #consume} 抛出异常时，框架记录错误日志并跳过该消息，继续消费后续消息</li>
 *   <li>跳过的消息 {@code processed} 仍为 0，重启后会由框架重新投递</li>
 * </ul>
 */
public abstract class AbstractMessageSubscriber implements MessageSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageSubscriber.class);

    private final Flow.Subscriber<Object> subscriber;

    protected AbstractMessageSubscriber() {
        this.subscriber = new Flow.Subscriber<>() {

            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1); // 初始请求第一条消息
            }

            @Override
            public void onNext(Object item) {
                try {
                    AbstractMessageSubscriber.this.consume(item);
                } catch (Exception e) {
                    // 消费失败：记录日志，跳过此条，继续处理后续消息
                    // 跳过的消息 processed=0，重启时会被重新投递（at-least-once）
                    LOGGER.error("bamboo :: consume failed, message skipped: {}", item, e);
                } finally {
                    subscription.request(1); // 请求下一条
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("bamboo :: MessageSubscriber has error", throwable);
            }

            @Override
            public void onComplete() {
                LOGGER.debug("bamboo :: All items received");
            }
        };
    }

    @Override
    public Flow.Subscriber<Object> getInnerSubscriber() {
        return subscriber;
    }
}
