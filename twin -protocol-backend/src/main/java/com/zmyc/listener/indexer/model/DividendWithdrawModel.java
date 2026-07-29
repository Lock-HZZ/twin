package com.zmyc.listener.indexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DividendWithdrawModel {

    private String user;

    private int rewardType;

    private int assetType;

    /** 用户实际到账金额（扣费后） */
    private BigDecimal amount;

    /** 手续费金额（已转入 FeeDividend 合约） */
    private BigDecimal fee;

    private String txHash;
}
