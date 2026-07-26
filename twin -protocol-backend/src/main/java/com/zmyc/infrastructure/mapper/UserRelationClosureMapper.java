package com.zmyc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmyc.infrastructure.dto.AncestorLineDTO;
import com.zmyc.infrastructure.entity.UserRelationClosureDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserRelationClosureMapper extends BaseMapper<UserRelationClosureDO> {

    /** 插入直推关系（depth=1） */
    @Insert("INSERT INTO user_relation_closure (ancestor_id, descendant_id, depth, created_date) " +
            "VALUES (#{parentId}, #{newUserId}, 1, #{createdDate})")
    int insertDirectRelation(@Param("parentId") Long parentId,
                             @Param("newUserId") Long newUserId,
                             @Param("createdDate") Long createdDate);

    /** 从父节点的祖先行继承 depth>=2 的关系 */
    @Insert("INSERT INTO user_relation_closure (ancestor_id, descendant_id, depth, created_date) " +
            "SELECT c.ancestor_id, #{newUserId}, c.depth + 1, #{createdDate} " +
            "FROM user_relation_closure c " +
            "WHERE c.descendant_id = #{parentId}")
    int insertFromAncestors(@Param("newUserId") Long newUserId,
                            @Param("parentId") Long parentId,
                            @Param("createdDate") Long createdDate);

    /** 查询某用户的所有祖先ID，由近到远排序（入金时逐层累加团队业绩用） */
    @Select("SELECT ancestor_id " +
            "FROM user_relation_closure " +
            "WHERE descendant_id = #{userId} " +
            "ORDER BY depth ASC")
    List<Long> findAncestorIds(@Param("userId") Long userId);
}
