package com.zmyc.listener.indexer.processor;

import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.common.enums.Decimals;
import com.zmyc.listener.indexer.TransactionInfo;
import com.zmyc.listener.indexer.model.DividendWithdrawModel;
import com.zmyc.service.FeeDividendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

/**
 * 监听 Dividend 合约 Withdraw 事件
 * 仅处理 USDC 提现（assetType=0）且 fee>0 的情况
 * 触发后将手续费按比例分配给节点(40%)和合伙人(30%)
 *
 * event Withdraw(address indexed user, uint8 indexed rewardType,
 *                uint8 indexed assetType, uint256 amount, uint256 fee)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DividendWithdrawProcessor implements EventProcessor<DividendWithdrawModel> {

    @Autowired
    private FeeDividendService feeDividendService;


    @Override
    public Event getEvent() {
        return new Event(
                "Withdraw",
                List.of(
                        new TypeReference<Address>(true) {},   // user (indexed)
                        new TypeReference<Uint8>(true) {},     // rewardType (indexed)
                        new TypeReference<Uint8>(true) {},     // assetType (indexed)
                        new TypeReference<Uint256>() {},        // amount
                        new TypeReference<Uint256>() {}         // fee
                )
        );
    }

    @Override
    public DividendWithdrawModel getModel(EventLog eventLog, TransactionInfo transactionInfo) {
        Transaction transaction = transactionInfo.getTransaction();
        String txHash = transaction.getHash();

        String[] topics = eventLog.getTopics().split(",");
        String user = "0x" + topics[1].substring(26);
        int rewardType = new BigInteger(topics[2].substring(2), 16).intValue();
        int assetType = new BigInteger(topics[3].substring(2), 16).intValue();

        List<Type> decoded = FunctionReturnDecoder.decode(
                eventLog.getData(), getEvent().getNonIndexedParameters());

        BigDecimal amount = new BigDecimal(((Uint256) decoded.get(0)).getValue())
                .divide(Decimals.USDC.value, 8, RoundingMode.DOWN);
        BigDecimal fee = new BigDecimal(((Uint256) decoded.get(1)).getValue())
                .divide(Decimals.USDC.value, 8, RoundingMode.DOWN);

        return new DividendWithdrawModel(user, rewardType, assetType, amount, fee, txHash);
    }

    @Override
    public void doBusinessLogic(Object obj) {
        DividendWithdrawModel model = (DividendWithdrawModel) obj;

        // 只处理 USDC 提现且有手续费的情况
        if (model.getAssetType() != 0 || model.getFee().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        log.info("检测到USDC提现，触发手续费分配: txHash={}, user={}, fee={}",
                model.getTxHash(), model.getUser(), model.getFee());

        feeDividendService.distributeFee(model.getTxHash(), model.getFee());
    }
}
