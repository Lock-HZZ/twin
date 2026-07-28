package com.zmyc.common.enums;

import java.math.BigInteger;

public enum AssetType {

    USDC((byte) 0, BigInteger.TEN.pow(6)),
    TIP((byte) 1, BigInteger.TEN.pow(18));

    public final byte code;
    public final BigInteger decimals;

    AssetType(byte code, BigInteger decimals) {
        this.code = code;
        this.decimals = decimals;
    }

    public static AssetType of(byte code) {
        for (AssetType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown asset type: " + code);
    }

}
