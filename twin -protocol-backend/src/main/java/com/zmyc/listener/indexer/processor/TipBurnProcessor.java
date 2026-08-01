package com.zmyc.listener.indexer.processor;

import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.common.config.BurnConfig;
import com.zmyc.common.enums.Decimals;
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
        return new Event("burnPool", List.of(
                new TypeReference<Uint256>() {},   // burnAmount
                new TypeReference<Uint256>() {},   // toBurn
                new TypeReference<Uint256>() {}    // toDividend
        ));
    }

    /**
     * 解析燃烧分红事件，返回 TipBurnModel
     */
    @Override
    public TipBurnModel getModel(EventLog eventLog, TransactionInfo transactionInfo) {
        List<Type> decoded = FunctionReturnDecoder.decode(
                eventLog.getData(), getEvent().getNonIndexedParameters());

        BigDecimal burnAmount = new BigDecimal(((Uint256) decoded.get(0)).getValue())
                .divide(Decimals.TIP.value, 18, RoundingMode.DOWN);

        BigDecimal toDividendAmount = new BigDecimal(((Uint256) decoded.get(2)).getValue())
                .divide(Decimals.TIP.value, 18, RoundingMode.DOWN);

        String txHash = transactionInfo.getTransaction().getHash();
        log.info("检测到燃烧分红事件: txHash={}, toDividend={}", txHash, toDividendAmount);

        return new TipBurnModel(toDividendAmount, txHash, burnAmount);
    }

    @Override
    public void doBusinessLogic(Object obj) {
        TipBurnModel model = (TipBurnModel) obj;
        TipBurnRecordDO tipBurnRecordDO = burnRecordRepository.findByTxHash(model.getTxHash());
        tipBurnRecordDO.setBurnAmount(model.getBurnAmount());
        tipBurnRecordDO.setDividendAmount(model.getToDividendAmount());
        burnRecordRepository.save(tipBurnRecordDO);
        rewardService.distributeBurnDividend(tipBurnRecordDO);
    }
}
