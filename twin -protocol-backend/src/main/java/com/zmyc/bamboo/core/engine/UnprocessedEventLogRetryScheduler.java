package com.zmyc.bamboo.core.engine;

import com.zmyc.bamboo.core.dao.EventLogDao;
import com.zmyc.bamboo.core.engine.dispatcher.MessageDispatcher;
import com.zmyc.bamboo.core.model.EventLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定时扫描长时间处于 unprocessed 状态的事件日志并重新投递。
 *
 * <p>启动后每隔 {@code retryMinutes} 分钟检查一次 DB，找出 created_date 超过该时间
 * 仍未被 markProcessed 的记录，重新 publish 到 MessageDispatcher 触发再处理。
 */
public class UnprocessedEventLogRetryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnprocessedEventLogRetryScheduler.class);
    private static final String THREAD_NAME_TEMPLATE = "bamboo-retry-%s";

    private final AtomicInteger counter = new AtomicInteger(0);
    private final EventLogDao eventLogDao;
    private final List<MessageDispatcher> dispatchers;
    private final int retrySeconds;
    private final ScheduledThreadPoolExecutor executor;

    private ScheduledFuture<?> future;

    public UnprocessedEventLogRetryScheduler(EventLogDao eventLogDao,
                                              List<MessageDispatcher> dispatchers,
                                              int retrySeconds) {
        this.eventLogDao = eventLogDao;
        this.dispatchers = dispatchers;
        this.retrySeconds = retrySeconds;
        this.executor = buildExecutor();
    }

    public void start() {
        future = executor.scheduleWithFixedDelay(
                this::retry,
                retrySeconds,
                retrySeconds,
                TimeUnit.SECONDS
        );
        LOGGER.info("bamboo :: unprocessed retry scheduler started, check-interval={}s, threshold={}s",
                retrySeconds, retrySeconds);
    }

    public void stop() {
        if (future != null) future.cancel(true);
    }

    private void retry() {
        try {
            List<EventLog> stale = eventLogDao.findStaleUnprocessed(retrySeconds);
            if (stale.isEmpty()) return;
            LOGGER.warn("bamboo :: found {} stale unprocessed event log(s) (>{}s), re-dispatching...",
                    stale.size(), retrySeconds);
            for (MessageDispatcher dispatcher : dispatchers) {
                if (!dispatcher.isSupport(EventLog.class)) continue;
                for (EventLog log : stale) {
                    dispatcher.publish(log);
                }
            }
        } catch (Exception e) {
            LOGGER.error("bamboo :: error during unprocessed retry scan", e);
        }
    }

    private ScheduledThreadPoolExecutor buildExecutor() {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(String.format(THREAD_NAME_TEMPLATE, counter.incrementAndGet()));
            return t;
        };
        RejectedExecutionHandler reh = (r, ex) ->
                LOGGER.warn("bamboo :: retry task rejected, executor may be shutting down");
        return new ScheduledThreadPoolExecutor(1, tf, reh);
    }
}
