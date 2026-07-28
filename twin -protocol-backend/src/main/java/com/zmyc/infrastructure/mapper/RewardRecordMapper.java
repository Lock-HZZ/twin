package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.entity.RewardRecordDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RewardRecordMapper extends BaseMapper<RewardRecordDO> {

    @Insert("<script>" +
            "INSERT INTO reward_record " +
            "(user_id, amount, reward_type, asset_type, batch_id, business_id, reward_date, status, expires_at, remark, created_date, updated_date) VALUES " +
            "<foreach collection='list' item='r' separator=','>" +
            "(#{r.userId}, #{r.amount}, #{r.rewardType}, #{r.assetType}, #{r.batchId}, #{r.businessId}, #{r.rewardDate}, #{r.status}, #{r.expiresAt}, #{r.remark}, #{r.createdDate}, #{r.updatedDate})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<RewardRecordDO> list);

    @Update("UPDATE reward_record SET batch_id=#{batchId}, status=#{status}, sent_at=#{sentAt}, updated_date=#{updatedDate} " +
            "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>")
    int updateBatchIdAndStatus(@Param("batchId") String batchId,
                                @Param("status") int status,
                                @Param("sentAt") long sentAt,
                                @Param("ids") List<Long> ids,
                                @Param("updatedDate") long updatedDate);

    @Update("UPDATE reward_record SET status=#{status}, tx_hash=#{txHash}, paid_at=#{paidAt}, updated_date=#{updatedDate} " +
            "WHERE batch_id=#{batchId}")
    int updateStatusByBatchId(@Param("batchId") String batchId,
                               @Param("status") int status,
                               @Param("txHash") String txHash,
                               @Param("paidAt") Long paidAt,
                               @Param("updatedDate") long updatedDate);
}
