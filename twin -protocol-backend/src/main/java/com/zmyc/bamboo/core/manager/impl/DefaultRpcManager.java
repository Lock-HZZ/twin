package com.zmyc.bamboo.core.manager.impl;

import com.zmyc.bamboo.core.manager.RpcManager;
import com.zmyc.bamboo.core.model.EventLog;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthChainId;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 基于 web3j 的 RPC 管理器默认实现。
 *
 * <p>所有 RPC 调用均为同步阻塞模式。网络超时或节点错误会抛出 {@link RuntimeException}，
 * 由 {@link com.zmyc.bamboo.core.engine.IndexerTask} 的重试机制处理。
 */
public class DefaultRpcManager implements RpcManager {

    private final Web3j web3j;

    public DefaultRpcManager(Web3j web3j) {
        this.web3j = web3j;
    }

    @Override
    public BigInteger getChainId() {
        try {
            EthChainId response = (EthChainId) web3j.ethChainId().send();
            if (response.hasError()) {
                throw new RuntimeException(response.getError().getMessage());
            }
            return response.getChainId();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BigInteger getLatestBlockHeight() {
        try {
            EthBlockNumber response = (EthBlockNumber) web3j.ethBlockNumber().send();
            if (response.hasError()) {
                throw new RuntimeException(response.getError().getMessage());
            }
            return response.getBlockNumber();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用 eth_getLogs 拉取指定范围内的原始事件日志。
     *
     * <p>单次请求范围过大（超过 1000~2000 块）时，部分节点会拒绝并返回错误，
     * 建议结合 {@code IndexerTask} 的 STEP_BLOCKS 配置控制每次请求的区块数。
     */
    @Override
    public List<EventLog> getLogs(BigInteger fromBlock, BigInteger toBlock,
                                  List<String> contractAddresses) {
        try {
            EthFilter ethFilter = new EthFilter(
                    DefaultBlockParameter.valueOf(fromBlock),
                    DefaultBlockParameter.valueOf(toBlock),
                    contractAddresses
            );

            EthLog response = (EthLog) web3j.ethGetLogs(ethFilter).send();
            if (response.hasError()) {
                throw new RuntimeException(response.getError().getMessage());
            }

            return response.getLogs().stream()
                    .map(logResult -> toEventLog((Log) logResult.get()))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 将 web3j 原始 Log 对象转换为框架内部的 EventLog 模型。 */
    private EventLog toEventLog(Log log) {
        // topics 列表以英文逗号拼接存储，topics[0] 为事件签名哈希
        String topics = String.join(",", log.getTopics());
        return new EventLog(
                null,
                log.getAddress(),
                topics,
                log.getData(),
                log.getBlockNumber(),
                log.getTransactionHash(),
                log.getTransactionIndex(),
                log.getBlockHash(),
                log.getLogIndex(),
                log.isRemoved(),
                false,
                OffsetDateTime.now(),
                null
        );
    }
}
