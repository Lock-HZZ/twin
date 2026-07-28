-- ============================================================
-- 每日分红记录表
-- ============================================================

CREATE TABLE IF NOT EXISTS `daily_dividend` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dividend_date`     DATE            NOT NULL COMMENT '分红日期',
    `total_tip_amount`  DECIMAL(30,8)   NOT NULL COMMENT '总分红TIP数量',
    `total_miner_count` INT             NOT NULL COMMENT '参与分红的矿机总数',
    `per_miner_amount`  DECIMAL(30,8)   NOT NULL COMMENT '每张矿机分红金额',
    `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1-待发放, 2-已发放, 3-已取消',
    `distributed_date`  BIGINT          DEFAULT NULL COMMENT '发放时间',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    `created_by`        BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `last_updated_by`   BIGINT          DEFAULT NULL COMMENT '最后更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dividend_date` (`dividend_date`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日分红记录表';

-- ============================================================
-- 用户分红明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_dividend_detail` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dividend_id`       BIGINT          NOT NULL COMMENT '分红记录ID',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `miner_count`       INT             NOT NULL COMMENT '用户参与分红的矿机数量',
    `tip_amount`        DECIMAL(30,8)   NOT NULL COMMENT '获得的TIP数量',
    `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1-待发放, 2-已发放',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    `created_by`        BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `last_updated_by`   BIGINT          DEFAULT NULL COMMENT '最后更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dividend_user` (`dividend_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户分红明细表';

-- 插入配置：每日分红TIP总量
INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_date`, `last_updated_date`)
VALUES ('daily.dividend.tip.amount', '10000.0', '每日分红TIP总量', UNIX_TIMESTAMP(), UNIX_TIMESTAMP());
