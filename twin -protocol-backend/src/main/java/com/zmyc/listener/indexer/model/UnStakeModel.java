package com.zmyc.listener.indexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnStakeModel {

    private Long stakeId;

    private String user;

    private String txHash;
}
