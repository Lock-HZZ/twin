package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.UserMinerDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMinerMapper extends BaseMapper<UserMinerDO> {

    @Select("SELECT * FROM user_miner WHERE user_id = #{userId} AND status = 1 ORDER BY created_date DESC")
    List<UserMinerDO> findActiveByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_miner WHERE deposit_id = #{depositId}")
    List<UserMinerDO> findByDepositId(@Param("depositId") Long depositId);

    @Select("SELECT * FROM user_miner WHERE status = 1 AND expired_date <= #{now}")
    List<UserMinerDO> findMiningAndExpired(@Param("now") Long now);

    @Select("SELECT COUNT(*) FROM user_miner WHERE status = 1")
    int countActiveMining();

    @Select("SELECT user_id, COUNT(*) AS miner_count FROM user_miner " +
            "WHERE #{now} >= activated_date AND #{now} < expired_date " +
            "GROUP BY user_id")
    List<UserMinerCountDTO> countByUserIdForMining(@Param("now") Long now);

    class UserMinerCountDTO {
        public Long userId;
        public Integer minerCount;
    }
}
