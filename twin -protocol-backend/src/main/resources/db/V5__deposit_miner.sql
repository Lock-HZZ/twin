-- ============================================================
-- 用户入金记录表
-- ============================================================

CREATE TABLE IF NOT EXISTS `user_deposit` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `amount`            DECIMAL(30,8)   NOT NULL COMMENT '入金数量（USDC）',
    `tx_hash`           VARCHAR(128)    NOT NULL COMMENT '交易哈希',
    `miner_count`       INT             DEFAULT NULL COMMENT '购买矿机数量',
    `miner_price_usdc`  DECIMAL(30,8)   DEFAULT NULL COMMENT '矿机单价（USDC）',
    `energy_earned`     DECIMAL(30,8)   DEFAULT NULL COMMENT '获得的能量值',
    `energy_multiplier` DECIMAL(10,4)   DEFAULT NULL COMMENT '能量倍率',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    `created_by`        BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `last_updated_by`   BIGINT          DEFAULT NULL COMMENT '最后更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tx_hash` (`tx_hash`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户入金记录表';
