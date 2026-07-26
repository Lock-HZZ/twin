package com.zmyc.common.util;

import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Web3jUtils {
    private Web3jUtils() {
    }


    static final String personalMessagePrefix = "\u0019Ethereum Signed Message:\n";

    public static String recover(String message, String signature) throws SignatureException {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] prefixBytes = personalMessagePrefix.getBytes(StandardCharsets.UTF_8);
        byte[] lengthBytes = String.format("%d", msgBytes.length).getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(prefixBytes.length + lengthBytes.length + msgBytes.length);

        buffer.put(prefixBytes);
        buffer.put(lengthBytes);
        buffer.put(msgBytes);

        byte[] finalBytes = buffer.array();

        byte[] messageHash = Hash.sha3(finalBytes);
        Sign.SignatureData signatureData = extractSignature(signature);
        BigInteger publicKey = Sign.signedMessageHashToKey(messageHash, signatureData);
        String address =  "0x" + Keys.getAddress(publicKey);

        return Keys.toChecksumAddress(address);
    }

    public static boolean isSignatureValid(final String address, final String signature, final String message) {
        boolean match = false;

        final String prefix = personalMessagePrefix + message.length();
        final byte[] msgHash = Hash.sha3((prefix + message).getBytes());
        final byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;
        }

        final Sign.SignatureData sd = new Sign.SignatureData(v,
                Arrays.copyOfRange(signatureBytes, 0, 32),
                Arrays.copyOfRange(signatureBytes, 32, 64));

        String addressRecovered = null;

        for (int i = 0; i < 4; i++) {
            final BigInteger publicKey = Sign.recoverFromSignature((byte) i, new ECDSASignature(
                    new BigInteger(1, sd.getR()),
                    new BigInteger(1, sd.getS())), msgHash);

            if (publicKey != null) {
                addressRecovered = "0x" + Keys.getAddress(publicKey);

                if (addressRecovered.equalsIgnoreCase(address)) {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }

    private static Sign.SignatureData extractSignature(String signatureHex) {
        byte[] signatureBytes = Numeric.hexStringToByteArray(signatureHex);
        byte v = signatureBytes[64];
        byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
        byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);

        return new Sign.SignatureData(v, r, s);
    }

    public static String extractAddressFromTopic(String topic) {
        if (topic == null || topic.length() != 66 || !topic.startsWith("0x")) {
            throw new IllegalArgumentException("Invalid topic string");
        }
        return "0x" + topic.substring(26);
    }

    public static String sendErc20(Web3j web3j, String contractAddress, String privateKey,
                                   String to, BigInteger value, long chainId) throws Exception {

        Credentials credentials = Credentials.create(privateKey);

        String fromAddress = credentials.getAddress();
        BigInteger balance = web3j.ethGetBalance(fromAddress, org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                .send()
                .getBalance();

        System.out.println("钱包地址: " + fromAddress);
        System.out.println("当前余额: " + balance + " wei (" + balance.divide(BigInteger.valueOf(1000000000000000000L)) + " ETH)");

        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);

        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(value)),
                Collections.emptyList());

        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

        BigInteger gasLimit = BigInteger.valueOf(80000);

        BigInteger adjustedGasPrice = gasPrice.multiply(BigInteger.valueOf(200))
                .divide(BigInteger.valueOf(100));

        BigInteger estimatedGasCost = adjustedGasPrice.multiply(gasLimit);

        System.out.println("Gas配置 - gasPrice: " + adjustedGasPrice + " wei, gasLimit: " + gasLimit);
        System.out.println("预估gas费用: " + estimatedGasCost + " wei (" +
                estimatedGasCost.divide(BigInteger.valueOf(1000000000000000000L)) + " ETH)");

        // 检查余额是否足够支付 gas
        if (balance.compareTo(estimatedGasCost) < 0) {
            throw new RuntimeException(String.format(
                    "钱包余额不足以支付gas费用。需要: %s wei, 当前: %s wei, 缺少: %s wei",
                    estimatedGasCost, balance, estimatedGasCost.subtract(balance)
            ));
        }

        org.web3j.protocol.core.methods.response.EthSendTransaction response =
                txManager.sendTransaction(
                        adjustedGasPrice,
                        gasLimit,
                        contractAddress,
                        encodedFunction,
                        BigInteger.ZERO
                );

        String txHash = response.getTransactionHash();
        System.out.println("交易已发送，TxHash: " + txHash);
        if(StringUtils.isEmpty(txHash)){
            throw new RuntimeException(response.getError().getMessage());
        }
        return txHash;
    }

    public static String sendETH(Web3j web3j, String privateKey, String to, BigInteger value, long chainId) throws Exception {
        Credentials credentials = Credentials.create(privateKey);
        String fromAddress = credentials.getAddress();
        BigInteger balance = web3j.ethGetBalance(fromAddress, org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                .send()
                .getBalance();
        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(42000);

        BigInteger adjustedGasPrice = gasPrice.multiply(BigInteger.valueOf(110))
                .divide(BigInteger.valueOf(100));

        BigInteger estimatedGasCost = adjustedGasPrice.multiply(gasLimit);

        if (balance.compareTo(value.add(estimatedGasCost)) < 0) {
            System.out.println("钱包余额不足以支付转账金额和gas费用");
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        org.web3j.protocol.core.methods.response.EthSendTransaction response =
                txManager.sendTransaction(
                        adjustedGasPrice,
                        gasLimit,
                        to,
                        "",
                        value
                );

        String txHash = response.getTransactionHash();
        if(StringUtils.isEmpty(txHash)) {
            throw new RuntimeException(response.getError().getMessage());
        }
        return txHash;
    }

    public static String sendTransaction(
            Web3j web3j,
            String encodedFunction,
            String contractAddress,
            long chainId,
            String privateKey
    ) throws Exception {

        Credentials credentials = Credentials.create(privateKey);

        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        // 预估gas
        EthEstimateGas send = web3j.ethEstimateGas(org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(
                credentials.getAddress(),
                null,
                null,
                null,
                contractAddress,
                encodedFunction
        )).send();
        if (send.hasError()) {
            throw new RuntimeException("预估gas失败: " + send.getError().getMessage());
        }
        BigInteger gasLimit = send.getAmountUsed();

        BigInteger adjustedGasPrice = gasPrice.multiply(BigInteger.valueOf(200))
                .divide(BigInteger.valueOf(100));

        EthSendTransaction response =
                txManager.sendTransaction(
                        adjustedGasPrice,
                        gasLimit.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100)), // 乘以1.2作为安全边际
                        contractAddress,
                        encodedFunction,
                        BigInteger.ZERO
                );

        String txHash = response.getTransactionHash();
        System.out.println("交易已发送，TxHash: " + txHash);
        if(StringUtils.isEmpty(txHash)){
            throw new RuntimeException(response.getError().getMessage());
        }
        return txHash;
    }

    /**
     * 获取最新块的时间戳（秒）
     */
    public static long getLatestBlockTimestamp(Web3j web3j) throws Exception {
        try {
            var blockResponse = web3j.ethGetBlockByNumber(
                    org.web3j.protocol.core.DefaultBlockParameterName.LATEST,
                    false
            ).send();

            if (blockResponse.hasError()) {
                log.error("获取最新块失败: {}", blockResponse.getError().getMessage());
                throw new RuntimeException("获取最新块失败: " + blockResponse.getError().getMessage());
            }

            var block = blockResponse.getBlock();
            if (block == null) {
                throw new RuntimeException("无法获取最新块信息");
            }

            long timestamp = block.getTimestamp().longValue();
            log.info("获取最新块时间戳: {} (块号: {})", timestamp, block.getNumber());
            return timestamp;
        } catch (Exception e) {
            log.error("获取区块时间戳异常", e);
            throw e;
        }
    }

    public static TransactionReceipt waitForEthConfirmation(Web3j web3j, String txHash) {

        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash)
                        .send()
                        .getTransactionReceipt()
                        .orElse(null);

                if (receipt != null) {
                    log.info("交易 {} 已确认，状态: {}", txHash, receipt.getStatus());
                    return receipt;
                }

                log.info("第 {} 次获取交易 {} 回执为空，2秒后重试（共 {} 次）...", attempt, txHash, maxRetries);
            } catch (Exception e) {
                log.info("第 {} 次获取交易 {} 回执时出错: {}，2秒后重试（共 {} 次）", attempt, txHash, e.getMessage(), maxRetries);
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    log.error("等待过程中线程被中断: {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }

        log.error("在尝试 {} 次后仍未获取到交易 {} 的回执，交易可能仍在处理中", maxRetries, txHash);
        return null;
    }

    public static Transaction getTransactionByHash(Web3j web3j, String txHash) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                Transaction transaction = web3j.ethGetTransactionByHash(txHash)
                        .send()
                        .getTransaction()
                        .orElse(null);

                if (transaction != null) {
                    log.info("交易 {} 已上链，状态: {}", txHash, "pending".equalsIgnoreCase(transaction.getBlockHash()) ? "未确认" : "已确认");
                    return transaction;
                }

                log.info("第 {} 次获取交易 {} 内容为空，2秒后重试（共 {} 次）...", attempt, txHash, maxRetries);
            } catch (Exception e) {
                log.info("第 {} 次获取交易 {} 内容出错: {}，2秒后重试（共 {} 次）", attempt, txHash, e.getMessage(), maxRetries);
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    log.error("等待过程中线程被中断: {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }

        log.error("在尝试 {} 次后仍未获取到交易 {} 的交易内容，交易已被放弃", maxRetries, txHash);
        return null;
    }
}
