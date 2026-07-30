package com.zmyc.application.vo.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ShareInfoResponse {

    /** 钱包地址 */
    private String address;

    /** 节点身份：0-普通，1-黄金，2-钻石，3-皇冠，4-合伙人 */
    private Integer role;

    /** 节点身份名称 */
    private String roleName;

    /** 动态分币等级：0-无，1-D1 ... 8-D8 */
    private Integer dLevel;

    /** S等级名称：无/S1-S7 */
    private String sLevel;

    /** 自己是否有效用户（有COMPLETED入金） */
    private Boolean isValid;

    /** 累计直推用户数（所有直推，不论是否有效） */
    private Long totalDirectCount;

    /** 累计直推有效用户数（直推中有入金的） */
    private Long validDirectCount;

    /** 团队总人数（所有层级下级） */
    private Long totalTeamCount;

    /** 团队有效用户数（有入金的下级） */
    private Long validTeamCount;

    /** 直推用户入金总额（USDC） */
    private BigDecimal directDepositAmount;

    /** 团队入金总额（USDC，不含自身） */
    private BigDecimal teamDepositAmount;

    /** 小区业绩（USDC） */
    private BigDecimal communityDepositAmount;
}
