package com.zmyc.listener.indexer;

import com.zmyc.bamboo.core.dao.EventLogDao;
import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.listener.indexer.processor.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventLogProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogProcessingService.class);

    private final EventLogDao eventLogDao;

    public EventLogProcessingService(EventLogDao eventLogDao) {
        this.eventLogDao = eventLogDao;
    }

    /**
     * 先原子抢占（markProcessed WHERE processed=0），再执行业务逻辑，全在同一事务内：
     * - markProcessed 返回 0：已被其他线程/重试处理过，直接跳过（幂等保护）
     * - markProcessed 返回 1：抢占成功，执行业务逻辑
     * - 业务逻辑抛异常：事务回滚，markProcessed 撤销，processed 回到 0，下次重试继续生效
     */
    @Transactional(rollbackFor = Exception.class)
    public void processAndMark(EventProcessor<?> processor, EventLog eventLog, TransactionInfo transactionInfo) {
        int affected = eventLogDao.markProcessed(
                eventLog.getChainId(),
                eventLog.getAddress(),
                eventLog.getTransactionHash(),
                eventLog.getLogIndex()
        );
        if (affected == 0) {
            LOGGER.info("bamboo :: event already processed, skipping. txHash={}", eventLog.getTransactionHash());
            return;
        }
        Object model = processor.getModel(eventLog, transactionInfo);
        processor.doBusinessLogic(model);
    }
}
