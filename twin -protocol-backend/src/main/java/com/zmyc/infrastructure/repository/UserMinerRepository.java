package com.zmyc.infrastructure.repository;

import com.zmyc.infrastructure.entity.UserMinerDO;
import com.zmyc.infrastructure.mapper.UserMinerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserMinerRepository {

    @Autowired
    private UserMinerMapper minerMapper;

    /** 查询所有挖矿中且已到期的矿机 */
    public List<UserMinerDO> findMiningAndExpired(Long now) {
        return minerMapper.findMiningAndExpired(now);
    }

    /** 查询用户所有激活的矿机 */
    public List<UserMinerDO> findActiveByUserId(Long userId) {
        return minerMapper.findActiveByUserId(userId);
    }

    /** 根据入金ID查询矿机 */
    public List<UserMinerDO> findByDepositId(Long depositId) {
        return minerMapper.findByDepositId(depositId);
    }

    /** 保存矿机 */
    public void save(UserMinerDO miner) {
        if (miner.getId() == null) {
            minerMapper.insert(miner);
        } else {
            minerMapper.updateById(miner);
        }
    }

    /** 根据ID查询 */
    public UserMinerDO findById(Long id) {
        return minerMapper.selectById(id);
    }
}
