package com.zmyc.listener.indexer.processor;

import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.listener.indexer.TransactionInfo;
import com.zmyc.service.LpRewardReleaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * 监听 Trade 合约的 RemoveLiquidity 事件
 * event RemoveLiquidity(address indexed user, uint256 liquidity, uint256 usdcOut, uint256 tipToDividend)
 */
@Slf4j
@Component
public class RemoveLiquidityProcessor implements EventProcessor<RemoveLiquidityProcessor.RemoveLiquidityModel> {

    @Autowired
    private LpRewardReleaseService lpRewardReleaseService;

    @Override
    public Event getEvent() {
        return new Event("RemoveLiquidity",
                Arrays.asList(
                        new TypeReference<Address>(true) {},  // user (indexed)
                        new TypeReference<Uint256>() {},      // liquidity
                        new TypeReference<Uint256>() {},      // usdcOut
                        new TypeReference<Uint256>() {}       // tipToDividend
                ));
    }

    @Override
    public RemoveLiquidityModel getModel(EventLog eventLog, TransactionInfo transactionInfo) {
        String[] topics = eventLog.getTopics().split(",");
        String userAddress = "0x" + topics[1].substring(26);

        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                eventLog.getData(),
                getEvent().getNonIndexedParameters()
        );

        BigInteger liquidity = (BigInteger) nonIndexedValues.get(0).getValue();
        BigInteger usdcOut = (BigInteger) nonIndexedValues.get(1).getValue();
        BigInteger tipToDividend = (BigInteger) nonIndexedValues.get(2).getValue();

        BigDecimal liquidityDecimal = new BigDecimal(liquidity).divide(new BigDecimal(BigInteger.TEN.pow(18)));
        BigDecimal usdcOutDecimal = new BigDecimal(usdcOut).divide(new BigDecimal(BigInteger.TEN.pow(6)));
        BigDecimal tipToDividendDecimal = new BigDecimal(tipToDividend).divide(new BigDecimal(BigInteger.TEN.pow(18)));

        RemoveLiquidityModel model = new RemoveLiquidityModel();
        model.userAddress = userAddress;
        model.liquidity = liquidityDecimal;
        model.usdcOut = usdcOutDecimal;
        model.tipToDividend = tipToDividendDecimal;
        model.txHash = eventLog.getTransactionHash();

        return model;
    }

    @Override
    public void doBusinessLogic(Object model) {
        RemoveLiquidityModel m = (RemoveLiquidityModel) model;
        log.info("处理移除LP事件: user={}, liquidity={}, usdcOut={}, tipToDividend={}, txHash={}",
                m.userAddress, m.liquidity, m.usdcOut, m.tipToDividend, m.txHash);

        try {
            lpRewardReleaseService.createReleaseSchedule(
                    m.userAddress, m.tipToDividend, m.txHash);
        } catch (Exception e) {
            log.error("处理移除LP事件失败: txHash={}", m.txHash, e);
            throw e;
        }
    }

    public static class RemoveLiquidityModel {
        public String userAddress;
        public BigDecimal liquidity;
        public BigDecimal usdcOut;
        public BigDecimal tipToDividend;
        public String txHash;
    }
}
