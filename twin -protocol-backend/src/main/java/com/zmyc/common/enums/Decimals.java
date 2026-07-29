package com.zmyc.common.enums;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public enum Decimals {

    USDC(new BigDecimal("10").pow(6)),
    TIP(new BigDecimal("10").pow(18));

    public final BigDecimal value;

    Decimals(BigDecimal value) {
        this.value = value;
    }

    public static Decimals of(BigDecimal value) {
        for (Decimals t : values()) {
            if (Objects.equals(t.value, value)) return t;
        }
        throw new IllegalArgumentException("Unknown decimals: " + value);
    }

}
