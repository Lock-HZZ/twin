package com.zmyc.contract;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Dividend 合约 Wrapper
 * 自动生成的合约调用类
 */
public class DividendContract extends Contract {

    public DividendContract(String contractAddress, Web3j web3j, Credentials credentials,
                           ContractGasProvider contractGasProvider) {
        super("", contractAddress, web3j, credentials, contractGasProvider);
    }

    public DividendContract(String contractAddress, Web3j web3j, TransactionManager transactionManager,
                           ContractGasProvider contractGasProvider) {
        super("", contractAddress, web3j, transactionManager, contractGasProvider);
    }

    /**
     * 批量添加奖励
     *
     * @param users 用户地址数组
     * @param rewardTypes 奖励类型数组
     * @param assetTypes 资产类型数组
     * @param amounts 金额数组
     * @return 交易回执
     */
    public RemoteCall<TransactionReceipt> addRewards(
            List<String> users,
            List<BigInteger> rewardTypes,
            List<BigInteger> assetTypes,
            List<BigInteger> amounts) {

        final Function function = new Function(
                "addRewards",
                Arrays.<Type>asList(
                        new DynamicArray<>(Address.class,
                                org.web3j.abi.Utils.typeMap(users, Address.class)),
                        new DynamicArray<>(Uint8.class,
                                org.web3j.abi.Utils.typeMap(rewardTypes, Uint8.class)),
                        new DynamicArray<>(Uint8.class,
                                org.web3j.abi.Utils.typeMap(assetTypes, Uint8.class)),
                        new DynamicArray<>(Uint256.class,
                                org.web3j.abi.Utils.typeMap(amounts, Uint256.class))),
                Collections.<TypeReference<?>>emptyList());

        return executeRemoteCallTransaction(function);
    }

    /**
     * 幂等批量添加奖励（同一 batchId 只能成功一次）
     *
     * @param batchId 批次唯一标识（32字节）
     * @param users 用户地址数组
     * @param rewardTypes 奖励类型数组
     * @param assetTypes 资产类型数组
     * @param amounts 金额数组
     * @return 交易回执
     */
    public RemoteCall<TransactionReceipt> addRewardsBatch(
            byte[] batchId,
            List<String> users,
            List<BigInteger> rewardTypes,
            List<BigInteger> assetTypes,
            List<BigInteger> amounts) {

        final Function function = new Function(
                "addRewardsBatch",
                Arrays.<Type>asList(
                        new Bytes32(batchId),
                        new DynamicArray<>(Address.class,
                                org.web3j.abi.Utils.typeMap(users, Address.class)),
                        new DynamicArray<>(Uint8.class,
                                org.web3j.abi.Utils.typeMap(rewardTypes, Uint8.class)),
                        new DynamicArray<>(Uint8.class,
                                org.web3j.abi.Utils.typeMap(assetTypes, Uint8.class)),
                        new DynamicArray<>(Uint256.class,
                                org.web3j.abi.Utils.typeMap(amounts, Uint256.class))),
                Collections.<TypeReference<?>>emptyList());

        return executeRemoteCallTransaction(function);
    }

    /**
     * 查询某批次是否已处理
     *
     * @param batchId 批次唯一标识（32字节）
     * @return 是否已处理
     */
    public RemoteCall<Boolean> isBatchProcessed(byte[] batchId) {
        final Function function = new Function(
                "isBatchProcessed",
                Arrays.<Type>asList(new Bytes32(batchId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));

        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    /**
     * 查询用户余额
     *
     * @param user 用户地址
     * @param assetType 资产类型（0-USDC, 1-TIP）
     * @return 余额
     */
    public RemoteCall<BigInteger> getTotalBalance(String user, BigInteger assetType) {
        final Function function = new Function(
                "getTotalBalance",
                Arrays.<Type>asList(new Address(user), new Uint8(assetType)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));

        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    /**
     * 加载合约实例
     */
    public static DividendContract load(String contractAddress, Web3j web3j,
                                       Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DividendContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    /**
     * 加载合约实例（使用 TransactionManager）
     */
    public static DividendContract load(String contractAddress, Web3j web3j,
                                       TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DividendContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }
}
