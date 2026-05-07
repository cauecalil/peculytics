package com.peculytics.categorizationservice.service;

import com.peculytics.categorizationservice.categorization.BatchCategorizationService;
import com.peculytics.categorizationservice.categorization.CategorizationResult;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionBatchProcessorService {
    private final TransactionBatchMessageValidator transactionBatchMessageValidator;
    private final BatchCategorizationService batchCategorizationService;
    private final TransactionBatchPersistenceService transactionBatchPersistenceService;

    public void process(TransactionBatchMessage message) {
        transactionBatchMessageValidator.validate(message);

        if (transactionBatchPersistenceService.isProcessed(message)) {
            return;
        }

        Map<Integer, CategorizationResult> results = batchCategorizationService.categorize(message);
        transactionBatchPersistenceService.persistProcessedBatch(message, results);
    }
}
