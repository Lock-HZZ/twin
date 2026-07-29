package com.zmyc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fee.dividend.contract")
public class FeeDividendContractConfig {

    private String contractAddress;

    private Long gasLimit = 3000000L;
}
