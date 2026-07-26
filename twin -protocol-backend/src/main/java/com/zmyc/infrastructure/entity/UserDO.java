package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName("users")
@Data
public class UserDO extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String address;

    private String invitedCode;

    private String email;

    private Byte enabled;

    private String registrationIp;

    private String lastLoginIp;

    /** 用户角色：0-普通用户，1-黄金节点，2-钻石节点，3-皇冠节点，4-合伙人 */
    private Integer role;

    /** 用户角色常量 */
    public static class Role {
        public static final int NORMAL = 0;   // 普通用户
        public static final int GOLD = 1;     // 黄金节点
        public static final int DIAMOND = 2;  // 钻石节点
        public static final int CROWN = 3;    // 皇冠节点
        public static final int PARTNER = 4;  // 合伙人
    }
}