package com.zmyc.listener.indexer.processor;

import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.common.config.BurnConfig;
import com.zmyc.infrastructure.entity.TipBurnRecordDO;
import com.zmyc.infrastructure.repository.TipBurnRecordRepository;
import com.zmyc.listener.indexer.TransactionInfo;
import com.zmyc.listener.indexer.model.TipBurnModel;
import com.zmyc.service.RewardService;
import com.zmyc.service.TipBurnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 监听 TIP 合约的 Transfer(pair → dividendAddress) 事件
 * burnPoolTokens 会将分红部分从 pair 转到 dividendAddress，
 * 该 Transfer 是燃烧成功的唯一链上信号（destroyFromLP 不会转到 dividendAddress）
 *
 * 注意：需要在 bamboo 配置中将 TIP 合约注册为被扫描合约
 */
@org.springframework.stereotype.Component
@RequiredArgsConstructor
@Slf4j
public class TipBurnProcessor implements EventProcessor<TipBurnModel> {

    private final RewardService rewardService;
    private final TipBurnRecordRepository burnRecordRepository;

    @Override
    public Event getEvent() {
        return new Event("Transfer", List.of(
                new TypeReference<Address>(true) {},   // from (indexed)
                new TypeReference<Address>(true) {},   // to   (indexed)
                new TypeReference<Uint256>()      {}   // value
        ));
    }

    /**
     * topics[0] = event sig hash
     * topics[1] = from (pair address, 32-byte padded)
     * topics[2] = to   (dividendAddress, 32-byte padded)
     * data       = value (toDividendAmount in wei)
     *
     * 只处理 to == dividendContractAddress 的 Transfer（即燃烧分红转账）
     * 其他 Transfer 返回 null，doBusinessLogic 跳过
     */
    @Override
    public TipBurnModel getModel(EventLog eventLog, TransactionInfo transactionInfo) {
        String[] topics = eventLog.getTopics().split(",");
        if (topics.length < 3) {
            return null;
        }

        List<Type> decoded = FunctionReturnDecoder.decode(
                eventLog.getData(), getEvent().getNonIndexedParameters());

        BigDecimal toDividendAmount = new BigDecimal(((Uint256) decoded.get(0)).getValue())
                .divide(new BigDecimal("1000000000000000000"), 18, RoundingMode.DOWN);

        String txHash = transactionInfo.getTransaction().getHash();
        log.info("检测到燃烧分红Transfer事件: txHash={}, toDividend={}", txHash, toDividendAmount);

        return new TipBurnModel(toDividendAmount, txHash);
    }

    @Override
    public void doBusinessLogic(Object obj) {
        TipBurnModel model = (TipBurnModel) obj;
        TipBurnRecordDO tipBurnRecordDO = burnRecordRepository.findByTxHash(model.getTxHash());
        rewardService.distributeBurnDividend(tipBurnRecordDO);
    }
}
