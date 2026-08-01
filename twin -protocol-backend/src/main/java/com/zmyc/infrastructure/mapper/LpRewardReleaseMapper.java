package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.LpRewardReleaseDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface LpRewardReleaseMapper extends BaseMapper<LpRewardReleaseDO> {

    @Update("<script>" +
            "UPDATE lp_reward_release " +
            "SET status = #{status}, batch_id = #{batchId}, released_amount = daily_amount, last_updated_by = #{now} " +
            "WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>"
    )
    void updateStatusByIds(List<Long> ids, Integer status, String batchId, long now);

}
