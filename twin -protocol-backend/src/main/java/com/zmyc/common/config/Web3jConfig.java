package com.zmyc.common.config;

import com.zmyc.infrastructure.entity.BlockchainChainDO;
import com.zmyc.infrastructure.repository.BlockchainChainRepository;
import lombok.Setter;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PreDestroy;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ConfigurationProperties(prefix = "web3")
public class Web3jConfig {

    private static final Logger log = LoggerFactory.getLogger(Web3jConfig.class);

    @Autowired
    private BlockchainChainRepository chainRepository;

    @Setter
    private Map<String, List<String>> chains = new HashMap<>();

    private final Map<String, ChainConnectionPool> chainPools = new HashMap<>();

    /**
     * 创建Web3j实例管理器Bean
     */
    @Bean
    public Web3jManager web3jManager() {
        initializeChains();
        return new Web3jManager(chainPools);
    }

    /**
     * 初始化所有配置的链
     */
    private void initializeChains() {
        List<BlockchainChainDO> enabledChains = chainRepository.findAllEnabled();

        for (BlockchainChainDO chain : enabledChains) {
            String chainKey = String.valueOf(chain.getChainId());
            List<String> rpcUrls = Collections.singletonList(chain.getRpcUrl());

            ChainConnectionPool pool = new ChainConnectionPool(chain.getChainName(), rpcUrls);
            chainPools.put(chainKey, pool);
            log.info("从数据库初始化链 [{}] (chainId: {}), RPC: {}",
                chain.getChainName(), chain.getChainId(), maskUrl(chain.getRpcUrl()));
        }

        chains.forEach((chainName, rpcUrls) -> {
            if (rpcUrls != null && !rpcUrls.isEmpty()) {
                List<String> validUrls = rpcUrls.stream()
                        .filter(url -> url != null && !url.trim().isEmpty())
                        .toList();

                if (!validUrls.isEmpty()) {
                    ChainConnectionPool pool = new ChainConnectionPool(chainName, validUrls);
                    chainPools.put(chainName.toLowerCase(), pool);
                    log.info("从配置文件初始化链 [{}]，配置了 {} 个RPC节点", chainName, validUrls.size());
                }
            }
        });
    }

    private String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll("([a-zA-Z0-9]{32,})", "***");
    }

    private Web3j createWeb3j(String rpcUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(new okhttp3.ConnectionPool(5, 2, TimeUnit.MINUTES))
                .build();
        return Web3j.build(new HttpService(rpcUrl, client));
    }

    /**
     * 应用关闭时自动关闭所有Web3j连接
     */
    @PreDestroy
    public void destroy() {
        chainPools.forEach((chain, pool) -> {
            try {
                pool.shutdown();
                log.info("Web3j连接池已关闭: {}", chain);
            } catch (Exception e) {
                log.error("关闭Web3j连接池时发生错误 [{}]: {}", chain, e.getMessage());
            }
        });
        chainPools.clear();
    }

    /**
     * 链连接池，管理单条链的多个RPC节点
     */
    public class ChainConnectionPool {
        private final String chainName;
        private final List<String> rpcUrls;
        private final List<Web3j> web3jInstances;
        private final AtomicInteger currentIndex;
        private final Map<Integer, Long> failureTimestamps;
        private static final long FAILURE_COOLDOWN_MS = 60000; // 失败后冷却时间1分钟

        public ChainConnectionPool(String chainName, List<String> rpcUrls) {
            this.chainName = chainName;
            this.rpcUrls = new ArrayList<>(rpcUrls);
            this.web3jInstances = new ArrayList<>();
            this.currentIndex = new AtomicInteger(0);
            this.failureTimestamps = new HashMap<>();

            for (String rpcUrl : rpcUrls) {
                try {
                    Web3j web3j = createWeb3j(rpcUrl);
                    web3jInstances.add(web3j);
                    log.info("成功创建RPC连接 [{}]: {}", chainName, maskUrl(rpcUrl));
                } catch (Exception e) {
                    log.error("创建RPC连接失败 [{}]: {}", chainName, maskUrl(rpcUrl), e);
                    web3jInstances.add(null);
                }
            }
        }

        /**
         * 获取下一个可用的Web3j实例（轮询）
         */
        public Web3j getNextWeb3j() {
            int size = web3jInstances.size();
            if (size == 0) {
                throw new IllegalStateException("链 [" + chainName + "] 没有可用的RPC节点");
            }

            // 尝试所有节点
            for (int i = 0; i < size; i++) {
                int index = currentIndex.getAndIncrement() % size;
                
                // 检查是否在冷却期
                Long failureTime = failureTimestamps.get(index);
                if (failureTime != null && System.currentTimeMillis() - failureTime < FAILURE_COOLDOWN_MS) {
                    continue;
                }

                Web3j web3j = web3jInstances.get(index);
                if (web3j != null) {
                    log.debug("使用RPC节点 [{}] 索引: {}, URL: {}", chainName, index, maskUrl(rpcUrls.get(index)));
                    return web3j;
                }
            }

            for (int i = 0; i < size; i++) {
                Web3j web3j = web3jInstances.get(i);
                if (web3j != null) {
                    log.warn("所有节点都在冷却期，强制使用节点 [{}] 索引: {}", chainName, i);
                    return web3j;
                }
            }

            throw new IllegalStateException("链 [" + chainName + "] 没有可用的RPC节点");
        }

        public void markFailure(Web3j failedWeb3j) {
            for (int i = 0; i < web3jInstances.size(); i++) {
                if (web3jInstances.get(i) == failedWeb3j) {
                    failureTimestamps.put(i, System.currentTimeMillis());
                    log.warn("标记RPC节点失败 [{}] 索引: {}, URL: {}", chainName, i, maskUrl(rpcUrls.get(i)));
                    break;
                }
            }
        }

        public List<String> getRpcUrls() {
            return new ArrayList<>(rpcUrls);
        }

        public void shutdown() {
            for (int i = 0; i < web3jInstances.size(); i++) {
                Web3j web3j = web3jInstances.get(i);
                if (web3j != null) {
                    try {
                        web3j.shutdown();
                        log.info("关闭RPC连接 [{}]: {}", chainName, maskUrl(rpcUrls.get(i)));
                    } catch (Exception e) {
                        log.error("关闭RPC连接失败 [{}]: {}", chainName, maskUrl(rpcUrls.get(i)), e);
                    }
                }
            }
        }

        private String maskUrl(String url) {
            if (url == null) return "null";
            return url.replaceAll("([a-zA-Z0-9]{32,})", "***");
        }
    }

    public static class Web3jManager {
        private final Map<String, ChainConnectionPool> chainPools;

        public Web3jManager(Map<String, ChainConnectionPool> chainPools) {
            this.chainPools = chainPools;
        }

        public Web3j getWeb3j(String chain) {
            ChainConnectionPool pool = chainPools.get(chain.toLowerCase());
            if (pool == null) {
                throw new IllegalArgumentException("未配置的链: " + chain);
            }
            return pool.getNextWeb3j();
        }

        public void markFailure(String chain, Web3j failedWeb3j) {
            ChainConnectionPool pool = chainPools.get(chain.toLowerCase());
            if (pool != null) {
                pool.markFailure(failedWeb3j);
            }
        }

        public Set<String> getAvailableChains() {
            return chainPools.keySet();
        }

        public boolean isChainAvailable(String chain) {
            return chainPools.containsKey(chain.toLowerCase());
        }

        public List<String> getChainRpcUrls(String chain) {
            ChainConnectionPool pool = chainPools.get(chain.toLowerCase());
            if (pool == null) {
                return Collections.emptyList();
            }
            return pool.getRpcUrls();
        }
    }
}
