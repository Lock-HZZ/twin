package com.zmyc.service;

import com.zmyc.infrastructure.entity.FeeDistributionRecordDO;
import com.zmyc.infrastructure.entity.UserDO;
import com.zmyc.infrastructure.repository.FeeDistributionRecordRepository;
import com.zmyc.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 提现手续费二次分配服务
 *
 * 当用户从 Dividend 合约提取 USDC 时，合约自动扣留 10% 手续费并转入 FeeDividend 合约。
 * 本服务监听 Withdraw 事件，将该手续费按以下规则分配给节点和合伙人：
 *   节点  (40%)：黄金=1, 钻石=3, 皇冠=10，按权重平均
 *   合伙人(30%)：等额平分
 *   平台  (30%)：留在 FeeDividend 合约（不调用 addRewards，由平台自行提取）
 */
@Service
@Slf4j
public class FeeDividendService {

    private static final BigDecimal NODE_RATIO = new BigDecimal("0.40");
    private static final BigDecimal PARTNER_RATIO = new BigDecimal("0.30");

    private static final Map<Integer, Integer> NODE_WEIGHTS = Map.of(
            UserDO.Role.GOLD, 1,
            UserDO.Role.DIAMOND, 3,
            UserDO.Role.CROWN, 10
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeeDistributionRecordRepository distributionRecordRepository;

    @Autowired
    private FeeDividendContractService feeDividendContractService;

    /**
     * 将本次手续费分配给节点和合伙人
     *
     * @param withdrawTxHash 触发来源：用户提现的交易哈希（幂等键）
     * @param feeAmount      本次需分配的总手续费金额（USDC，业务精度）
     */
    public void distributeFee(String withdrawTxHash, BigDecimal feeAmount) {
        // 幂等：同一笔提现只处理一次
        if (distributionRecordRepository.findByWithdrawTxHash(withdrawTxHash) != null) {
            log.warn("手续费分配已存在，跳过: withdrawTxHash={}", withdrawTxHash);
            return;
        }

        if (feeAmount == null || feeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("手续费金额为0，跳过: withdrawTxHash={}", withdrawTxHash);
            return;
        }

        log.info("开始手续费二次分配: withdrawTxHash={}, feeAmount={}", withdrawTxHash, feeAmount);

        Map<String, BigDecimal> rewards = buildRewardsMap(feeAmount, withdrawTxHash);
        if (rewards.isEmpty()) {
            log.warn("无可分配对象，跳过上链: withdrawTxHash={}", withdrawTxHash);
            return;
        }

        String batchId = generateBatchId(withdrawTxHash);

        // 落库（PENDING）
        long now = System.currentTimeMillis() / 1000;
        FeeDistributionRecordDO record = new FeeDistributionRecordDO();
        record.setWithdrawTxHash(withdrawTxHash);
        record.setBatchId(batchId);
        record.setFeeAmount(feeAmount);
        record.setStatus(FeeDistributionRecordDO.Status.PENDING);
        record.setCreatedDate(now);
        record.setUpdatedDate(now);
        distributionRecordRepository.save(record);

        sendBatch(record, rewards);
    }

    /**
     * 重发批次（补偿任务专用）：交易失效且链上未处理时调用
     */
    public void resendBatch(FeeDistributionRecordDO record) {
        Map<String, BigDecimal> rewards = buildRewardsMap(record.getFeeAmount(), record.getWithdrawTxHash());
        if (rewards.isEmpty()) {
            log.warn("重发时无可分配对象: withdrawTxHash={}", record.getWithdrawTxHash());
            return;
        }
        sendBatch(record, rewards);
    }

    private void sendBatch(FeeDistributionRecordDO record, Map<String, BigDecimal> rewards) {
        try {
            String distributeTxHash = feeDividendContractService.sendFeeRewardsBatch(record.getBatchId(), rewards);
            record.setDistributeTxHash(distributeTxHash);
            record.setStatus(FeeDistributionRecordDO.Status.SENT);
            record.setUpdatedDate(System.currentTimeMillis() / 1000);
            distributionRecordRepository.save(record);
            log.info("手续费分配已发送: withdrawTxHash={}, batchId={}, distributeTxHash={}, recipients={}",
                    record.getWithdrawTxHash(), record.getBatchId(), distributeTxHash, rewards.size());
        } catch (Exception e) {
            log.error("手续费分配上链失败，保持PENDING待补偿任务重发: withdrawTxHash={}, error={}",
                    record.getWithdrawTxHash(), e.getMessage(), e);
        }
    }

    private Map<String, BigDecimal> buildRewardsMap(BigDecimal feeAmount, String withdrawTxHash) {
        BigDecimal nodePool = feeAmount.multiply(NODE_RATIO);
        BigDecimal partnerPool = feeAmount.multiply(PARTNER_RATIO);

        Map<String, BigDecimal> rewards = new LinkedHashMap<>();

        // 节点分配（加权平均）
        List<UserDO> nodes = userRepository.findByRoles(
                Arrays.asList(UserDO.Role.GOLD, UserDO.Role.DIAMOND, UserDO.Role.CROWN));
        int totalNodeWeight = nodes.stream()
                .mapToInt(u -> NODE_WEIGHTS.getOrDefault(u.getRole(), 0))
                .sum();

        if (totalNodeWeight > 0) {
            for (UserDO node : nodes) {
                if (node.getAddress() == null || node.getAddress().isEmpty()) continue;
                int weight = NODE_WEIGHTS.getOrDefault(node.getRole(), 0);
                BigDecimal share = nodePool
                        .multiply(new BigDecimal(weight))
                        .divide(new BigDecimal(totalNodeWeight), 6, RoundingMode.DOWN);
                if (share.compareTo(BigDecimal.ZERO) > 0) {
                    rewards.merge(node.getAddress(), share, BigDecimal::add);
                }
            }
        } else {
            log.warn("无节点用户，节点部分手续费留存合约: withdrawTxHash={}", withdrawTxHash);
        }

        // 合伙人分配（等额平分）
        List<UserDO> partners = userRepository.findByRole(UserDO.Role.PARTNER);
        if (!partners.isEmpty()) {
            BigDecimal perPartner = partnerPool.divide(
                    new BigDecimal(partners.size()), 6, RoundingMode.DOWN);
            for (UserDO partner : partners) {
                if (partner.getAddress() == null || partner.getAddress().isEmpty()) continue;
                if (perPartner.compareTo(BigDecimal.ZERO) > 0) {
                    rewards.merge(partner.getAddress(), perPartner, BigDecimal::add);
                }
            }
        } else {
            log.warn("无合伙人用户，合伙人部分手续费留存合约: withdrawTxHash={}", withdrawTxHash);
        }

        return rewards;
    }

    /** batchId = keccak256("FEE_DIVIDEND_" + withdrawTxHash) */
    private String generateBatchId(String withdrawTxHash) {
        String data = "FEE_DIVIDEND_" + withdrawTxHash;
        byte[] hash = org.web3j.crypto.Hash.sha3(data.getBytes());
        return Numeric.toHexString(hash);
    }
}
