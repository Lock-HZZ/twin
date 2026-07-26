package com.zmyc.bamboo.core.manager;

import com.zmyc.bamboo.core.model.EventLog;

import java.math.BigInteger;
import java.util.List;

/**
 * 区块链 RPC 管理器接口。
 *
 * <p>封装与区块链节点的通信，提供获取链信息和拉取事件日志的能力。
 * 默认实现见 {@link com.zmyc.bamboo.core.manager.impl.DefaultRpcManager}（基于 web3j）。
 */
public interface RpcManager {

    /** 获取当前连接链的 Chain ID。 */
    BigInteger getChainId();

    /** 获取链上最新区块高度。 */
    BigInteger getLatestBlockHeight();

    /**
     * 拉取指定区块范围内、指定合约地址的事件日志（调用 eth_getLogs）。
     *
     * @param fromBlock         起始区块（含）
     * @param toBlock           结束区块（含）
     * @param contractAddresses 要监听的合约地址列表
     * @return 原始事件日志列表
     */
    List<EventLog> getLogs(BigInteger fromBlock, BigInteger toBlock, List<String> contractAddresses);
}
