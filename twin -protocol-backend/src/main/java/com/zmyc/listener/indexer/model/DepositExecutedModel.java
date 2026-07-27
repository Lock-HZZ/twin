package com.zmyc.listener.indexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositExecutedModel {

    private String user;

    private Long nonce;

    private BigDecimal usdcAmount;

    private BigDecimal toDividend;

    private BigDecimal tipBought;

    private BigDecimal liquidity;

    private String txHash;
}
