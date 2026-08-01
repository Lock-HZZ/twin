package com.zmyc.service;

import com.zmyc.common.config.DividendContractConfig;
import com.zmyc.common.config.FeeDividendContractConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * FeeDividend 合约调用服务
 * 封装 FeeDividend.addRewards(address[], uint256[]) 调用
 */
@Service
@Slf4j
public class FeeDividendContractService {

    private static final BigInteger USDC_DECIMALS = BigInteger.TEN.pow(6);

    @Autowired
    private FeeDividendContractConfig feeDividendConfig;

    @Autowired
    private DividendContractConfig dividendConfig;

    @Autowired
    private com.zmyc.common.config.Web3jConfig.Web3jManager web3jManager;

    private Credentials credentials;
    private StaticGasProvider gasProvider;

    @PostConstruct
    public void init() {
        credentials = Credentials.create(dividendConfig.getOperatorPrivateKey());
        BigInteger gasPrice = getNetworkGasPrice();
        gasProvider = new StaticGasProvider(gasPrice, BigInteger.valueOf(feeDividendConfig.getGasLimit()));
        log.info("FeeDividend合约服务初始化完成: contract={}", feeDividendConfig.getContractAddress());
    }

    /**
     * 调用 FeeDividend.addRewardsBatch(bytes32, address[], uint256[])
     * 同一 batchId 链上只会成功执行一次，后端可安全重发
     *
     * @param batchId 32字节hex（含0x前缀），全局唯一
     * @param rewards key=用户地址, value=USDC金额（业务精度，6位小数）
     * @return 交易哈希
     */
    public String sendFeeRewardsBatch(String batchId, Map<String, BigDecimal> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            throw new IllegalArgumentException("奖励映射不能为空");
        }

        List<String> users = List.copyOf(rewards.keySet());
        List<BigInteger> amounts = users.stream()
                .map(addr -> rewards.get(addr)
                        .multiply(new BigDecimal(USDC_DECIMALS))
                        .setScale(0, RoundingMode.DOWN)
                        .toBigInteger())
                .toList();

        byte[] batchIdBytes = Numeric.hexStringToByteArray(batchId);

        Function function = new Function(
                "addRewardsBatch",
                Arrays.asList(
                        new org.web3j.abi.datatypes.generated.Bytes32(batchIdBytes),
                        new DynamicArray<>(Address.class,
                                users.stream().map(Address::new).toList()),
                        new DynamicArray<>(Uint256.class,
                                amounts.stream().map(Uint256::new).toList())
                ),
                List.of()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        try {
            Web3j web3j = getWeb3j();
            BigInteger nonce = web3j.ethGetTransactionCount(
                    credentials.getAddress(), DefaultBlockParameterName.PENDING).send().getTransactionCount();

            RawTransaction rawTx = RawTransaction.createTransaction(
                    nonce,
                    getNetworkGasPrice(),
                    gasProvider.getGasLimit(null),
                    feeDividendConfig.getContractAddress(),
                    BigInteger.ZERO,
                    encodedFunction
            );

            byte[] signedTx = TransactionEncoder.signMessage(rawTx, dividendConfig.getChainId(), credentials);
            EthSendTransaction response = web3j.ethSendRawTransaction(Numeric.toHexString(signedTx)).send();

            if (response.hasError()) {
                throw new RuntimeException("FeeDividend批次交易发送失败: " + response.getError().getMessage());
            }

            String txHash = response.getTransactionHash();
            log.info("FeeDividend批次已发送: batchId={}, count={}, txHash={}", batchId, users.size(), txHash);
            return txHash;
        } catch (Exception e) {
            log.error("FeeDividend批次发送失败: batchId={}, error={}", batchId, e.getMessage(), e);
            throw new RuntimeException("FeeDividend批次发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询批次是否已在链上处理（幂等校验）
     */
    public boolean isBatchProcessed(String batchId) {
        byte[] batchIdBytes = Numeric.hexStringToByteArray(batchId);

        Function function = new Function(
                "isBatchProcessed",
                List.of(new org.web3j.abi.datatypes.generated.Bytes32(batchIdBytes)),
                List.of(new TypeReference<org.web3j.abi.datatypes.Bool>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        try {
            Web3j web3j = getWeb3j();
            String result = web3j.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            credentials.getAddress(),
                            feeDividendConfig.getContractAddress(),
                            encodedFunction),
                    DefaultBlockParameterName.LATEST).send().getValue();

            List<Type> decoded = org.web3j.abi.FunctionReturnDecoder.decode(
                    result, function.getOutputParameters());
            return !decoded.isEmpty() && (boolean) decoded.get(0).getValue();
        } catch (Exception e) {
            log.warn("查询FeeDividend批次状态失败: batchId={}, error={}", batchId, e.getMessage());
            return false;
        }
    }

    /**
     * 检查交易是否仍在内存池或已确认
     */
    public boolean isTransactionAlive(String txHash) {
        if (txHash == null || txHash.isEmpty()) return false;
        try {
            Web3j web3j = getWeb3j();
            var receiptOpt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            if (receiptOpt.isPresent()) {
                return "0x1".equals(receiptOpt.get().getStatus());
            }
            return web3j.ethGetTransactionByHash(txHash).send().getTransaction().isPresent();
        } catch (Exception e) {
            log.warn("查询FeeDividend交易存活状态异常，保守视为存活: txHash={}", txHash);
            return true;
        }
    }

    /**
     * @deprecated 无幂等保证，保留供内部测试使用。生产请用 sendFeeRewardsBatch
     */
    @Deprecated
    public String sendFeeRewards(Map<String, BigDecimal> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            throw new IllegalArgumentException("奖励映射不能为空");
        }

        List<String> users = List.copyOf(rewards.keySet());
        List<BigInteger> amounts = users.stream()
                .map(addr -> rewards.get(addr)
                        .multiply(new BigDecimal(USDC_DECIMALS))
                        .setScale(0, RoundingMode.DOWN)
                        .toBigInteger())
                .toList();

        Function function = new Function(
                "addRewards",
                Arrays.asList(
                        new DynamicArray<>(Address.class,
                                users.stream().map(Address::new).toList()),
                        new DynamicArray<>(Uint256.class,
                                amounts.stream().map(Uint256::new).toList())
                ),
                List.of()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        try {
            Web3j web3j = getWeb3j();
            BigInteger nonce = web3j.ethGetTransactionCount(
                    credentials.getAddress(), DefaultBlockParameterName.PENDING).send().getTransactionCount();

            RawTransaction rawTx = RawTransaction.createTransaction(
                    nonce,
                    getNetworkGasPrice(),
                    gasProvider.getGasLimit(null),
                    feeDividendConfig.getContractAddress(),
                    BigInteger.ZERO,
                    encodedFunction
            );

            byte[] signedTx = TransactionEncoder.signMessage(rawTx, dividendConfig.getChainId(), credentials);
            EthSendTransaction response = web3j.ethSendRawTransaction(Numeric.toHexString(signedTx)).send();

            if (response.hasError()) {
                throw new RuntimeException("FeeDividend交易发送失败: " + response.getError().getMessage());
            }

            String txHash = response.getTransactionHash();
            log.info("FeeDividend手续费分配已发送: txHash={}, count={}", txHash, users.size());
            return txHash;
        } catch (Exception e) {
            log.error("FeeDividend手续费分配发送失败: error={}", e.getMessage(), e);
            throw new RuntimeException("FeeDividend手续费分配发送失败: " + e.getMessage(), e);
        }
    }

    private Web3j getWeb3j() {
        return web3jManager.getWeb3j(String.valueOf(dividendConfig.getChainId()));
    }

    private BigInteger getNetworkGasPrice() {
        try {
            BigInteger gasPrice = getWeb3j().ethGasPrice().send().getGasPrice();
            return gasPrice.multiply(BigInteger.valueOf(12)).divide(BigInteger.TEN); // 1.2倍
        } catch (Exception e) {
            log.warn("获取网络Gas价格失败，使用默认值");
            return BigInteger.valueOf(5_000_000_000L);
        }
    }
}
