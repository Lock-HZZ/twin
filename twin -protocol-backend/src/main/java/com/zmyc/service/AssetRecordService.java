package com.zmyc.service;

import com.zmyc.application.vo.request.AssetRecordRequest;
import com.zmyc.application.vo.response.AssetRecordResponse;
import com.zmyc.application.vo.response.PageResponse;
import com.zmyc.infrastructure.dto.AssetRecordDTO;
import com.zmyc.infrastructure.mapper.AssetRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetRecordService {

    @Autowired
    private AssetRecordMapper assetRecordMapper;

    public PageResponse<AssetRecordResponse> queryRecords(Long userId, AssetRecordRequest request) {
        String assetType = request.getAssetType();
        if (assetType != null) {
            assetType = assetType.toUpperCase().strip();
            if (!List.of("ENERGY", "USDC", "TIP").contains(assetType)) {
                assetType = null;
            }
        }

        int pageSize = request.getPageSize();
        int offset = request.getOffset();

        long total = assetRecordMapper.countRecords(userId, assetType);
        List<AssetRecordDTO> rows = total > 0
                ? assetRecordMapper.queryRecords(userId, assetType, offset, pageSize)
                : List.of();

        List<AssetRecordResponse> list = rows.stream().map(r -> AssetRecordResponse.builder()
                .category(r.getCategory())
                .assetType(r.getAssetType())
                .amount(r.getAmount())
                .balanceBefore(r.getBalanceBefore())
                .balanceAfter(r.getBalanceAfter())
                .remark(r.getRemark())
                .relatedId(r.getRelatedId())
                .createdDate(r.getCreatedDate())
                .build()).toList();

        return new PageResponse<>(list, total, request.getPage(), pageSize);
    }
}
