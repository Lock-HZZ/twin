package com.zmyc.bamboo.core.engine;

/**
 * 区块链事件索引引擎接口。
 *
 * <p>负责管理扫链任务的生命周期，支持启动和停止。
 * 默认实现见 {@link DefaultIndexerEngine}。
 */
public interface IndexerEngine {

    /** 启动扫链引擎，开始定时轮询区块事件。 */
    void start();

    /** 停止扫链引擎，取消当前调度任务。 */
    void stop();
}
