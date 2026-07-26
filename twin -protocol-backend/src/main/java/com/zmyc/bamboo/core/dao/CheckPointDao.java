package com.zmyc.bamboo.core.dao;

import com.zmyc.bamboo.core.model.CheckPoint;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

public class CheckPointDao {

    // JdbcTemplate 内部通过 DataSourceUtils.getConnection() 获取连接，
    // 当外层有 @Transactional 时会自动绑定到当前事务，无需手动传递 Connection
    private final JdbcTemplate jdbcTemplate;

    public CheckPointDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void create(BigInteger chainId, BigInteger blockHeight) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update("insert into bamboo_check_point values (?, ?, ?, ?)",
                chainId, blockHeight, now, now);
    }

    public CheckPoint get(BigInteger chainId) {
        List<CheckPoint> list = jdbcTemplate.query(
                "select * from bamboo_check_point where chain_id=?",
                (rs, rowNum) -> new CheckPoint(
                        BigInteger.valueOf(rs.getLong("chain_id")),
                        BigInteger.valueOf(rs.getLong("block_height")),
                        rs.getObject("created_date", OffsetDateTime.class),
                        rs.getObject("last_modified_date", OffsetDateTime.class)),
                chainId);
        return list.isEmpty() ? null : list.get(0);
    }

    public void modify(BigInteger chainId, BigInteger blockHeight) {
        jdbcTemplate.update(
                "update bamboo_check_point set block_height=?, last_modified_date=? where chain_id=?",
                blockHeight, OffsetDateTime.now(), chainId);
    }
}
