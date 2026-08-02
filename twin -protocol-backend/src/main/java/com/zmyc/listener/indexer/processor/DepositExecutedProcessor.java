package com.zmyc.listener.indexer.processor;

import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.common.enums.Decimals;
import com.zmyc.infrastructure.entity.UserDepositDO;
import com.zmyc.infrastructure.repository.UserDepositRepository;
import com.zmyc.listener.indexer.TransactionInfo;
import com.zmyc.listener.indexer.model.DepositExecutedModel;
import com.zmyc.service.RewardService;
import com.zmyc.service.UserDepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Event;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositExecutedProcessor implements EventProcessor<DepositExecutedModel>{

    @Autowired
    private UserDepositService depositService;

    @Autowired
    private UserDepositRepository depositRepository;

    @Autowired
    private RewardService rewardService;

    @Override
    public Event getEvent() {
        return new Event(
                "DepositExecuted",
                List.of(
                        new TypeReference<Address>(true) {}, // user
                        new TypeReference<Uint256>(true) {}, //nonce
                        new TypeReference<Uint256>() {}, // usdcAmount
                        new TypeReference<Uint256>() {}, // toDividend
                        new TypeReference<Uint256>() {}, // tipBought
                        new TypeReference<Uint256>() {}, // liquidity
                        new TypeReference<Uint256>() {}, // dustUSDC
                        new TypeReference<Uint256>() {}  // dustTIP
                )
        );
    }

    @Override
    public DepositExecutedModel getModel(EventLog eventLog, TransactionInfo transactionInfo) {
        Transaction transaction = transactionInfo.getTransaction();
        String hash = transaction.getHash();

        String[] topics = eventLog.getTopics().split(",");

        String userAddress = "0x" + topics[1].substring(26);
        Long nonce = new BigInteger(topics[2].substring(2), 16).longValue();

        String data = eventLog.getData();
        List<Type> decodedValues = FunctionReturnDecoder.decode(
                data,
                getEvent().getNonIndexedParameters()
        );
        BigInteger usdcAmount = ((Uint256) decodedValues.get(0)).getValue();
        BigInteger toDividend = ((Uint256) decodedValues.get(1)).getValue();

        BigInteger tipBought = ((Uint256) decodedValues.get(2)).getValue();
        BigInteger liquidity = ((Uint256) decodedValues.get(3)).getValue();

        BigDecimal usdcAmountDecimal= new BigDecimal(usdcAmount).divide(Decimals.USDC.value, 8, RoundingMode.DOWN);
        BigDecimal toDividendDecimal = new BigDecimal(toDividend).divide(Decimals.USDC.value, 8, RoundingMode.DOWN);
        BigDecimal tipBoughtDecimal = new BigDecimal(tipBought).divide(Decimals.TIP.value, 8, RoundingMode.DOWN);
        BigDecimal liquidityDecimal = new BigDecimal(liquidity).divide(Decimals.TIP.value, 8, RoundingMode.DOWN);
        return new DepositExecutedModel(userAddress, nonce, usdcAmountDecimal, toDividendDecimal, tipBoughtDecimal, liquidityDecimal, hash);
    }

    @Override
    public void doBusinessLogic(Object model) {
        DepositExecutedModel depositExecutedModel = (DepositExecutedModel) model;
        String txHash = depositExecutedModel.getTxHash();
        log.info("Processing DepositExecutedModel event: hash={}", txHash);

        depositService.processDeposit(depositExecutedModel.getNonce(),
                depositExecutedModel.getLiquidity(), depositExecutedModel.getUsdcAmount(), txHash);

        if (depositExecutedModel.getToDividend() == null ||
                depositExecutedModel.getToDividend().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }

        UserDepositDO deposit = depositRepository.findByTxHash(txHash);
        if (deposit == null) {
            log.warn("入金分红跳过：找不到deposit记录, txHash={}", txHash);
            return;
        }

        rewardService.distributeDepositDividend(depositExecutedModel.getUser(),
                deposit.getUserId(), depositExecutedModel.getToDividend(), deposit.getId());
    }

}
