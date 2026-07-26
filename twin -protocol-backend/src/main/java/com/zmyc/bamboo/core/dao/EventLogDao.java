package com.zmyc.bamboo.core.dao;

import com.zmyc.bamboo.core.model.EventLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

public class EventLogDao {

    // JdbcTemplate 内部通过 DataSourceUtils.getConnection() 获取连接，
    // 当外层有 @Transactional 时会自动绑定到当前事务，无需手动传递 Connection
    private final JdbcTemplate jdbcTemplate;

    public EventLogDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void writeAll(List<EventLog> logs) {
        if (logs == null || logs.isEmpty()) return;
        String sql = "insert ignore into bamboo_event_log " +
                     "values (null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, logs, logs.size(), (ps, log) -> {
            OffsetDateTime now = OffsetDateTime.now();
            ps.setObject(1,  log.getChainId());
            ps.setObject(2,  log.getAddress());
            ps.setObject(3,  log.getTopics());
            ps.setObject(4,  log.getData());
            ps.setObject(5,  log.getBlockNumber());
            ps.setObject(6,  log.getTransactionHash());
            ps.setObject(7,  log.getTransactionIndex());
            ps.setObject(8,  log.getBlockHash());
            ps.setObject(9,  log.getLogIndex());
            ps.setObject(10, log.getRemoved());
            ps.setObject(11, now);
            ps.setObject(12, now);
            ps.setInt(13, 0);
        });
    }

    public boolean exists(BigInteger chainId, String address, String transactionHash, BigInteger logIndex) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from bamboo_event_log where chain_id=? and address=? and transaction_hash=? and log_index=?",
                Integer.class, chainId, address, transactionHash, logIndex);
        return count != null && count > 0;
    }

    /**
     * 原子地将 processed 从 0 更新为 1（WHERE processed=0）。
     * 返回受影响行数：1 表示抢占成功，0 表示已被处理（幂等保护）。
     */
    public int markProcessed(BigInteger chainId, String address, String transactionHash, BigInteger logIndex) {
        return jdbcTemplate.update(
                "update bamboo_event_log set processed=1, last_modified_date=? " +
                "where chain_id=? and address=? and transaction_hash=? and log_index=? and processed=0",
                OffsetDateTime.now(), chainId, address, transactionHash, logIndex);
    }

    public List<EventLog> findUnprocessed() {
        String sql = "select id, chain_id, address, topics, data, block_number, transaction_hash, " +
                     "transaction_index, block_hash, log_index, removed, processed, " +
                     "created_date, last_modified_date " +
                     "from bamboo_event_log where processed=0 order by id asc";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public List<EventLog> findStaleUnprocessed(int olderThanSeconds) {
        String sql = "select id, chain_id, address, topics, data, block_number, transaction_hash, " +
                     "transaction_index, block_hash, log_index, removed, processed, " +
                     "created_date, last_modified_date " +
                     "from bamboo_event_log " +
                     "where processed=0 and created_date <= ? " +
                     "order by id asc";
        OffsetDateTime threshold = OffsetDateTime.now().minusSeconds(olderThanSeconds);
        return jdbcTemplate.query(sql, ROW_MAPPER, threshold);
    }

    private static final RowMapper<EventLog> ROW_MAPPER = (rs, rowNum) -> {
        EventLog log = new EventLog();
        log.setId(rs.getBigDecimal("id").toBigInteger());
        log.setChainId(rs.getBigDecimal("chain_id").toBigInteger());
        log.setAddress(rs.getString("address"));
        log.setTopics(rs.getString("topics"));
        log.setData(rs.getString("data"));
        log.setBlockNumber(rs.getBigDecimal("block_number").toBigInteger());
        log.setTransactionHash(rs.getString("transaction_hash"));
        log.setTransactionIndex(rs.getBigDecimal("transaction_index").toBigInteger());
        log.setBlockHash(rs.getString("block_hash"));
        log.setLogIndex(rs.getBigDecimal("log_index").toBigInteger());
        log.setRemoved(rs.getBoolean("removed"));
        log.setProcessed(rs.getBoolean("processed"));
        log.setCreatedDate(rs.getObject("created_date", OffsetDateTime.class));
        log.setLastModifiedDate(rs.getObject("last_modified_date", OffsetDateTime.class));
        return log;
    };
}
