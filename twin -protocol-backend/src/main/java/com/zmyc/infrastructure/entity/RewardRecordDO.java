package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 通用奖励记录表
 * 统一记录所有类型的链上奖励发放
 */
@Data
@TableName("reward_record")
public class RewardRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 奖励金额（业务精度） */
    private BigDecimal amount;

    /** 奖励类型：1-质押分红, 2-推荐奖励, 3-LP挖矿 */
    private Byte rewardType;

    /** 资产类型：0-USDC, 1-TIP */
    private Byte assetType;

    /** 批次ID（32字节hex，链上幂等标识） */
    private String batchId;

    /** 业务关联ID（如 user_stake.id、referral_record.id 等） */
    private Long businessId;

    /** 发放日期（yyyyMMdd格式，如 20260721） */
    private Integer rewardDate;

    /** 状态：0-PENDING, 1-SENT, 2-PAID, 3-FAILED */
    private Status status;

    /** 交易哈希 */
    private String txHash;

    /** 发送时间（秒级时间戳） */
    private Long sentAt;

    /** 确认时间（秒级时间戳） */
    private Long paidAt;

    /** 过期时间（秒级时间戳，PENDING/SENT 状态超过此时间可被补偿任务处理） */
    private Long expiresAt;

    /** 备注 */
    private String remark;

    /** 创建时间（秒级时间戳） */
    private Long createdDate;

    /** 更新时间（秒级时间戳） */
    private Long updatedDate;

    public enum Status {
        PENDING(0),  // 待发送
        SENT(1),     // 已发送，待确认
        PAID(2),     // 已确认成功
        FAILED(3);   // 失败

        private final int value;

        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
