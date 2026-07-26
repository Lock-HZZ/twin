package com.zmyc.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_relation_closure")
public class UserRelationClosureDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ancestorId;

    private Long descendantId;

    private Integer depth;

    private Long createdDate;
}
