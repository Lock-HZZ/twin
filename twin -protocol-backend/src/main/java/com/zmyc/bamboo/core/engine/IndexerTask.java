package com.zmyc.bamboo.core.engine;

import com.zmyc.bamboo.core.dao.CheckPointDao;
import com.zmyc.bamboo.core.dao.EventLogDao;
import com.zmyc.bamboo.core.engine.dispatcher.MessageDispatcher;
import com.zmyc.bamboo.core.engine.filter.EventLogFilter;
import com.zmyc.bamboo.core.manager.RpcManager;
import com.zmyc.bamboo.core.model.CheckPoint;
import com.zmyc.bamboo.core.model.EventDefinition;
import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.bamboo.core.service.IndexerPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndexerTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexerTask.class);

    private static final BigInteger STEP_BLOCKS = BigInteger.valueOf(1000L);

    private BigInteger chainId;
    private final RpcManager rpcManager;
    private final Map<String, List<EventDefinition>> contractEventDefinitionsMap;
    private final List<String> contracts;
    private final CheckPointDao checkPointDao;
    private final EventLogDao eventLogDao;
    private final EventLogFilter eventLogFilter;
    private final List<MessageDispatcher> messageDispatchers;
    private final IndexerPersistenceService persistenceService;

    public IndexerTask(BigInteger chainId,
                       RpcManager rpcManager,
                       Map<String, List<EventDefinition>> contractEventDefinitionsMap,
                       CheckPointDao checkPointDao,
                       EventLogDao eventLogDao,
                       EventLogFilter eventLogFilter,
                       List<MessageDispatcher> messageDispatchers,
                       IndexerPersistenceService persistenceService) {
        this.chainId = chainId;
        this.rpcManager = rpcManager;
        this.contractEventDefinitionsMap = contractEventDefinitionsMap;
        this.contracts = contractEventDefinitionsMap.keySet().stream().toList();
        this.checkPointDao = checkPointDao;
        this.eventLogDao = eventLogDao;
        this.eventLogFilter = eventLogFilter;
        this.messageDispatchers = messageDispatchers;
        this.persistenceService = persistenceService;
    }

    @Override
    public void run() {
        try {
            doRun();
        } catch (Exception e) {
            LOGGER.error("IndexerTask failed", e);
        }
    }

    private void doRun() {
        CheckPoint checkPoint = checkPointDao.get(chainId);
        if (checkPoint == null) {
            LOGGER.error("bamboo indexer :: checkpoint not found for chainId={}, please initialize it first", chainId);
            return;
        }
        BigInteger fromBlock = checkPoint.getBlockHeight();

        BigInteger latestBlock = rpcManager.getLatestBlockHeight().subtract(BigInteger.TWO);

        // 已追上链头，等待新块出现，本轮跳过
        if (fromBlock.compareTo(latestBlock) > 0) {
            LOGGER.debug("bamboo indexer :: already at tip, waiting for new block. checkpoint={} latest={}", fromBlock, latestBlock);
            return;
        }

        // 每轮最多扫 STEP_BLOCKS 个块，防止单次请求范围过大被节点拒绝
        BigInteger toBlock = fromBlock.add(STEP_BLOCKS);
        if (toBlock.compareTo(latestBlock) >= 0) {
            toBlock = latestBlock;
        }

        List<EventLog> allLogs = getLogsWithRetry(fromBlock, toBlock);
        for (EventLog log : allLogs) {
            log.setChainId(chainId);
        }
        List<EventLog> filteredLogs = eventLogFilter.filter(contractEventDefinitionsMap, allLogs);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bamboo indexer ::\nfromBlock: {} toBlock: {}\ntotal logs: {}\ntotal filtered logs: {}\n",
                    fromBlock, toBlock, allLogs.size(), filteredLogs.size());
        }

        filteredLogs = new ArrayList<>(filteredLogs);
        filteredLogs.removeIf(log ->
                eventLogDao.exists(chainId, log.getAddress(), log.getTransactionHash(), log.getLogIndex())
        );

        // 通过 Spring 代理调用，@Transactional 在此生效：
        // writeAll + checkPointModify 在同一事务中，要么都成功要么都回滚
        // 检查点保存 toBlock + 1，下轮从下一个块开始，避免边界块被重复扫描
        persistenceService.saveAndAdvance(filteredLogs, chainId, toBlock.add(BigInteger.ONE));

        // dispatch 在事务提交后执行，保证推送的事件一定已落库
        for (MessageDispatcher dispatcher : messageDispatchers) {
            if (!dispatcher.isSupport(EventLog.class)) {
                continue;
            }
            for (EventLog log : filteredLogs) {
                dispatcher.publish(log);
            }
        }
    }

    private List<EventLog> getLogsWithRetry(BigInteger fromBlock, BigInteger toBlock) {
        int maxRetries = 3;
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return rpcManager.getLogs(fromBlock, toBlock, contracts);
            } catch (Exception e) {
                lastException = e;
                LOGGER.warn("第 {}/{} 次获取区块日志失败 from={} to={}: {}",
                        attempt, maxRetries, fromBlock, toBlock, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2_000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("getLogs retry interrupted", ie);
                    }
                }
            }
        }
        throw new RuntimeException(
                "getLogs failed after " + maxRetries + " retries from=" + fromBlock + " to=" + toBlock,
                lastException);
    }
}
