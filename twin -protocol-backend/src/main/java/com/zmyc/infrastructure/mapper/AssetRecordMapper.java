package com.zmyc.infrastructure.mapper;

import com.zmyc.infrastructure.dto.AssetRecordDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产明细统一查询 Mapper
 * 用 UNION ALL 合并能量、USDC、TIP 三类流水，统一分页排序
 *
 * category 枚举值：
 *   ENERGY_EARN      能量值-入金获得
 *   ENERGY_CONSUME   能量值-动态奖励消耗
 *   USDC_REWARD      USDC奖励（见点/管理奖励等）
 *   USDC_DEPOSIT     USDC入金
 *   TIP_STAKE        TIP质押（正=质押，负=赎回）
 *   TIP_STAKE_DIVIDEND TIP质押分红
 */
@Mapper
public interface AssetRecordMapper {

    String ENERGY_SQL =
            "SELECT " +
            "  CASE et.transaction_type " +
            "    WHEN 1 THEN 'ENERGY_EARN' " +
            "    WHEN 2 THEN 'ENERGY_CONSUME' " +
            "    ELSE 'ENERGY_OTHER' END AS category, " +
            "  'ENERGY' AS asset_type, " +
            "  CASE et.transaction_type WHEN 2 THEN -et.amount ELSE et.amount END AS amount, " +
            "  et.balance_before, " +
            "  et.balance_after, " +
            "  et.remark, " +
            "  et.related_id, " +
            "  et.created_date " +
            "FROM energy_transaction et " +
            "WHERE et.user_id = #{userId}";

    String USDC_REWARD_SQL =
            "SELECT " +
            "  'USDC_REWARD' AS category, " +
            "  'USDC' AS asset_type, " +
            "  rr.amount, " +
            "  NULL AS balance_before, " +
            "  NULL AS balance_after, " +
            "  rr.remark, " +
            "  rr.id AS related_id, " +
            "  rr.created_date " +
            "FROM reward_record rr " +
            "WHERE rr.user_id = #{userId} AND rr.asset_type = 0 AND rr.status IN (1, 2)";

    String USDC_DEPOSIT_SQL =
            "SELECT " +
            "  'USDC_DEPOSIT' AS category, " +
            "  'USDC' AS asset_type, " +
            "  ud.amount, " +
            "  NULL AS balance_before, " +
            "  NULL AS balance_after, " +
            "  '入金' AS remark, " +
            "  ud.id AS related_id, " +
            "  ud.created_date " +
            "FROM user_deposit ud " +
            "WHERE ud.user_id = #{userId} AND ud.status = 1";

    String TIP_STAKE_SQL =
            "SELECT " +
            "  'TIP_STAKE' AS category, " +
            "  'TIP' AS asset_type, " +
            "  CASE us.status WHEN 1 THEN -us.amount ELSE us.amount END AS amount, " +
            "  NULL AS balance_before, " +
            "  NULL AS balance_after, " +
            "  CASE us.status WHEN 1 THEN '赎回质押' ELSE '质押TIP' END AS remark, " +
            "  us.id AS related_id, " +
            "  us.created_date " +
            "FROM user_stake us " +
            "WHERE us.user_id = #{userId}";

    String TIP_DIVIDEND_SQL =
            "SELECT " +
            "  'TIP_STAKE_DIVIDEND' AS category, " +
            "  'TIP' AS asset_type, " +
            "  sdr.amount, " +
            "  NULL AS balance_before, " +
            "  NULL AS balance_after, " +
            "  '质押分红' AS remark, " +
            "  sdr.id AS related_id, " +
            "  sdr.created_date " +
            "FROM stake_dividend_record sdr " +
            "WHERE sdr.user_id = #{userId} AND sdr.status IN (1, 2)";

    String TIP_REWARD_SQL =
            "SELECT " +
            "  'TIP_REWARD' AS category, " +
            "  'TIP' AS asset_type, " +
            "  rr.amount, " +
            "  NULL AS balance_before, " +
            "  NULL AS balance_after, " +
            "  rr.remark, " +
            "  rr.id AS related_id, " +
            "  rr.created_date " +
            "FROM reward_record rr " +
            "WHERE rr.user_id = #{userId} AND rr.asset_type = 1 AND rr.status IN (1, 2)";

    @Select("<script>" +
            "SELECT * FROM (" +
            "  <if test=\"assetType == null or assetType == 'ENERGY'\">" + ENERGY_SQL + " UNION ALL " + "</if>" +
            "  <if test=\"assetType == null or assetType == 'USDC'\">" + USDC_REWARD_SQL + " UNION ALL " + USDC_DEPOSIT_SQL + " UNION ALL " + "</if>" +
            "  <if test=\"assetType == null or assetType == 'TIP'\">" + TIP_STAKE_SQL + " UNION ALL " + TIP_DIVIDEND_SQL + " UNION ALL " + TIP_REWARD_SQL + " UNION ALL " + "</if>" +
            "  SELECT NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL WHERE 1=0" +
            ") t " +
            "WHERE t.category IS NOT NULL " +
            "ORDER BY t.created_date DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<AssetRecordDTO> queryRecords(@Param("userId") Long userId,
                                      @Param("assetType") String assetType,
                                      @Param("offset") int offset,
                                      @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM (" +
            "  <if test=\"assetType == null or assetType == 'ENERGY'\">" + ENERGY_SQL + " UNION ALL " + "</if>" +
            "  <if test=\"assetType == null or assetType == 'USDC'\">" + USDC_REWARD_SQL + " UNION ALL " + USDC_DEPOSIT_SQL + " UNION ALL " + "</if>" +
            "  <if test=\"assetType == null or assetType == 'TIP'\">" + TIP_STAKE_SQL + " UNION ALL " + TIP_DIVIDEND_SQL + " UNION ALL " + TIP_REWARD_SQL + " UNION ALL " + "</if>" +
            "  SELECT NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL WHERE 1=0" +
            ") t " +
            "WHERE t.category IS NOT NULL" +
            "</script>")
    long countRecords(@Param("userId") Long userId,
                      @Param("assetType") String assetType);
}
