package com.zmyc.application.vo.request;

import lombok.Data;

@Data
public class AssetRecordRequest {

    /** 资产类型筛选：ENERGY / USDC / TIP，为空查全部 */
    private String assetType;

    private Integer page = 1;

    private Integer pageSize = 10;

    public int getOffset() {
        return (Math.max(page, 1) - 1) * pageSize;
    }
}
