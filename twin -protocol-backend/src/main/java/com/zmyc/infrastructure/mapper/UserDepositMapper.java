package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.UserDepositDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface UserDepositMapper extends BaseMapper<UserDepositDO> {

    @Select("SELECT * FROM user_deposit WHERE tx_hash = #{txHash}")
    UserDepositDO findByTxHash(@Param("txHash") String txHash);

    /** 统计团队（所有层级下级）的入金总额 */
    @Select("SELECT COALESCE(SUM(d.amount), 0) " +
            "FROM user_relation_closure c " +
            "INNER JOIN user_deposit d ON d.user_id = c.descendant_id AND d.status = 1 " +
            "WHERE c.ancestor_id = #{userId}")
    BigDecimal sumTeamDepositAmount(@Param("userId") Long userId);

    /** 判断用户是否有效（有至少一笔COMPLETED入金） */
    @Select("SELECT COUNT(*) FROM user_deposit WHERE user_id = #{userId} AND status = 1 LIMIT 1")
    int countCompletedDeposits(@Param("userId") Long userId);
}
