package com.zmyc.common.util;

import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * 签名工具类
 */
@Slf4j
public class ECDSASignatureUtil {

    public static String generateSign(String privateKeyHex,
                                    String paymentAddress,
                                    String userAddress,
                                    BigInteger usdtAmt,
                                    BigInteger beftAmt,
                                    BigInteger holdAmt,
                                    BigInteger nonce,
                                    BigInteger timestamp,
                                    long chainId) {

        byte[] packed = concat(
                addressToBytes20(paymentAddress),
                uint256ToBytes32(BigInteger.valueOf(chainId)),
                addressToBytes20(userAddress),
                uint256ToBytes32(usdtAmt),
                uint256ToBytes32(beftAmt),
                uint256ToBytes32(holdAmt),
                uint256ToBytes32(nonce),
                uint256ToBytes32(timestamp)
        );

        // 3) keccak256
        byte[] structHash = Hash.sha3(packed);

        // 4) toEthSignedMessageHash + sign
        BigInteger pk = Numeric.toBigInt(privateKeyHex);
        ECKeyPair keyPair = ECKeyPair.create(pk);
        Sign.SignatureData sig = Sign.signPrefixedMessage(structHash, keyPair);

        byte v = sig.getV()[0];
        if (v < 27) v += 27;

        byte[] signature65 = concat(sig.getR(), sig.getS(), new byte[]{v});
        return Numeric.toHexString(signature65);
    }

    public static String generateSign(String privateKeyHex,
                                      String paymentAddress,
                                      String userAddress,
                                      BigInteger usdtAmt,
                                      BigInteger eniAmt,
                                      BigInteger nonce,
                                      BigInteger timestamp,
                                      long chainId) {

        byte[] packed = concat(
                addressToBytes20(paymentAddress),
                uint256ToBytes32(BigInteger.valueOf(chainId)),
                addressToBytes20(userAddress),
                uint256ToBytes32(usdtAmt),
                uint256ToBytes32(eniAmt),
                uint256ToBytes32(nonce),
                uint256ToBytes32(timestamp)
        );

        // 3) keccak256
        byte[] structHash = Hash.sha3(packed);

        // 4) toEthSignedMessageHash + sign
        BigInteger pk = Numeric.toBigInt(privateKeyHex);
        ECKeyPair keyPair = ECKeyPair.create(pk);
        Sign.SignatureData sig = Sign.signPrefixedMessage(structHash, keyPair);

        byte v = sig.getV()[0];
        if (v < 27) v += 27; // 归一化为 27/28

        byte[] signature65 = concat(sig.getR(), sig.getS(), new byte[]{v});
        return Numeric.toHexString(signature65);
    }

    private static byte[] addressToBytes20(String address) {
        String clean = Numeric.cleanHexPrefix(address);
        if (clean.length() != 40) {
            throw new IllegalArgumentException("Invalid address: " + address);
        }
        return Numeric.hexStringToByteArray("0x" + clean);
    }

    private static byte[] uint256ToBytes32(BigInteger value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("uint256 cannot be negative");
        }
        byte[] raw = value.toByteArray(); // big-endian
        if (raw.length == 33 && raw[0] == 0) {
            raw = Arrays.copyOfRange(raw, 1, 33);
        }
        if (raw.length > 32) {
            throw new IllegalArgumentException("uint256 too large");
        }
        byte[] out = new byte[32];
        System.arraycopy(raw, 0, out, 32 - raw.length, raw.length); // 左侧补零
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        ByteBuffer buffer = ByteBuffer.allocate(total);
        for (byte[] p : parts) buffer.put(p);
        return buffer.array();
    }

}

