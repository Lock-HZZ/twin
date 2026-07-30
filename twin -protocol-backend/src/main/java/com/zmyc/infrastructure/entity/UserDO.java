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

    /** 动态分币等级：0-无等级，1-D1(30人)，2-D2(120人)，...，8-D8(30000人) */
    private Integer level;

    /** 用户角色常量 */
    public static class Role {
        public static final int NORMAL = 0;   // 普通用户
        public static final int GOLD = 1;     // 黄金节点
        public static final int DIAMOND = 2;  // 钻石节点
        public static final int CROWN = 3;    // 皇冠节点
        public static final int PARTNER = 4;  // 合伙人
    }

    /** 动态分币等级常量 */
    public static class Level {
        public static final int NONE = 0;
        public static final int D1 = 1;  // 30-119人
        public static final int D2 = 2;  // 120-359人
        public static final int D3 = 3;  // 360-999人
        public static final int D4 = 4;  // 1000-3999人
        public static final int D5 = 5;  // 4000-9999人
        public static final int D6 = 6;  // 10000-14999人
        public static final int D7 = 7;  // 15000-29999人
        public static final int D8 = 8;  // 30000+人
    }
}