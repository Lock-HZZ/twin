package com.zmyc.infrastructure.repository;

import com.zmyc.common.util.BatchUtils;
import com.zmyc.infrastructure.entity.UserDividendDetailDO;
import com.zmyc.infrastructure.mapper.UserDividendDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDividendDetailRepository {

    @Autowired
    private UserDividendDetailMapper detailMapper;

    /** 分批批量保存分红明细 */
    public void batchSave(List<UserDividendDetailDO> details) {
        BatchUtils.execute(details, detailMapper::batchInsert);
    }
}
