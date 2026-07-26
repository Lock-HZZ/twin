package com.zmyc.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {
    
    @Value("${node-order.thread-pool-size:4}")
    private int corePoolSize;
    
    @Value("${node-order.thread-pool-max-size:8}")
    private int maxPoolSize;
    
    @Value("${node-order.thread-pool-queue-capacity:100}")
    private int queueCapacity;
    
    /**
     * 节点订单释放任务线程池
     */
    @Bean("nodeOrderReleaseExecutor")
    public Executor nodeOrderReleaseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(corePoolSize);
        
        // 最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        
        // 队列容量
        executor.setQueueCapacity(queueCapacity);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("node-release-");
        
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(600);
        
        // 初始化
        executor.initialize();
        
        log.info("Node order release thread pool initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }
}

