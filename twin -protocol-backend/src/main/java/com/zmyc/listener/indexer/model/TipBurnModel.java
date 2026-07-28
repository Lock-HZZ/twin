package com.zmyc.listener.indexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TipBurnModel {
    /** 分配给分红合约的数量（用于 distributeBurnDividend） */
    private BigDecimal toDividendAmount;
    /** 触发燃烧的交易 Hash */
    private String txHash;
}
