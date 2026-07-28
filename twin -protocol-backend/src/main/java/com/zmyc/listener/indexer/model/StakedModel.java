package com.zmyc.listener.indexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StakedModel {

    private String user;

    private Long stakeId;

    private BigDecimal amount;

    private Integer plan;

    private Long startTime;

    private Long endTime;

    private String txHash;
}
