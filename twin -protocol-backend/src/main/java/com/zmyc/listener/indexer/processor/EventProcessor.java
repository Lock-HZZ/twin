package com.zmyc.listener.indexer.processor;

import com.zmyc.listener.indexer.TransactionInfo;
import com.zmyc.bamboo.core.model.EventLog;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.datatypes.Event;

public interface EventProcessor<Model> {
    Event getEvent();

    default String getSignatureHash() {
        var event = getEvent();
        return EventEncoder.encode(event);
    }

    Model getModel(EventLog eventLog, TransactionInfo transactionInfo);

    void doBusinessLogic(Object model);
}
