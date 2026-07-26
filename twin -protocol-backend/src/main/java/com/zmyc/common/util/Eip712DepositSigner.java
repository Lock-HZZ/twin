package com.zmyc.common.util;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * EIP-712 签名工具类
 * 用于后端 signer 为入金合约 Deposit.depositWithSig 生成签名。
 * <p>
 * 合约签名结构（Deposit.sol）：
 * domain = EIP712Domain(name="TwinProtocolDeposit", version="1", chainId, verifyingContract=deposit合约地址)
 * DEPOSIT_TYPEHASH = keccak256("Deposit(address user,uint256 amount,uint256 nonce,uint256 deadline,uint8 functionType)")
 * digest = keccak256("\x19\x01" || domainSeparator || structHash)
 */
public class Eip712DepositSigner {

    private static final String DOMAIN_TYPE =
            "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)";

    private static final String DEPOSIT_TYPE =
            "Deposit(address user,uint256 amount,uint256 nonce,uint256 deadline,uint8 functionType)";

    private static final byte[] DOMAIN_TYPEHASH = Hash.sha3(DOMAIN_TYPE.getBytes(StandardCharsets.UTF_8));
    private static final byte[] DEPOSIT_TYPEHASH = Hash.sha3(DEPOSIT_TYPE.getBytes(StandardCharsets.UTF_8));

    private Eip712DepositSigner() {
    }

    /**
     * 生成入金 EIP-712 签名
     *
     * @param privateKeyHex     signer 私钥（0x开头或不带前缀）
     * @param name              domain name，合约固定为 "TwinProtocolDeposit"
     * @param version           domain version，合约固定为 "1"
     * @param chainId           链ID
     * @param verifyingContract 入金合约地址
     * @param user              用户钱包地址
     * @param amount            入金金额（USDC 最小单位，uint256）
     * @param nonce             随机数（uint256）
     * @param deadline          截止时间戳（秒，uint256）
     * @param functionType      功能类型（合约固定为 2）
     * @return 0x开头的65字节签名
     */
    public static String sign(String privateKeyHex,
                              String name,
                              String version,
                              long chainId,
                              String verifyingContract,
                              String user,
                              BigInteger amount,
                              BigInteger nonce,
                              BigInteger deadline,
                              int functionType) {

        byte[] domainSeparator = buildDomainSeparator(name, version, chainId, verifyingContract);
        byte[] structHash = buildStructHash(user, amount, nonce, deadline, functionType);

        // digest = keccak256(0x1901 || domainSeparator || structHash)
        byte[] prefixed = concat(new byte[]{0x19, 0x01}, domainSeparator, structHash);
        byte[] digest = Hash.sha3(prefixed);

        // 直接对 digest 签名（合约用的是 _hashTypedDataV4 的结果，不再加 personal 前缀）
        ECKeyPair keyPair = ECKeyPair.create(Numeric.toBigInt(privateKeyHex));
        Sign.SignatureData sig = Sign.signMessage(digest, keyPair, false);

        byte v = sig.getV()[0];
        if (v < 27) {
            v += 27;
        }

        byte[] signature65 = concat(sig.getR(), sig.getS(), new byte[]{v});
        return Numeric.toHexString(signature65);
    }

    private static byte[] buildDomainSeparator(String name, String version, long chainId, String verifyingContract) {
        return Hash.sha3(concat(
                DOMAIN_TYPEHASH,
                Hash.sha3(name.getBytes(StandardCharsets.UTF_8)),
                Hash.sha3(version.getBytes(StandardCharsets.UTF_8)),
                uint256(BigInteger.valueOf(chainId)),
                addressAsWord(verifyingContract)
        ));
    }

    private static byte[] buildStructHash(String user, BigInteger amount, BigInteger nonce,
                                          BigInteger deadline, int functionType) {
        return Hash.sha3(concat(
                DEPOSIT_TYPEHASH,
                addressAsWord(user),
                uint256(amount),
                uint256(nonce),
                uint256(deadline),
                uint256(BigInteger.valueOf(functionType))
        ));
    }

    /** address 左侧补零到 32 字节 */
    private static byte[] addressAsWord(String address) {
        byte[] addr = Numeric.hexStringToByteArray(Numeric.prependHexPrefix(Numeric.cleanHexPrefix(address)));
        if (addr.length != 20) {
            throw new IllegalArgumentException("Invalid address: " + address);
        }
        byte[] out = new byte[32];
        System.arraycopy(addr, 0, out, 12, 20);
        return out;
    }

    /** uint256 转 32 字节 big-endian */
    private static byte[] uint256(BigInteger value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("uint256 cannot be negative");
        }
        byte[] raw = value.toByteArray();
        if (raw.length == 33 && raw[0] == 0) {
            byte[] trimmed = new byte[32];
            System.arraycopy(raw, 1, trimmed, 0, 32);
            return trimmed;
        }
        if (raw.length > 32) {
            throw new IllegalArgumentException("uint256 too large");
        }
        byte[] out = new byte[32];
        System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) {
            total += p.length;
        }
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }
}
