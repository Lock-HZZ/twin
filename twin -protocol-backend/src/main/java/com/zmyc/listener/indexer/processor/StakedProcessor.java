package com.zmyc.listener.indexer.processor;

import com.zmyc.bamboo.core.model.EventLog;
import com.zmyc.listener.indexer.TransactionInfo;
import com.zmyc.listener.indexer.model.StakedModel;
import com.zmyc.infrastructure.repository.UserRepository;
import com.zmyc.service.UserStakeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StakedProcessor implements EventProcessor<StakedModel> {

    private final UserStakeService stakeService;
    private final UserRepository userRepository;

    @Override
    public Event getEvent() {
        return new Event(
                "Staked",
                List.of(
                        new TypeReference<Uint256>(true) {},  // stakeId (indexed)
                        new TypeReference<Address>(true) {},   // user (indexed)
                        new TypeReference<Uint256>() {},       // amount
                        new TypeReference<Uint256>() {},       // plan
                        new TypeReference<Uint256>() {},       // startTime
                        new TypeReference<Uint256>() {}        // endTime
                )
        );
    }

    @Override
    public StakedModel getModel(EventLog eventLog, TransactionInfo transactionInfo) {
        Transaction transaction = transactionInfo.getTransaction();
        String hash = transaction.getHash();

        String[] topics = eventLog.getTopics().split(",");
        Long stakeId = new BigInteger(topics[1].substring(2), 16).longValue();
        String userAddress = "0x" + topics[2].substring(26);

        List<Type> decoded = FunctionReturnDecoder.decode(
                eventLog.getData(),
                getEvent().getNonIndexedParameters()
        );

        BigDecimal tipDecimal = new BigDecimal("1000000000000000000");
        BigDecimal amount = new BigDecimal(((Uint256) decoded.get(0)).getValue())
                .divide(tipDecimal, 8, RoundingMode.DOWN);
        Integer plan = ((Uint256) decoded.get(1)).getValue().intValue();
        Long startTime = ((Uint256) decoded.get(2)).getValue().longValue();
        Long endTime = ((Uint256) decoded.get(3)).getValue().longValue();

        return new StakedModel(userAddress, stakeId, amount, plan, startTime, endTime, hash);
    }

    @Override
    public void doBusinessLogic(Object model) {
        StakedModel m = (StakedModel) model;
        log.info("Processing Staked event: stakeId={}, user={}, hash={}", m.getStakeId(), m.getUser(), m.getTxHash());

        Long userId = userRepository.findByAddress(m.getUser()).getId();
        stakeService.recordStakeFromChain(userId, m.getStakeId(), m.getAmount(), m.getPlan(),
                m.getStartTime(), m.getEndTime(), m.getTxHash());
    }
}
