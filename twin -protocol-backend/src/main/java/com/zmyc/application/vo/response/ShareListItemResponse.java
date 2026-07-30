package com.zmyc.application.vo.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ShareListItemResponse {

    private String address;

    /** 注册时间（秒级时间戳） */
    private Long createdDate;

    /** 是否有效用户（有COMPLETED入金） */
    private Boolean isValid;

    /** 节点身份：0-普通，1-黄金，2-钻石，3-皇冠，4-合伙人 */
    private Integer role;

    private String roleName;

    /** 动态分币等级 0-8 */
    private Integer dLevel;

    /** S等级：无/S1-S7 */
    private String sLevel;

    /** 团队总人数 */
    private Long totalTeamCount;

    /** 团队有效人数 */
    private Long validTeamCount;

    /** 团队业绩（USDC） */
    private BigDecimal teamDepositAmount;
}
