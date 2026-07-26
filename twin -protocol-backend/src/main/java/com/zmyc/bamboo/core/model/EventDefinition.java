package com.zmyc.bamboo.core.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * 合约事件定义，描述需要监听的某一类事件。
 *
 * <p>对应配置文件中 {@code bamboo.event-listener.contract-event-definitions} 的列表项。
 * 过滤时以 {@code signatureHash} 匹配链上日志的 {@code topics[0]}。
 */
public class EventDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件签名，例如 {@code Transfer(address,address,uint256)}，仅用于描述，不参与过滤。 */
    private String signature;

    /**
     * 事件签名的 Keccak-256 哈希，例如 {@code 0xddf252ad...}。
     * 链上日志的 {@code topics[0]} 即为此值，用于精确匹配事件类型。
     */
    private String signatureHash;

    /** 事件描述，便于识别，不参与过滤逻辑。 */
    private String description;

    public EventDefinition() {}

    public EventDefinition(String signature, String signatureHash, String description) {
        this.signature = signature;
        this.signatureHash = signatureHash;
        this.description = description;
    }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getSignatureHash() { return signatureHash; }
    public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventDefinition)) return false;
        EventDefinition that = (EventDefinition) o;
        return Objects.equals(signatureHash, that.signatureHash);
    }

    @Override
    public int hashCode() { return Objects.hash(signatureHash); }

    @Override
    public String toString() {
        return "EventDefinition{signature='" + signature + "', signatureHash='" + signatureHash + "'}";
    }
}
