-- V8__stake_system.sql

-- 用户质押记录表
CREATE TABLE IF NOT EXISTS `user_stake` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `stake_id` BIGINT NOT NULL COMMENT '链上质押ID',
  `amount` DECIMAL(20,6) NOT NULL COMMENT '质押数量（TIP）',
  `plan` INT NOT NULL COMMENT '质押套餐（天数：30/90/180/360）',
  `apy` DECIMAL(10,2) NOT NULL COMMENT '年化收益率（例如：5.0 表示 5%）',
  `start_time` BIGINT NOT NULL COMMENT '开始时间（秒）',
  `end_time` BIGINT NOT NULL COMMENT '到期时间（秒）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-进行中，1-已赎回',
  `tx_hash` VARCHAR(255) COMMENT '交易哈希',
  `created_date` BIGINT NOT NULL COMMENT '创建时间（秒）',
  `updated_date` BIGINT COMMENT '更新时间（秒）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stake_id` (`stake_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户质押记录表';

-- 质押分红记录表
CREATE TABLE IF NOT EXISTS `stake_dividend_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `stake_id` BIGINT NOT NULL COMMENT '质押记录ID（关联user_stake.id）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(20,6) NOT NULL COMMENT '分红金额（TIP）',
  `dividend_date` INT NOT NULL COMMENT '分红日期（yyyyMMdd格式）',
  `batch_id` VARCHAR(66) COMMENT '批次ID（链上幂等去重，同批次共享）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待发放，1-已发送待确认，2-已发放，3-失败',
  `paid_time` BIGINT COMMENT '发放时间（秒）',
  `tx_hash` VARCHAR(255) COMMENT '交易哈希',
  `created_date` BIGINT NOT NULL COMMENT '创建时间（秒）',
  `updated_date` BIGINT COMMENT '更新时间（秒）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stake_date` (`stake_id`, `dividend_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_date` (`status`, `dividend_date`),
  KEY `idx_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质押分红记录表';

-- 插入质押APY配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_date`)
VALUES
  ('stake.apy.30', '5.0', '30天质押年化收益率（%）', UNIX_TIMESTAMP()),
  ('stake.apy.90', '8.0', '90天质押年化收益率（%）', UNIX_TIMESTAMP()),
  ('stake.apy.180', '12.0', '180天质押年化收益率（%）', UNIX_TIMESTAMP()),
  ('stake.apy.360', '15.0', '360天质押年化收益率（%）', UNIX_TIMESTAMP())
ON DUPLICATE KEY UPDATE
  `config_value` = VALUES(`config_value`),
  `description` = VALUES(`description`);
