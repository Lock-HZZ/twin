-- ============================================================
-- Twin Protocol 数据库初始化脚本
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 区块链链配置
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blockchain_chain` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `chain_id`       BIGINT       NOT NULL             COMMENT '链ID（1=ETH主网,56=BSC）',
    `chain_name`     VARCHAR(64)  NOT NULL             COMMENT '链名称',
    `rpc_url`        VARCHAR(256) NOT NULL             COMMENT 'RPC节点地址',
    `explorer_url`   VARCHAR(256)                      COMMENT '区块浏览器地址',
    `native_symbol`  VARCHAR(16)                       COMMENT '原生币符号',
    `enabled`        TINYINT(1)   NOT NULL DEFAULT 1   COMMENT '0-禁用 1-启用',
    `created_date`   BIGINT                            COMMENT '创建时间（秒级时间戳）',
    `last_updated_date` BIGINT                         COMMENT '更新时间（秒级时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chain_id` (`chain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区块链链配置';

-- ------------------------------------------------------------
-- 代币配置
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blockchain_token` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `chain_id`         BIGINT        NOT NULL             COMMENT '所属链ID',
    `contract_address` VARCHAR(66)                        COMMENT '合约地址（原生币为空）',
    `symbol`           VARCHAR(32)   NOT NULL             COMMENT '代币符号',
    `name`             VARCHAR(64)                        COMMENT '代币全称',
    `decimals`         INT           NOT NULL DEFAULT 18  COMMENT '精度位数',
    `is_native`        TINYINT(1)    NOT NULL DEFAULT 0   COMMENT '0-非原生 1-原生币',
    `usdt_rate`        DECIMAL(30,18)                     COMMENT '对USDT汇率',
    `deposit_enabled`  TINYINT(1)    NOT NULL DEFAULT 0   COMMENT '是否允许入金',
    `enabled`          TINYINT(1)    NOT NULL DEFAULT 1   COMMENT '0-禁用 1-启用',
    `created_date`     BIGINT                             COMMENT '创建时间',
    `last_updated_date` BIGINT                            COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_chain_id` (`chain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代币配置';

-- ------------------------------------------------------------
-- 系统配置
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_config` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `config_key`       VARCHAR(128)  NOT NULL             COMMENT '配置键',
    `config_value`     TEXT                               COMMENT '配置值',
    `description`      VARCHAR(256)                       COMMENT '描述',
    `enabled`          TINYINT(1)    NOT NULL DEFAULT 1,
    `created_date`     BIGINT,
    `last_updated_date` BIGINT,
    `created_by`       BIGINT,
    `last_updated_by`  BIGINT,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

-- ------------------------------------------------------------
-- 用户
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id`               BIGINT        NOT NULL             COMMENT '雪花ID',
    `address`          VARCHAR(66)   NOT NULL             COMMENT '钱包地址',
    `invited_code`     VARCHAR(32)   NOT NULL             COMMENT '邀请码',
    `email`            VARCHAR(128)                       COMMENT '邮箱',
    `enabled`          TINYINT(1)    NOT NULL DEFAULT 1,
    `registration_ip`  VARCHAR(64),
    `last_login_ip`    VARCHAR(64),
    `role`             INT           NOT NULL DEFAULT 0   COMMENT '0-普通 1-黄金 2-钻石 3-皇冠 4-合伙人',
    `level`            INT           NOT NULL DEFAULT 0   COMMENT '动态分币等级 0-无 1-D1 ... 8-D8',
    `created_date`     BIGINT,
    `last_updated_date` BIGINT,
    `created_by`       BIGINT,
    `last_updated_by`  BIGINT,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_address` (`address`),
    UNIQUE KEY `uk_invited_code` (`invited_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 用户关系闭包（推荐关系）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_relation_closure` (
    `id`            BIGINT  NOT NULL AUTO_INCREMENT,
    `ancestor_id`   BIGINT  NOT NULL  COMMENT '祖先用户ID',
    `descendant_id` BIGINT  NOT NULL  COMMENT '子孙用户ID',
    `depth`         INT     NOT NULL  COMMENT '深度（1=直推）',
    `created_date`  BIGINT,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ancestor_descendant` (`ancestor_id`, `descendant_id`),
    KEY `idx_descendant_id` (`descendant_id`),
    KEY `idx_ancestor_depth` (`ancestor_id`, `depth`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关系闭包表';

-- ------------------------------------------------------------
-- 用户业绩
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_performance` (
    `user_id`               BIGINT         NOT NULL            COMMENT '用户ID',
    `personal_volume_usdt`  DECIMAL(30,6)  NOT NULL DEFAULT 0  COMMENT '个人业绩（自身入金）',
    `team_volume_usdt`      DECIMAL(30,6)  NOT NULL DEFAULT 0  COMMENT '团队业绩（所有下级入金，不含自身）',
    `community_volume_usdt` DECIMAL(30,6)  NOT NULL DEFAULT 0  COMMENT '小区业绩（各直推线之和-最大线）',
    `created_date`          BIGINT,
    `last_updated_date`     BIGINT,
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户业绩';

-- ------------------------------------------------------------
-- 用户入金
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_deposit` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT         NOT NULL,
    `amount`           DECIMAL(30,6)  NOT NULL             COMMENT '入金金额（USDC）',
    `tx_hash`          VARCHAR(66)                         COMMENT '链上交易哈希',
    `energy_earned`    DECIMAL(30,6)                       COMMENT '获得的能量值',
    `energy_multiplier` DECIMAL(10,4)                      COMMENT '能量倍率',
    `liquidity`        DECIMAL(30,18)                      COMMENT 'LP流动性数量',
    `weight`           DECIMAL(10,4)                       COMMENT '入金权重',
    `weighted_amount`  DECIMAL(30,18)                      COMMENT '加权金额 = amount * weight',
    `status`           INT            NOT NULL DEFAULT 0   COMMENT '0-PENDING 1-COMPLETED 2-EXPIRED 3-REMOVING 4-REMOVED',
    `nonce`            BIGINT                              COMMENT '随机数（防重放）',
    `expires_at`       BIGINT                              COMMENT '过期时间（秒级时间戳）',
    `withdraw_tx_hash` VARCHAR(66)                         COMMENT '移除LP的交易哈希',
    `created_date`     BIGINT,
    `last_updated_date` BIGINT,
    `created_by`       BIGINT,
    `last_updated_by`  BIGINT,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_nonce` (`nonce`),
    UNIQUE KEY `uk_tx_hash` (`tx_hash`),
    KEY `idx_user_id_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户入金记录';

-- ------------------------------------------------------------
-- 用户质押
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_stake` (
    `id`           BIGINT         NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT         NOT NULL,
    `stake_id`     BIGINT                              COMMENT '链上质押ID',
    `amount`       DECIMAL(30,18) NOT NULL             COMMENT '质押数量（TIP）',
    `plan`         INT                                 COMMENT '质押套餐天数（30/90/180/360）',
    `apy`          DECIMAL(10,4)                       COMMENT '年化收益率',
    `start_time`   BIGINT                              COMMENT '开始时间（秒）',
    `end_time`     BIGINT                              COMMENT '到期时间（秒）',
    `status`       INT            NOT NULL DEFAULT 0   COMMENT '0-进行中 1-已赎回',
    `tx_hash`      VARCHAR(66),
    `created_date`     BIGINT,
    `last_updated_date` BIGINT,
    `created_by`       BIGINT,
    `last_updated_by`  BIGINT,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_stake_id` (`stake_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户质押记录';

-- ------------------------------------------------------------
-- 用户能量
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_energy` (
    `user_id`         BIGINT         NOT NULL           COMMENT '用户ID（主键）',
    `energy_balance`  DECIMAL(30,6)  NOT NULL DEFAULT 0 COMMENT '当前能量余额',
    `total_earned`    DECIMAL(30,6)  NOT NULL DEFAULT 0 COMMENT '累计获得能量',
    `total_consumed`  DECIMAL(30,6)  NOT NULL DEFAULT 0 COMMENT '累计消耗能量',
    `created_date`     BIGINT,
    `last_updated_date` BIGINT,
    `created_by`       BIGINT,
    `last_updated_by`  BIGINT,
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户能量账户';

-- ------------------------------------------------------------
-- 能量交易明细
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `energy_transaction` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT         NOT NULL,
    `transaction_type` TINYINT        NOT NULL             COMMENT '1-入金获取 2-消耗',
    `amount`           DECIMAL(30,6)  NOT NULL,
    `balance_before`   DECIMAL(30,6)  NOT NULL,
    `balance_after`    DECIMAL(30,6)  NOT NULL,
    `related_id`       BIGINT                              COMMENT '关联业务ID',
    `remark`           VARCHAR(256),
    `created_date`     BIGINT,
    `last_updated_date` BIGINT,
    `created_by`       BIGINT,
    `last_updated_by`  BIGINT,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能量交易明细';

-- ------------------------------------------------------------
-- 余额交易明细
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `balance_transaction` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT         NOT NULL,
    `token_id`       BIGINT                              COMMENT '代币ID',
    `tx_type`        TINYINT        NOT NULL             COMMENT '1-分红入账 2-提现出账 3-冻结 4-解冻',
    `amount`         DECIMAL(30,18) NOT NULL,
    `balance_before` DECIMAL(30,18) NOT NULL,
    `balance_after`  DECIMAL(30,18) NOT NULL,
    `related_id`     BIGINT                              COMMENT '关联业务ID',
    `remark`         VARCHAR(256),
    `created_date`     BIGINT,
    `last_updated_date` BIGINT,
    `created_by`       BIGINT,
    `last_updated_by`  BIGINT,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额交易明细';

-- ------------------------------------------------------------
-- TIP燃烧记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tip_burn_record` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT,
    `burn_date`       BIGINT         NOT NULL             COMMENT '燃烧日期（当天零点时间戳）',
    `burn_rate`       INT                                 COMMENT '燃烧比例（基点，8000=80%）',
    `burn_amount`     DECIMAL(30,18)                      COMMENT '燃烧TIP数量',
    `dividend_amount` DECIMAL(30,18)                      COMMENT '进入分红池的TIP数量',
    `tx_hash`         VARCHAR(66),
    `status`          INT            NOT NULL DEFAULT 0   COMMENT '0-待执行 1-执行中 2-成功 3-失败',
    `retry_count`     INT            NOT NULL DEFAULT 0,
    `fail_reason`     VARCHAR(512),
    `created_date`     BIGINT,
    `last_updated_date` BIGINT,
    `created_by`       BIGINT,
    `last_updated_by`  BIGINT,
    PRIMARY KEY (`id`),
    KEY `idx_burn_date` (`burn_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TIP燃烧记录';

-- ------------------------------------------------------------
-- 通用奖励记录（质押分红/LP挖矿/燃烧分红等统一入此表）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `reward_record` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT         NOT NULL,
    `amount`      DECIMAL(30,18) NOT NULL                 COMMENT '奖励金额',
    `reward_type` TINYINT        NOT NULL                 COMMENT '1-质押分红 2-推荐奖励 3-LP挖矿 4-燃烧入金加权 5-燃烧节点加权 6-燃烧合伙人 7-燃烧动态分币 8-入金见点 9-入金管理',
    `asset_type`  TINYINT        NOT NULL DEFAULT 1       COMMENT '0-USDC 1-TIP',
    `batch_id`    VARCHAR(66)                             COMMENT '链上幂等批次ID',
    `business_id` BIGINT                                  COMMENT '关联业务ID',
    `reward_date` INT                                     COMMENT '发放日期 yyyyMMdd',
    `status`      INT            NOT NULL DEFAULT 0       COMMENT '0-PENDING 1-SENT 2-PAID 3-FAILED',
    `tx_hash`     VARCHAR(66),
    `sent_at`     BIGINT,
    `paid_at`     BIGINT,
    `expires_at`  BIGINT,
    `remark`      VARCHAR(256),
    `created_date`  BIGINT,
    `updated_date`  BIGINT,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_batch_id` (`batch_id`),
    KEY `idx_status_type` (`status`, `reward_type`),
    KEY `idx_business_id_type` (`business_id`, `reward_type`),
    KEY `idx_reward_date` (`reward_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用奖励记录';

-- ------------------------------------------------------------
-- LP挖矿奖励60天线性释放计划
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `lp_reward_release` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT         NOT NULL,
    `deposit_id`      BIGINT         NOT NULL             COMMENT '关联入金订单ID',
    `remove_tx_hash`  VARCHAR(66)    NOT NULL             COMMENT '移除LP的交易哈希（幂等键）',
    `total_amount`    DECIMAL(30,18) NOT NULL             COMMENT '60天总奖励金额（TIP）',
    `daily_amount`    DECIMAL(30,18) NOT NULL             COMMENT '每日释放金额',
    `release_date`    INT            NOT NULL             COMMENT '计划释放日期 yyyyMMdd',
    `released_amount` DECIMAL(30,18) NOT NULL DEFAULT 0,
    `status`          INT            NOT NULL DEFAULT 0   COMMENT '0-PENDING 1-RELEASED',
    `batch_id`        VARCHAR(66)                         COMMENT '释放时关联的reward_record.batch_id',
    `created_date`     BIGINT,
    `last_updated_date` BIGINT,
    `created_by`       BIGINT,
    `last_updated_by`  BIGINT,
    PRIMARY KEY (`id`),
    KEY `idx_release_date_status` (`release_date`, `status`),
    KEY `idx_remove_tx_hash` (`remove_tx_hash`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LP挖矿奖励60天线性释放计划';

-- ------------------------------------------------------------
-- 手续费二次分配记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fee_distribution_record` (
    `id`                BIGINT         NOT NULL AUTO_INCREMENT,
    `withdraw_tx_hash`  VARCHAR(66)    NOT NULL             COMMENT '触发来源：用户提现txHash（幂等键）',
    `batch_id`          VARCHAR(66)                         COMMENT '链上幂等批次ID',
    `fee_amount`        DECIMAL(30,6)  NOT NULL             COMMENT '手续费金额（USDC）',
    `status`            TINYINT        NOT NULL DEFAULT 0   COMMENT '0-PENDING 1-SENT 2-CONFIRMED',
    `distribute_tx_hash` VARCHAR(66)                        COMMENT '分配交易哈希',
    `created_date`      BIGINT,
    `updated_date`      BIGINT,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_withdraw_tx_hash` (`withdraw_tx_hash`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手续费二次分配记录';

-- ============================================================
-- 初始系统配置数据
-- ============================================================
INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `enabled`, `created_date`, `last_updated_date`) VALUES
('deposit.energy.multiplier',   '1',        '入金能量倍率（每1 USDC获得N点能量）',         1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('deposit.daily.max',           '1000000',  '每日全局最大入金额度（USDC）',                 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('deposit.allowed.roles',       '0,1,2,3,4','允许入金的角色列表（逗号分隔）',               1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('deposit.weight.growth.rate',  '0.001',    '入金权重每日增长率',                           1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('deposit.order.expiration',    '3600',     '入金订单过期时间（秒）',                       1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
('deposit.signature.ttl',       '300',      'EIP712签名有效期（秒）',                       1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

SET FOREIGN_KEY_CHECKS = 1;
