package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.UserBalanceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserBalanceMapper extends BaseMapper<UserBalanceDO> {

    @Select("SELECT * FROM user_balance WHERE user_id = #{userId} AND token_id = #{tokenId} FOR UPDATE")
    UserBalanceDO findForUpdate(@Param("userId") Long userId, @Param("tokenId") Long tokenId);

    @Select("SELECT * FROM user_balance WHERE user_id = #{userId} AND token_id = #{tokenId}")
    UserBalanceDO findByUserToken(@Param("userId") Long userId, @Param("tokenId") Long tokenId);

    /** 查询多个用户在同一代币下的余额（分红前读 before） */
    @Select("<script>SELECT * FROM user_balance WHERE token_id = #{tokenId} AND user_id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<UserBalanceDO> findByTokenIdAndUserIds(@Param("tokenId") Long tokenId,
                                                @Param("userIds") List<Long> userIds);

    /** 原子性扣减余额（WHERE balance >= amount 防止超扣） */
    @Update("UPDATE user_balance SET balance = balance - #{amount}, version = version + 1, last_updated_date = #{now} " +
            "WHERE user_id = #{userId} AND token_id = #{tokenId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId,
                      @Param("tokenId") Long tokenId,
                      @Param("amount") BigDecimal amount,
                      @Param("now") Long now);

    /** INSERT ON DUPLICATE KEY UPDATE 增加余额（入金初始化场景） */
    @Update("INSERT INTO user_balance (user_id, token_id, balance, frozen_balance, version, created_date, last_updated_date) " +
            "VALUES (#{userId}, #{tokenId}, #{amount}, 0, 1, #{now}, #{now}) " +
            "ON DUPLICATE KEY UPDATE balance = balance + #{amount}, version = version + 1, last_updated_date = #{now}")
    int addBalanceOrCreate(@Param("userId") Long userId,
                           @Param("tokenId") Long tokenId,
                           @Param("amount") BigDecimal amount,
                           @Param("now") Long now);

    /** 批量 CASE WHEN UPDATE 增加余额（分红场景，余额行已存在） */
    void batchAddBalance(@Param("list") List<UserBalanceDO> list,
                         @Param("tokenId") Long tokenId,
                         @Param("now") Long now);
}
