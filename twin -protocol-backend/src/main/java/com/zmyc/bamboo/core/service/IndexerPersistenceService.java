package com.zmyc.bamboo.core.service;

import com.zmyc.bamboo.core.dao.CheckPointDao;
import com.zmyc.bamboo.core.dao.EventLogDao;
import com.zmyc.bamboo.core.model.EventLog;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

public class IndexerPersistenceService {

    private final EventLogDao eventLogDao;
    private final CheckPointDao checkPointDao;

    public IndexerPersistenceService(EventLogDao eventLogDao, CheckPointDao checkPointDao) {
        this.eventLogDao = eventLogDao;
        this.checkPointDao = checkPointDao;
    }

    /**
     * 将写入事件和推进检查点合并为同一事务：
     * - 两者都成功才 commit
     * - 任一失败则 rollback，下一轮引擎会重扫同段区块（INSERT IGNORE 保证幂等）
     */
    @Transactional
    public void saveAndAdvance(List<EventLog> logs, BigInteger chainId, BigInteger toBlock) {
        eventLogDao.writeAll(logs);
        checkPointDao.modify(chainId, toBlock);
    }
}
