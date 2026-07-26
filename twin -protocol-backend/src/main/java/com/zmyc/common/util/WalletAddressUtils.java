package com.zmyc.common.util;

import org.web3j.crypto.Keys;

/**
 * 钱包地址工具类：用于获取地址的正确（EIP-55）大小写格式
 */
public final class WalletAddressUtils {

    private WalletAddressUtils() {
    }

    /**
     * 将地址格式化为 EIP-55 校验格式（严格模式）
     */
    public static String toChecksumAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("address is null or empty");
        }

        String normalized = address.trim();

        // 统一加上 0x 前缀
        if (!normalized.startsWith("0x") && !normalized.startsWith("0X")) {
            normalized = "0x" + normalized;
        }

        // 先全部转小写，便于校验
        normalized = "0x" + normalized.substring(2).toLowerCase();

        if (EthereumAddressValidator.validateAddress(normalized)) {
            throw new IllegalArgumentException("invalid ethereum address format: " + address);
        }

        return Keys.toChecksumAddress(normalized);
    }

}

