package com.zmyc.util;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * USDT查询工具测试类
 * 
 * 注意：此测试需要真实的Web3连接，请确保配置了有效的RPC URL
 */
@SpringBootTest
class UsdtQueryUtilTest {

   /* @Autowired
    private CoinQueryUtil usdtQueryUtil;

    *//**
     * 测试BSC链的USDT查询
     * 注意：需要将web3.url配置为BSC的RPC地址
     *//*
    @Test
    void testBscUsdtQuery() {
        BigInteger latestBlock = usdtQueryUtil.getLatestBlockNumber();
        BigInteger fromBlock = BigInteger.valueOf(78856007);
        BigInteger toBlock = latestBlock;
        
        System.out.println("BSC链 - 查询区块范围: " + fromBlock + " - " + toBlock);
        
        BigDecimal amount = usdtQueryUtil.getINTIncomingAmount(
            fromBlock,
            toBlock
        );
        
        System.out.println("BSC USDT进账总额: " + amount + " USDT");
        
        assert amount != null;
    }*/
}

