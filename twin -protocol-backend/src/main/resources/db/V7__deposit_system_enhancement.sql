-- V7: 入金系统增强 - 签名生成模式
-- 后端生成 EIP-712 签名，前端调用合约 depositWithSig

-- users 表增加角色字段（默认普通用户）
ALTER TABLE `users`
ADD COLUMN `role` INT NOT NULL DEFAULT 0 COMMENT '用户角色：0-普通用户, 1-黄金节点, 2-钻石节点, 3-皇冠节点, 4-合伙人';

-- Nonce 防重放表
CREATE TABLE IF NOT EXISTS `deposit_nonce` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `nonce` BIGINT NOT NULL COMMENT '随机数（uint256）',
  `used_at` BIGINT NOT NULL COMMENT '使用时间戳（秒）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_nonce` (`user_id`, `nonce`),
  KEY `idx_used_at` (`used_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入金 nonce 防重放表';

-- 在 user_deposit 表增加 nonce 和 expires_at 字段（如果表已存在）
ALTER TABLE `user_deposit`
ADD COLUMN `nonce` BIGINT COMMENT '签名 nonce' AFTER `weight`,
ADD COLUMN `expires_at` BIGINT COMMENT '过期时间（10位时间戳），PENDING 状态超过此时间自动变为 EXPIRED' AFTER `nonce`;

-- 过期清理任务索引：按 status + expires_at 定位过期 PENDING 订单
CREATE INDEX `idx_status_expires` ON `user_deposit`(`status`, `expires_at`);

-- 额度实时聚合索引：按 status + created_date 聚合当日 PENDING/COMPLETED 金额
CREATE INDEX `idx_status_created` ON `user_deposit`(`status`, `created_date`);

-- 系统配置表新增配置项（如果表已存在）
INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_date`)
VALUES
  ('daily.max.deposit', '50000', '每日最大入金额度（USDC）', UNIX_TIMESTAMP()),
  ('allowed.deposit.levels', '0,1,2,3,4', '允许入金的用户角色（逗号分隔）：0-普通用户,1-黄金节点,2-钻石节点,3-皇冠节点,4-合伙人', UNIX_TIMESTAMP()),
  ('weight.growth.rate', '0.01', '权重增长率（每天）', UNIX_TIMESTAMP())
ON DUPLICATE KEY UPDATE
  `config_value` = VALUES(`config_value`),
  `updated_date` = UNIX_TIMESTAMP();
