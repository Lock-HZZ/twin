package com.zmyc.bamboo.core.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 扫链检查点，记录每条链当前扫描到的区块高度。
 *
 * <p>以 {@code chainId} 为主键，每条链唯一一条记录。
 * 引擎每轮扫描完成后更新 {@code blockHeight}，下次从此处继续。
 * 对应数据库表 {@code bamboo_check_point}。
 */
public class CheckPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 链 ID（主键）。 */
    private BigInteger chainId;

    /** 下次扫描的起始区块高度（即上次扫描结束区块 + 1）。 */
    private BigInteger blockHeight;

    private OffsetDateTime createdDate;
    private OffsetDateTime lastModifiedDate;

    public CheckPoint() {}

    public CheckPoint(BigInteger chainId, BigInteger blockHeight,
                      OffsetDateTime createdDate, OffsetDateTime lastModifiedDate) {
        this.chainId = chainId;
        this.blockHeight = blockHeight;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    public BigInteger getChainId() { return chainId; }
    public void setChainId(BigInteger chainId) { this.chainId = chainId; }

    public BigInteger getBlockHeight() { return blockHeight; }
    public void setBlockHeight(BigInteger blockHeight) { this.blockHeight = blockHeight; }

    public OffsetDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(OffsetDateTime createdDate) { this.createdDate = createdDate; }

    public OffsetDateTime getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(OffsetDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckPoint)) return false;
        CheckPoint that = (CheckPoint) o;
        return Objects.equals(chainId, that.chainId);
    }

    @Override
    public int hashCode() { return Objects.hash(chainId); }

    @Override
    public String toString() {
        return "CheckPoint{chainId=" + chainId + ", blockHeight=" + blockHeight + "}";
    }
}
