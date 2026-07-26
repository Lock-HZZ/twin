package com.zmyc.listener.indexer;

import lombok.Data;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

@Data
public class TransactionInfo {
    private TransactionReceipt transactionReceipt;
    private Transaction transaction;

    public TransactionInfo(TransactionReceipt transactionReceipt, Transaction transaction) {
        if (transaction == null || transactionReceipt == null) {
            throw new IllegalArgumentException("missing transaction or transaction receipt");
        }

        this.transaction = transaction;
        this.transactionReceipt = transactionReceipt;
    }
}
