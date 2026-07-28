package com.zmyc.service;

import com.zmyc.infrastructure.entity.DailyDividendDO;
import com.zmyc.infrastructure.repository.DailyDividendRepository;
import com.zmyc.infrastructure.repository.UserDividendDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Slf4j
public class DividendService {

    @Autowired
    private DailyDividendRepository dividendRepository;

    @Autowired
    private UserDividendDetailRepository detailRepository;

    /**
     * 每日分红任务，每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")//TODO
    @Transactional
    public void dailyDividendTask() {
        LocalDate today = LocalDate.now();
        log.info("开始执行每日分红任务，分红日期: {}", today);
        try {
            executeDailyDividend(today);
            log.info("每日分红任务执行成功，分红日期: {}", today);
        } catch (Exception e) {
            log.error("每日分红任务执行失败，分红日期: {}", today, e);
            throw e;
        }
    }

    /**
     * 执行每日分红
     */
    @Transactional
    public void executeDailyDividend(LocalDate dividendDate) {
        // 检查是否已执行过
        DailyDividendDO existingDividend = dividendRepository.findByDate(dividendDate);
        if (existingDividend != null) {
            log.error("分红记录已存在，跳过执行: date={}", dividendDate);
            return;
        }


        // 获取每日分红 TIP 总量
        BigDecimal totalTipAmount = getDailyDividendTipAmount();

    }


    private BigDecimal getDailyDividendTipAmount() {
        return BigDecimal.valueOf(10000.0); //TODO: 这里可以从系统配置中获取每日分红 TIP 总量，暂时写死为10000
    }
}
