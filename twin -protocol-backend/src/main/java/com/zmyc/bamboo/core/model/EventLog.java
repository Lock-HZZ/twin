package com.zmyc.bamboo.core.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Objects;

public class EventLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigInteger id;
    private BigInteger chainId;
    private String address;
    /** 多个 topic 以英文逗号拼接，例如 "0xabc,0xdef" */
    private String topics;
    private String data;
    private BigInteger blockNumber;
    private String transactionHash;
    private BigInteger transactionIndex;
    private String blockHash;
    private BigInteger logIndex;
    private Boolean removed;
    /** 业务是否已处理：false-未处理，true-已处理 */
    private Boolean processed;
    private OffsetDateTime createdDate;
    private OffsetDateTime lastModifiedDate;

    public EventLog() {}

    public EventLog(BigInteger id, String address, String topics, String data,
                    BigInteger blockNumber, String transactionHash, BigInteger transactionIndex,
                    String blockHash, BigInteger logIndex, Boolean removed,
                    Boolean processed, OffsetDateTime createdDate, OffsetDateTime lastModifiedDate) {
        this.id = id;
        this.address = address;
        this.topics = topics;
        this.data = data;
        this.blockNumber = blockNumber;
        this.transactionHash = transactionHash;
        this.transactionIndex = transactionIndex;
        this.blockHash = blockHash;
        this.logIndex = logIndex;
        this.removed = removed;
        this.processed = processed;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    public BigInteger getId() { return id; }
    public void setId(BigInteger id) { this.id = id; }

    public BigInteger getChainId() { return chainId; }
    public void setChainId(BigInteger chainId) { this.chainId = chainId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTopics() { return topics; }
    public void setTopics(String topics) { this.topics = topics; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public BigInteger getBlockNumber() { return blockNumber; }
    public void setBlockNumber(BigInteger blockNumber) { this.blockNumber = blockNumber; }

    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }

    public BigInteger getTransactionIndex() { return transactionIndex; }
    public void setTransactionIndex(BigInteger transactionIndex) { this.transactionIndex = transactionIndex; }

    public String getBlockHash() { return blockHash; }
    public void setBlockHash(String blockHash) { this.blockHash = blockHash; }

    public BigInteger getLogIndex() { return logIndex; }
    public void setLogIndex(BigInteger logIndex) { this.logIndex = logIndex; }

    public Boolean getRemoved() { return removed; }
    public void setRemoved(Boolean removed) { this.removed = removed; }

    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }

    public OffsetDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(OffsetDateTime createdDate) { this.createdDate = createdDate; }

    public OffsetDateTime getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(OffsetDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventLog)) return false;
        EventLog that = (EventLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "EventLog{id=" + id + ", address='" + address + "', topics='" + topics +
               "', transactionHash='" + transactionHash + "', blockNumber=" + blockNumber + "}";
    }
}
