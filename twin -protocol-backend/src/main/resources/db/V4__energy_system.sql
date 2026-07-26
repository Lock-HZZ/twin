-- ============================================================
-- 系统配置表
-- ============================================================

CREATE TABLE IF NOT EXISTS `system_config` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`        VARCHAR(64)     NOT NULL COMMENT '配置键',
    `config_value`      VARCHAR(255)    NOT NULL COMMENT '配置值',
    `description`       VARCHAR(255)    DEFAULT NULL COMMENT '配置说明',
    `enabled`           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    `created_by`        BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `last_updated_by`   BIGINT          DEFAULT NULL COMMENT '最后更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入默认配置：入金能量倍率 = 3
INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_date`, `last_updated_date`)
VALUES ('deposit.energy.multiplier', '3.0', '入金能量倍率：能量值 = 入金金额(USDT) * 倍率', UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

-- ============================================================
-- 用户能量值表
-- ============================================================

CREATE TABLE IF NOT EXISTS `user_energy` (
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `energy_balance`    DECIMAL(30,8)   NOT NULL DEFAULT 0.00000000 COMMENT '能量值余额',
    `total_earned`      DECIMAL(30,8)   NOT NULL DEFAULT 0.00000000 COMMENT '累计获得能量值',
    `total_consumed`    DECIMAL(30,8)   NOT NULL DEFAULT 0.00000000 COMMENT '累计消耗能量值',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    `created_by`        BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `last_updated_by`   BIGINT          DEFAULT NULL COMMENT '最后更新人ID',
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户能量值表';

-- ============================================================
-- 能量值流水表
-- ============================================================

CREATE TABLE IF NOT EXISTS `energy_transaction` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `transaction_type`  TINYINT         NOT NULL COMMENT '交易类型: 1-入金获得, 2-消耗',
    `amount`            DECIMAL(30,8)   NOT NULL COMMENT '能量值变动数量(正数为增加,负数为减少)',
    `balance_before`    DECIMAL(30,8)   NOT NULL COMMENT '变动前余额',
    `balance_after`     DECIMAL(30,8)   NOT NULL COMMENT '变动后余额',
    `related_id`        BIGINT          DEFAULT NULL COMMENT '关联业务ID(入金ID/消耗业务ID等)',
    `remark`            VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    `created_by`        BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `last_updated_by`   BIGINT          DEFAULT NULL COMMENT '最后更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_date` (`created_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能量值流水表';
