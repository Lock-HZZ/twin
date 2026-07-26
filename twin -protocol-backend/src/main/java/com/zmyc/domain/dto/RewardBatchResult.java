package com.zmyc.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量奖励发放结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardBatchResult {
    /** 批次ID */
    private String batchId;

    /** 交易哈希 */
    private String txHash;

    /** 发送数量 */
    private Integer count;

    /** 发送状态：SUCCESS-成功, PENDING-待确认, FAILED-失败 */
    private Status status;

    /** 错误信息 */
    private String errorMessage;

    public enum Status {
        SUCCESS,
        PENDING,
        FAILED
    }
}
