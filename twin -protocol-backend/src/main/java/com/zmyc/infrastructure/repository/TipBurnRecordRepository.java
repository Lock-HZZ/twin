package com.zmyc.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmyc.infrastructure.entity.TipBurnRecordDO;
import com.zmyc.infrastructure.mapper.TipBurnRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TipBurnRecordRepository {

    @Autowired
    private TipBurnRecordMapper burnRecordMapper;

    /** 根据燃烧日期查询记录 */
    public TipBurnRecordDO findByBurnDate(Long burnDate) {
        LambdaQueryWrapper<TipBurnRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TipBurnRecordDO::getBurnDate, burnDate);
        return burnRecordMapper.selectOne(wrapper);
    }

    /** 查询所有待处理或执行中的记录 */
    public List<TipBurnRecordDO> findPendingOrProcessing() {
        LambdaQueryWrapper<TipBurnRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TipBurnRecordDO::getStatus,
                  TipBurnRecordDO.Status.PENDING,
                  TipBurnRecordDO.Status.PROCESSING);
        return burnRecordMapper.selectList(wrapper);
    }

    /** 保存燃烧记录 */
    public void save(TipBurnRecordDO record) {
        if (record.getId() == null) {
            burnRecordMapper.insert(record);
        } else {
            burnRecordMapper.updateById(record);
        }
    }

    /** 根据ID查询 */
    public TipBurnRecordDO findById(Long id) {
        return burnRecordMapper.selectById(id);
    }

    /** 根据交易哈希查询 */
    public TipBurnRecordDO findByTxHash(String txHash) {
        LambdaQueryWrapper<TipBurnRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TipBurnRecordDO::getTxHash, txHash);
        return burnRecordMapper.selectOne(wrapper);
    }

    /** 查询最近一次成功的燃烧记录 */
    public TipBurnRecordDO findLastSuccess() {
        LambdaQueryWrapper<TipBurnRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TipBurnRecordDO::getStatus, TipBurnRecordDO.Status.SUCCESS)
               .orderByDesc(TipBurnRecordDO::getBurnDate)
               .last("LIMIT 1");
        return burnRecordMapper.selectOne(wrapper);
    }

    /** 统计从初始日期到现在的天数 */
    public int countDaysSinceStart(Long startDate) {
        LambdaQueryWrapper<TipBurnRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TipBurnRecordDO::getStatus, TipBurnRecordDO.Status.SUCCESS)
               .ge(TipBurnRecordDO::getBurnDate, startDate);
        return burnRecordMapper.selectCount(wrapper).intValue();
    }
}
