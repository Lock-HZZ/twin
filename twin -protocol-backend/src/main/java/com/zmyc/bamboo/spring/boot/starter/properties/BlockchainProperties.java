package com.zmyc.bamboo.spring.boot.starter.properties;

import java.math.BigInteger;

/**
 * 单条区块链节点配置属性。
 */
public class BlockchainProperties {

    /** 链 ID，用于区分不同链的检查点（如 BSC 主网为 56）。 */
    private BigInteger id;

    /** JSON-RPC 节点地址，例如 {@code https://bsc-dataseed.binance.org/}。 */
    private String rpcNodeUrl;

    /** 该链的事件监听配置。 */
    private EventListenerProperties eventListener;

    public BlockchainProperties() {}

    public BlockchainProperties(BigInteger id, String rpcNodeUrl) {
        this.id = id;
        this.rpcNodeUrl = rpcNodeUrl;
    }

    public BigInteger getId() { return id; }
    public void setId(BigInteger id) { this.id = id; }

    public String getRpcNodeUrl() { return rpcNodeUrl; }
    public void setRpcNodeUrl(String rpcNodeUrl) { this.rpcNodeUrl = rpcNodeUrl; }

    public EventListenerProperties getEventListener() { return eventListener; }
    public void setEventListener(EventListenerProperties eventListener) { this.eventListener = eventListener; }

    @Override
    public String toString() {
        return "BlockchainProperties{id=" + id + ", rpcNodeUrl='" + rpcNodeUrl + "'}";
    }
}
