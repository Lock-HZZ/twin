package com.zmyc.listener.indexer.processor;

import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.listener.indexer.TransactionInfo;
import com.zmyc.listener.indexer.model.UnStakeModel;
import com.zmyc.service.UserStakeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigInteger;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnStakeProcessor implements EventProcessor<UnStakeModel> {

    private final UserStakeService stakeService;

    @Override
    public Event getEvent() {
        return new Event(
                "Withdrawn",
                List.of(
                        new TypeReference<Uint256>(true) {},  // stakeId (indexed)
                        new TypeReference<Address>(true) {}   // user (indexed)
                )
        );
    }

    @Override
    public UnStakeModel getModel(EventLog eventLog, TransactionInfo transactionInfo) {
        Transaction transaction = transactionInfo.getTransaction();
        String hash = transaction.getHash();

        String[] topics = eventLog.getTopics().split(",");
        Long stakeId = new BigInteger(topics[1].substring(2), 16).longValue();
        String userAddress = "0x" + topics[2].substring(26);

        return new UnStakeModel(stakeId, userAddress, hash);
    }

    @Override
    public void doBusinessLogic(Object model) {
        UnStakeModel m = (UnStakeModel) model;
        log.info("Processing Withdrawn event: stakeId={}, user={}, hash={}", m.getStakeId(), m.getUser(), m.getTxHash());
        stakeService.recordWithdrawFromChain(m.getStakeId(), m.getTxHash());
    }
}
