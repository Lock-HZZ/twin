package com.zmyc.service;

import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.entity.UserRelationClosureDO;
import com.zmyc.infrastructure.repository.UserDepositRepository;
import com.zmyc.infrastructure.repository.UserPerformanceRepository;
import com.zmyc.infrastructure.repository.UserRelationClosureRepository;
import com.zmyc.infrastructure.repository.UserRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRelationClosureRepository closureRepository;

    @Autowired
    private UserPerformanceRepository performanceRepository;

    @Autowired
    private UserDepositRepository depositRepository;


    /**
     * 注册新用户（公开方法，供其他服务调用）
     */
    @Transactional
    public UserDO registerUser(String address, String invitedCode, String clientIp) {
        UserDO newUser = new UserDO();
        newUser.setAddress(address);
        newUser.setEnabled((byte) 1);
        newUser.setRegistrationIp(clientIp);
        newUser.setLastLoginIp(clientIp);

        if (invitedCode != null && !invitedCode.isEmpty()) {
            UserDO inviter = userRepository.findByInvitedCode(invitedCode);
            // 建立闭包表关系
            closureRepository.insertForNewUser(newUser.getId(), inviter.getId());
        }

        String newInvitedCode = RandomStringUtils.randomAlphanumeric(8);
        newUser.setInvitedCode(newInvitedCode);

        userRepository.save(newUser);

        // 初始化业绩记录
        performanceRepository.initForNewUser(newUser.getId());

        return newUser;
    }

    /**
     * 统计有效推荐人数（直推中拥有进行中订单的用户）
     *
     * @param userId 用户ID
     * @return 有效推荐人数
     */
    public int countValidReferrals(Long userId) {
        var children = closureRepository.findDirectChildren(userId);
        int count = 0;
        for (UserRelationClosureDO child : children) {
            if (depositRepository.countActiveOrders(child.getDescendantId()) > 0) {
                count++;
            }
        }
        return count;
    }

}