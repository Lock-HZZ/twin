package com.zmyc.listener.indexer;

import com.zmyc.common.enums.ErrorCode;
import com.zmyc.common.exception.BusinessException;
import com.zmyc.listener.indexer.processor.EventProcessor;
import com.zmyc.bamboo.core.engine.dispatcher.subscriber.AbstractMessageSubscriber;
import com.zmyc.bamboo.core.model.EventLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EventLogMessageSubscriber extends AbstractMessageSubscriber {
    private final Map<BigInteger, Web3j> chainWeb3jMap;
    private final List<EventProcessor<?>> eventProcessors;
    private final EventLogProcessingService eventLogProcessingService;

    public EventLogMessageSubscriber(
            Map<BigInteger, Web3j> chainWeb3jMap,
            List<EventProcessor<?>> eventProcessors,
            EventLogProcessingService eventLogProcessingService
    ) {
        this.chainWeb3jMap = chainWeb3jMap;
        this.eventProcessors = eventProcessors;
        this.eventLogProcessingService = eventLogProcessingService;
    }

    @Override
    public boolean isSupport(Class<?> clazz) {
        return EventLog.class.isAssignableFrom(clazz);
    }

    @Override
    public void consume(Object message) {
        EventLog eventLog = (EventLog) message;
        if (log.isDebugEnabled()) {
            log.info("Received event log: {}", eventLog);
        }

        String transactionHash = eventLog.getTransactionHash();
        String eventSignatureHash = eventLog.getTopics().split(",")[0];

        EventProcessor<?> targetEventProcessor = eventProcessors.stream()
                .filter(it -> it.getSignatureHash().equalsIgnoreCase(eventSignatureHash))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_PROCESSOR_NOT_FOUND, eventSignatureHash));

        TransactionInfo transactionInfo = getTransactionInfo(eventLog.getChainId(), transactionHash);

        eventLogProcessingService.processAndMark(targetEventProcessor, eventLog, transactionInfo);
    }

    private TransactionInfo getTransactionInfo(BigInteger chainId, String transactionHash) {
        Web3j web3j = chainWeb3jMap.get(chainId);
        try {
            TransactionReceipt transactionReceiptResponse  = waitForEthConfirmation(web3j, transactionHash);
            if (transactionReceiptResponse == null ||  !transactionReceiptResponse.isStatusOK()) {
                throw new BusinessException(ErrorCode.TRANSACTION_STATUS_FAILED, transactionHash);
            }

            var transactionRequest = web3j.ethGetTransactionByHash(transactionHash);

            var transactionResponse = transactionRequest.send();
            var transaction = transactionResponse.getTransaction().orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND, transactionHash));

            return new TransactionInfo(transactionReceiptResponse, transaction);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待交易确认时被中断", e);
        }
    }

    public TransactionReceipt waitForEthConfirmation(Web3j web3j, String txHash)
            throws InterruptedException {

        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash)
                        .send()
                        .getTransactionReceipt()
                        .orElse(null);

                if (receipt != null) {
                    log.info("交易 {} 已确认，状态: {}", txHash, receipt.getStatus());
                    return receipt;
                }

                log.info("第  次获取交易 {} 回执为空，2秒后重试（共 {} 次）...", attempt, txHash, maxRetries);
            } catch (Exception e) {
                log.info("第 {} 次获取交易 {} 回执时出错: {}，2秒后重试（共 {} 次）", attempt, txHash, e.getMessage(), maxRetries);
            }

            if (attempt < maxRetries) {
                Thread.sleep(2000);
            }
        }

        log.error("在尝试 {} 次后仍未获取到交易 {} 的回执，交易可能仍在处理中", maxRetries, txHash);
        return null;
    }
}
