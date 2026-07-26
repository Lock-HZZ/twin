package com.zmyc.common.util;

import java.util.regex.Pattern;

/**
 * 以太坊地址验证工具类
 */
public class EthereumAddressValidator {
    
    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    
    public static boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return true;
        }
        return !ETH_ADDRESS_PATTERN.matcher(address).matches();
    }
    
}

