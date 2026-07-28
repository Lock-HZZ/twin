package com.zmyc.common.enums;

public enum Decimals {

    USDC(6),
    TIP(18);

    public final int value;

    Decimals(int value) {
        this.value = value;
    }

    public static Decimals of(int value) {
        for (Decimals t : values()) {
            if (t.value == value) return t;
        }
        throw new IllegalArgumentException("Unknown decimals: " + value);
    }

}
