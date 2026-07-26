package com.zmyc.infrastructure.dto;

import lombok.Data;

@Data
public class AncestorLineDTO {

    private Long ancestorId;

    /** 该后代属于此祖先的哪条直推线头 */
    private Long lineHeadId;

    private Integer depth;
}
