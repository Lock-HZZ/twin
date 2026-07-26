-- ============================================================
-- 区块链配置表
-- ============================================================

CREATE TABLE IF NOT EXISTS `blockchain_chain` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `chain_id`          BIGINT          NOT NULL COMMENT '链ID(如1=以太坊主网,56=BSC)',
    `chain_name`        VARCHAR(64)     NOT NULL COMMENT '链名称(Ethereum/BSC/Polygon等)',
    `rpc_url`           VARCHAR(255)    NOT NULL COMMENT 'RPC节点地址',
    `explorer_url`      VARCHAR(255)    DEFAULT NULL COMMENT '区块浏览器地址',
    `native_symbol`     VARCHAR(16)     NOT NULL COMMENT '原生币符号(ETH/BNB/MATIC)',
    `enabled`           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chain_id` (`chain_id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区块链配置表';

-- ============================================================
-- 代币配置表
-- ============================================================

CREATE TABLE IF NOT EXISTS `blockchain_token` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `chain_id`          BIGINT          NOT NULL COMMENT '所属链ID',
    `contract_address`  VARCHAR(64)     NOT NULL COMMENT '合约地址(原生币则为0x0或空)',
    `symbol`            VARCHAR(32)     NOT NULL COMMENT '代币符号(USDT/USDC/BOT等)',
    `name`              VARCHAR(128)    DEFAULT NULL COMMENT '代币全称',
    `decimals`          INT             NOT NULL DEFAULT 18 COMMENT '精度位数',
    `is_native`         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否原生币: 0-否, 1-是',
    `usdt_rate`         DECIMAL(30,8)   NOT NULL DEFAULT 1.00000000 COMMENT '对USDT汇率(1 TOKEN = ? USDT)',
    `deposit_enabled`   TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否允许入金: 0-禁用, 1-启用',
    `enabled`           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    `created_date`      BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date` BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chain_contract` (`chain_id`, `contract_address`),
    KEY `idx_symbol` (`symbol`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代币配置表';
