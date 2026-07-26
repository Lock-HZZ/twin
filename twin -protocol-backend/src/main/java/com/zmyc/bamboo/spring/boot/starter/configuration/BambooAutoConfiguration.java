package com.zmyc.bamboo.spring.boot.starter.configuration;

import com.zmyc.bamboo.core.dao.CheckPointDao;
import com.zmyc.bamboo.core.dao.EventLogDao;
import com.zmyc.bamboo.core.engine.DefaultIndexerEngine;
import com.zmyc.bamboo.core.engine.IndexerEngine;
import com.zmyc.bamboo.core.engine.IndexerTask;
import com.zmyc.bamboo.core.engine.UnprocessedEventLogRetryScheduler;
import com.zmyc.bamboo.core.engine.dispatcher.EventLogMemoryMessageDispatcher;
import com.zmyc.bamboo.core.engine.dispatcher.MessageDispatcher;
import com.zmyc.bamboo.core.engine.dispatcher.subscriber.MessageSubscriber;
import com.zmyc.bamboo.core.engine.filter.DefaultEventLogFilter;
import com.zmyc.bamboo.core.manager.RpcManager;
import com.zmyc.bamboo.core.manager.impl.DefaultRpcManager;
import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.bamboo.core.service.IndexerPersistenceService;
import com.zmyc.bamboo.spring.boot.starter.properties.BambooProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(BambooProperties.class)
public class BambooAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BambooAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public Web3j bambooWeb3j(BambooProperties props) {
        var blockchains = props.getBlockchains();
        if (blockchains == null || blockchains.isEmpty()) {
            throw new IllegalArgumentException("bamboo.blockchains must not be empty");
        }
        var firstBlockchain = blockchains.values().iterator().next();
        String url = firstBlockchain.getRpcNodeUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("bamboo.blockchains[*].rpc-node-url must not be empty");
        }
        return Web3j.build(new HttpService(url));
    }

    @Bean
    @ConditionalOnMissingBean
    public RpcManager rpcManager(BambooProperties props) {
        var blockchains = props.getBlockchains();
        if (blockchains == null || blockchains.isEmpty()) {
            throw new IllegalArgumentException("bamboo.blockchains must not be empty");
        }
        var firstBlockchain = blockchains.values().iterator().next();
        String url = firstBlockchain.getRpcNodeUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("bamboo.blockchains[*].rpc-node-url must not be empty");
        }
        return new DefaultRpcManager(Web3j.build(new HttpService(url)));
    }

    @Bean
    public Map<BigInteger, Web3j> chainWeb3jMap(BambooProperties props) {
        Map<BigInteger, Web3j> map = new HashMap<>();
        var blockchains = props.getBlockchains();
        if (blockchains != null) {
            for (var entry : blockchains.entrySet()) {
                String rpcUrl = entry.getValue().getRpcNodeUrl();
                if (rpcUrl != null && !rpcUrl.isBlank()) {
                    map.put(entry.getValue().getId(), Web3j.build(new HttpService(rpcUrl)));
                }
            }
        }
        return map;
    }

    @Bean
    public CheckPointDao checkPointDao(ObjectProvider<DataSource> ds) {
        return new CheckPointDao(requireDataSource(ds));
    }

    @Bean
    public EventLogDao eventLogDao(ObjectProvider<DataSource> ds) {
        return new EventLogDao(requireDataSource(ds));
    }

    @Bean
    public MessageDispatcher eventLogMemoryMessageDispatcher(List<MessageSubscriber> subscribers) {
        EventLogMemoryMessageDispatcher dispatcher = new EventLogMemoryMessageDispatcher();
        for (MessageSubscriber subscriber : subscribers) {
            if (subscriber.isSupport(dispatcher.getMessageClass())) {
                dispatcher.addSubscriber(subscriber);
            }
        }
        return dispatcher;
    }

    // Spring 会将此 bean 包装为 @Transactional 代理
    @Bean
    public IndexerPersistenceService indexerPersistenceService(EventLogDao eventLogDao,
                                                               CheckPointDao checkPointDao) {
        return new IndexerPersistenceService(eventLogDao, checkPointDao);
    }

    @Bean
    public IndexerEngine indexerEngine(BambooProperties props,
                                       ObjectProvider<DataSource> ds,
                                       CheckPointDao checkPointDao,
                                       EventLogDao eventLogDao,
                                       IndexerPersistenceService persistenceService,
                                       List<MessageDispatcher> dispatchers) {
        if (Boolean.TRUE.equals(props.getAutoInitializeSchema())) {
            initializeSchema(requireDataSource(ds));
        }

        var blockchains = props.getBlockchains();
        if (blockchains == null || blockchains.isEmpty()) {
            throw new IllegalArgumentException("bamboo.blockchains must not be empty");
        }

        DefaultIndexerEngine engine = new DefaultIndexerEngine(
                props.getScheduleInitialSeconds(),
                props.getScheduleDelaySeconds()
        );

        for (var entry : blockchains.entrySet()) {
            BigInteger chainId = entry.getValue().getId();
            String rpcUrl = entry.getValue().getRpcNodeUrl();

            if (rpcUrl == null || rpcUrl.isBlank()) {
                throw new IllegalArgumentException("bamboo.blockchains[" + entry.getKey() + "].rpc-node-url must not be empty");
            }

            RpcManager chainRpcManager = new DefaultRpcManager(Web3j.build(new HttpService(rpcUrl)));

            if (Boolean.TRUE.equals(props.getAutoInitializeCheckPoint())) {
                if (checkPointDao.get(chainId) == null) {
                    checkPointDao.create(chainId, chainRpcManager.getLatestBlockHeight());
                }
            }

            IndexerTask task = new IndexerTask(
                    chainId,
                    chainRpcManager,
                    entry.getValue().getEventListener().getContractEventDefinitions(),
                    checkPointDao,
                    eventLogDao,
                    new DefaultEventLogFilter(),
                    dispatchers,
                    persistenceService
            );
            engine.addTask(task);
        }

        List<EventLog> unprocessed = eventLogDao.findUnprocessed();
        if (!unprocessed.isEmpty()) {
            LOGGER.warn("bamboo :: found {} unprocessed event log(s), re-dispatching on startup...",
                    unprocessed.size());
            for (MessageDispatcher dispatcher : dispatchers) {
                if (!dispatcher.isSupport(EventLog.class)) continue;
                for (EventLog log : unprocessed) {
                    dispatcher.publish(log);
                }
            }
        }

        if (Boolean.TRUE.equals(props.getAutoStartEngine())) {
            engine.start();
        }

        return engine;
    }

    @Bean
    public UnprocessedEventLogRetryScheduler unprocessedEventLogRetryScheduler(
            BambooProperties props,
            EventLogDao eventLogDao,
            List<MessageDispatcher> dispatchers) {
        int seconds = props.getUnprocessedRetrySeconds() != null ? props.getUnprocessedRetrySeconds() : 60;
        UnprocessedEventLogRetryScheduler scheduler =
                new UnprocessedEventLogRetryScheduler(eventLogDao, dispatchers, seconds);
        scheduler.start();
        return scheduler;
    }

    private DataSource requireDataSource(ObjectProvider<DataSource> provider) {
        DataSource ds = provider.getIfAvailable();
        if (ds == null) throw new RuntimeException("Please configure a DataSource");
        return ds;
    }

    private void initializeSchema(DataSource dataSource) {
        LOGGER.info("bamboo :: initializing schema...");
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("schema-mysql-initialize.sql")) {
            if (is == null) throw new RuntimeException("schema-mysql-initialize.sql not found in classpath");
            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    String s = statement.trim();
                    if (!s.isEmpty()) stmt.execute(s);
                }
            }
            LOGGER.info("bamboo :: schema initialized.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize bamboo schema", e);
        }
    }
}
