package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.UserPerformanceDO;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface UserPerformanceMapper extends BaseMapper<UserPerformanceDO> {

    /** 入金时累加个人业绩（仅自身） */
    @Update("UPDATE user_performance " +
            "SET personal_volume_usdt = personal_volume_usdt + #{amount}, " +
            "    last_updated_date    = #{now} " +
            "WHERE user_id = #{userId}")
    int addPersonalVolume(@Param("userId") Long userId,
                          @Param("amount") BigDecimal amount,
                          @Param("now") Long now);

    /** 批量累加祖先的团队业绩（团队 = 纯下级，不含自身） */
    @Update("UPDATE user_performance p " +
            "JOIN user_relation_closure c ON c.ancestor_id = p.user_id " +
            "SET p.team_volume_usdt  = p.team_volume_usdt + #{amount}, " +
            "    p.last_updated_date = #{now} " +
            "WHERE c.descendant_id = #{userId}")
    int batchAddTeamVolume(@Param("userId") Long userId,
                           @Param("amount") BigDecimal amount,
                           @Param("now") Long now);

    /**
     * 批量重算小区业绩：对受影响的祖先，SUM(直推线) - MAX(直推线) = 小区
     */
    @Update("UPDATE user_performance p " +
            "JOIN ( " +
            "    SELECT c.ancestor_id, " +
            "           COALESCE(SUM(line.team_volume_usdt) - MAX(line.team_volume_usdt), 0) AS community " +
            "    FROM user_relation_closure c " +
            "    JOIN user_performance line ON line.user_id = c.descendant_id " +
            "    WHERE c.depth = 1 AND c.ancestor_id IN " +
            "          (SELECT ancestor_id FROM user_relation_closure WHERE descendant_id = #{userId}) " +
            "    GROUP BY c.ancestor_id " +
            ") calc ON calc.ancestor_id = p.user_id " +
            "SET p.community_volume_usdt = calc.community, " +
            "    p.last_updated_date = #{now}")
    int batchUpdateCommunityVolume(@Param("userId") Long userId,
                                   @Param("now") Long now);

    /**
     * 查询某用户所有直推子节点的团队业绩，降序排列。
     * 第一行即大区，其余求和即小区。
     */
    @Select("SELECT p.team_volume_usdt " +
            "FROM user_relation_closure c " +
            "JOIN user_performance p ON p.user_id = c.descendant_id " +
            "WHERE c.ancestor_id = #{userId} AND c.depth = 1 " +
            "ORDER BY p.team_volume_usdt DESC")
    List<BigDecimal> findDirectLineVolumes(@Param("userId") Long userId);
}
