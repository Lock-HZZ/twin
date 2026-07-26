create table if not exists bamboo_check_point
(
    chain_id           bigint unsigned not null comment '链ID' primary key,
    block_height       bigint unsigned not null comment '区块高度',
    created_date       datetime(6)     not null comment '创建时间',
    last_modified_date datetime(6)     not null comment '上次修改时间'
) comment 'Bamboo检查点';

create table if not exists bamboo_event_log
(
    id                 bigint unsigned not null comment 'ID' primary key auto_increment,
    chain_id           bigint unsigned not null comment '链ID',
    address            varchar(64)     not null comment '地址',
    topics             text            not null comment '主题',
    data               text            not null comment '数据',
    block_number       bigint unsigned not null comment '区块号',
    transaction_hash   varchar(128)    not null comment '交易哈希',
    transaction_index  bigint unsigned not null comment '交易索引',
    block_hash         varchar(128)    not null comment '区块哈希',
    log_index          bigint unsigned not null comment '日志索引',
    removed            boolean         not null comment '已移除',
    created_date       datetime(6)     not null comment '创建时间',
    last_modified_date datetime(6)     not null comment '上次修改时间',
    processed          tinyint(1)      not null default 0 comment '业务是否已处理（0-未处理 1-已处理）',
    constraint unique (chain_id, address, transaction_hash, log_index),
    index idx_chain_processed (chain_id, processed)
) comment 'Bamboo事件日志';
