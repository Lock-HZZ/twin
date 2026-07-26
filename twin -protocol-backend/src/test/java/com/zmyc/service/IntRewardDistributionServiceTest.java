package com.zmyc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * INT权益奖励分配服务测试
 */
@Slf4j
@SpringBootTest
public class IntRewardDistributionServiceTest {/*

    @Autowired(required = false)
    private IntRewardDistributionService intRewardDistributionService;

    @Autowired(required = false)
    private CoinQueryUtil coinQueryUtil;

    @Autowired(required = false)
    private NodeOrderRepository nodeOrderRepository;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    *//**
     * 测试查询INT进账金额
     *//*
    @Test
    public void testGetINTIncomingAmount() {
        if (coinQueryUtil == null) {
            log.warn("CoinQueryUtil not available, skipping test");
            return;
        }

        BigInteger fromBlock = BigInteger.valueOf(78856007);
        BigInteger toBlock = coinQueryUtil.getLatestBlockNumber();
        
        log.info("Testing INT incoming amount query from block {} to {}", fromBlock, toBlock);
        
        // This will query the blockchain for INT incoming amounts
        // Uncomment to test: coinQueryUtil.getINTIncomingAmount(fromBlock, toBlock);
    }

    *//**
     * 测试查询节点订单数据（性能优化版本 - 使用SQL聚合）
     *//*
    @Test
    public void testQueryNodeOrdersOptimized() {
        if (nodeOrderRepository == null) {
            log.warn("NodeOrderRepository not available, skipping test");
            return;
        }

        // 测试SQL聚合查询总权重
        Integer totalWeight = nodeOrderRepository.calculateTotalWeight();
        log.info("Total weight (calculated by SQL): {}", totalWeight);

        // 测试统计用户数量
        int totalUsers = nodeOrderRepository.countDistinctUsersWithWeight();
        log.info("Total users with weight: {}", totalUsers);

        // 测试分页查询用户权重
        log.info("=== Testing paginated user weight query ===");
        int pageSize = 10;
        int offset = 0;
        var userWeights = nodeOrderRepository.findUserWeightsWithPage(offset, pageSize);
        
        log.info("First page (offset: {}, pageSize: {}): {} users", offset, pageSize, userWeights.size());
        for (var weightInfo : userWeights) {
            log.info("Address: {}, node1: {}, node2: {}, total weight: {}",
                    weightInfo.getAddress(),
                    weightInfo.getNode1Quantity(),
                    weightInfo.getNode2Quantity(),
                    weightInfo.getTotalWeight());
        }
    }

    *//**
     * 测试Redis存储和读取
     *//*
    @Test
    public void testRedisBlockStorage() {
        if (redisTemplate == null) {
            log.warn("RedisTemplate not available, skipping test");
            return;
        }

        String testKey = "int:reward:last_block";
        String testValue = "78856007";

        // 存储测试
        redisTemplate.opsForValue().set(testKey, testValue);
        log.info("Stored test block number: {}", testValue);

        // 读取测试
        String storedValue = redisTemplate.opsForValue().get(testKey);
        log.info("Retrieved block number: {}", storedValue);

        if (testValue.equals(storedValue)) {
            log.info("Redis storage test PASSED");
        } else {
            log.error("Redis storage test FAILED");
        }
    }

    *//**
     * 测试完整的奖励分配流程（注意：这会实际执行分配）
     * 取消注释以运行完整测试
     *//*
    // @Test
    public void testFullRewardDistribution() {
        if (intRewardDistributionService == null) {
            log.warn("IntRewardDistributionService not available, skipping test");
            return;
        }

        log.info("=== Starting Full Reward Distribution Test ===");
        
        // WARNING: This will actually distribute rewards!
        // Uncomment the line below only when you want to test in a safe environment
        // intRewardDistributionService.executeIntRewardDistribution();
        
        log.info("=== Full Reward Distribution Test Completed ===");
    }*/
}

