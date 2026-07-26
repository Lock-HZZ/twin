package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmyc.infrastructure.entity.UserRelationClosureDO;
import com.zmyc.infrastructure.mapper.UserRelationClosureMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRelationClosureRepository {

    @Autowired
    private UserRelationClosureMapper closureMapper;

    /**
     * 新用户注册时建立闭包关系：
     *   depth=1  直推关系，显式插入
     *   depth>=2 从父节点已有祖先行继承
     *   根用户（parentId=null）不插任何行
     */
    public void insertForNewUser(Long newUserId, Long parentId) {
        if (parentId == null) {
            return;
        }
        long now = System.currentTimeMillis() / 1000;
        closureMapper.insertDirectRelation(parentId, newUserId, now);
        closureMapper.insertFromAncestors(newUserId, parentId, now);
    }

    /** 查询某用户所有祖先ID，由近到远 */
    public List<Long> findAncestorIds(Long userId) {
        return closureMapper.findAncestorIds(userId);
    }

    /** 查询某用户的直接下级 */
    public List<UserRelationClosureDO> findDirectChildren(Long userId) {
        LambdaQueryWrapper<UserRelationClosureDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRelationClosureDO::getAncestorId, userId)
               .eq(UserRelationClosureDO::getDepth, 1);
        return closureMapper.selectList(wrapper);
    }

    /** 统计某用户的团队总人数 */
    public long countDescendants(Long userId) {
        LambdaQueryWrapper<UserRelationClosureDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRelationClosureDO::getAncestorId, userId);
        return closureMapper.selectCount(wrapper);
    }
}
