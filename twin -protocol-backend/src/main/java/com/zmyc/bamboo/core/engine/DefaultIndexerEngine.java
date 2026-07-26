package com.zmyc.bamboo.core.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 扫链引擎默认实现。
 *
 * <p>使用 {@link ScheduledThreadPoolExecutor} 为每条链创建独立的定时任务，
 * 上一轮未完成时不会并发触发下一轮（scheduleWithFixedDelay 语义）。
 */
public class DefaultIndexerEngine implements IndexerEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultIndexerEngine.class);

    private static final int DEFAULT_INITIAL_SECONDS = 5;
    private static final int DEFAULT_DELAY_SECONDS = 2;
    private static final String THREAD_NAME_TEMPLATE = "bamboo-engine-%s";

    private final AtomicInteger counter = new AtomicInteger(0);
    private final List<IndexerTask> indexerTasks = new ArrayList<>();
    private final ScheduledThreadPoolExecutor executorService;
    private final int initialSeconds;
    private final int delaySeconds;
    private final List<ScheduledFuture<?>> currentFutures = new ArrayList<>();

    public DefaultIndexerEngine(int initialSeconds, int delaySeconds) {
        this.executorService = new ScheduledThreadPoolExecutor(4, defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        this.initialSeconds = initialSeconds;
        this.delaySeconds = delaySeconds;
    }

    public void addTask(IndexerTask task) {
        indexerTasks.add(task);
    }

    @Override
    public void start() {
        for (IndexerTask task : indexerTasks) {
            ScheduledFuture<?> future = executorService.scheduleWithFixedDelay(
                    task,
                    initialSeconds,
                    delaySeconds,
                    TimeUnit.SECONDS
            );
            currentFutures.add(future);
        }
    }

    @Override
    public void stop() {
        for (ScheduledFuture<?> future : currentFutures) {
            future.cancel(true);
        }
        currentFutures.clear();
    }

    private ThreadFactory defaultThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName(String.format(THREAD_NAME_TEMPLATE, counter.incrementAndGet()));
            return thread;
        };
    }
}
