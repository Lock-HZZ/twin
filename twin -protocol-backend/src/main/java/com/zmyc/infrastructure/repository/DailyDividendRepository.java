package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zmyc.infrastructure.entity.DailyDividendDO;
import com.zmyc.infrastructure.mapper.DailyDividendMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class DailyDividendRepository {

    @Autowired
    private DailyDividendMapper dividendMapper;

    /** 根据日期查询分红记录 */
    public DailyDividendDO findByDate(LocalDate dividendDate) {
        LambdaUpdateWrapper<DailyDividendDO> queryWrapper = new LambdaUpdateWrapper<>();
        queryWrapper.eq(DailyDividendDO::getDividendDate, dividendDate);
        return dividendMapper.selectOne(queryWrapper);
    }

    /** 保存分红记录 */
    public void save(DailyDividendDO dividend) {
        if (dividend.getId() == null) {
            dividendMapper.insert(dividend);
        } else {
            dividendMapper.updateById(dividend);
        }
    }
}
