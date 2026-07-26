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

-- ============================================================
-- 星座矿机配置表
-- ============================================================

CREATE TABLE IF NOT EXISTS `miner_zodiac_config` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `zodiac_type`       VARCHAR(32)     NOT NULL COMMENT '星座类型：aries/taurus/gemini等',
    `zodiac_name`       VARCHAR(64)     NOT NULL COMMENT '星座名称：白羊座/金牛座等',
    `drop_rate`         DECIMAL(10,4)   NOT NULL COMMENT '抽取概率（百分比，如8.33表示8.33%）',
    `enabled`           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    `created_by`        BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `last_updated_by`   BIGINT          DEFAULT NULL COMMENT '最后更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_zodiac_type` (`zodiac_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='星座矿机配置表';

-- 插入十二星座配置（概率均等，每个约8.33%）
INSERT INTO `miner_zodiac_config` (`zodiac_type`, `zodiac_name`, `drop_rate`, `created_date`, `last_updated_date`)
VALUES
    ('aries', '白羊座', 8.33, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('taurus', '金牛座', 8.33, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('gemini', '双子座', 8.33, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('cancer', '巨蟹座', 8.33, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('leo', '狮子座', 8.33, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('virgo', '处女座', 8.33, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('libra', '天秤座', 8.33, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('scorpio', '天蝎座', 8.33, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('sagittarius', '射手座', 8.34, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('capricorn', '摩羯座', 8.34, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('aquarius', '水瓶座', 8.34, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
    ('pisces', '双鱼座', 8.34, UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

-- ============================================================
-- 用户矿机表
-- ============================================================

CREATE TABLE IF NOT EXISTS `user_miner` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `miner_type`        VARCHAR(32)     NOT NULL COMMENT '矿机类型：十二星座类型或super',
    `purchase_price`    DECIMAL(30,8)   NOT NULL COMMENT '购买价格（USDC）',
    `deposit_id`        BIGINT          DEFAULT NULL COMMENT '关联入金记录ID（合成的为NULL）',
    `is_synthesized`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否合成: 0-抽取获得, 1-合成获得',
    `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1-挖矿中, 2-已到期可合成, 3-已合成消耗',
    `activated_date`    BIGINT          NOT NULL COMMENT '激活时间（开始挖矿时间，入金次日零点）',
    `expired_date`      BIGINT          NOT NULL COMMENT '到期时间（可合成时间）',
    `total_mined`       DECIMAL(30,8)   NOT NULL DEFAULT 0.00000000 COMMENT '累计产出（分红）',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    `created_by`        BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `last_updated_by`   BIGINT          DEFAULT NULL COMMENT '最后更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_deposit_id` (`deposit_id`),
    KEY `idx_miner_type` (`miner_type`),
    KEY `idx_status` (`status`),
    KEY `idx_expired_date` (`expired_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户矿机表';

-- ============================================================
-- 矿机合成记录表
-- ============================================================

CREATE TABLE IF NOT EXISTS `miner_synthesis_log` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `consumed_miner_ids` TEXT           NOT NULL COMMENT '消耗的矿机ID列表（JSON数组）',
    `result_miner_id`   BIGINT          NOT NULL COMMENT '合成结果矿机ID',
    `result_miner_type` VARCHAR(32)     NOT NULL COMMENT '合成结果矿机类型',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    `created_by`        BIGINT          DEFAULT NULL COMMENT '创建人ID',
    `last_updated_by`   BIGINT          DEFAULT NULL COMMENT '最后更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_result_miner_id` (`result_miner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='矿机合成记录表';

-- 插入默认配置：矿机单价
INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_date`, `last_updated_date`)
VALUES ('miner.card.price', '100.0', '卡牌矿机单价（USDC）', UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

-- 插入默认配置：矿机挖矿天数
INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_date`, `last_updated_date`)
VALUES ('miner.mining.days', '30', '矿机挖矿天数（到期后可合成）', UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

