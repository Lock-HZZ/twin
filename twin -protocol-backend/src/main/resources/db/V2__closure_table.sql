-- ============================================================
-- 无限层级闭包表
-- ============================================================

CREATE TABLE IF NOT EXISTS `user_relation_closure` (
    `id`            BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ancestor_id`   BIGINT  NOT NULL COMMENT '祖先用户ID',
    `descendant_id` BIGINT  NOT NULL COMMENT '后代用户ID',
    `depth`         INT     NOT NULL COMMENT '层级距离: 1=直推, n=第n代',
    `created_date`  BIGINT  DEFAULT NULL COMMENT '创建时间(10位秒级时间戳)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_anc_desc`  (`ancestor_id`, `descendant_id`),
    KEY `idx_desc_depth`      (`descendant_id`, `depth`),
    KEY `idx_anc_depth`       (`ancestor_id`,   `depth`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户邀请关系闭包表';

-- ============================================================
-- 用户业绩表（独立于 users 表）
-- ============================================================

CREATE TABLE IF NOT EXISTS `user_performance` (
    `user_id`               BIGINT          NOT NULL COMMENT '用户ID（主键）',
    `personal_volume_usdt`  DECIMAL(30,8)   NOT NULL DEFAULT 0 COMMENT '个人业绩(USDT)',
    `team_volume_usdt`      DECIMAL(30,8)   NOT NULL DEFAULT 0 COMMENT '团队业绩(USDT)',
    `community_volume_usdt` DECIMAL(30,8)   NOT NULL DEFAULT 0 COMMENT '小区业绩缓存(USDT) = 团队 - 个人 - 大区',
    `created_date`          BIGINT          DEFAULT NULL COMMENT '创建时间',
    `last_updated_date`     BIGINT          DEFAULT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`user_id`),
    KEY `idx_team_volume`   (`team_volume_usdt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户业绩表';
