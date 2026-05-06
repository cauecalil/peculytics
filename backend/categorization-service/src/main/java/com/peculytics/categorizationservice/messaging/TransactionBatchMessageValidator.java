package com.peculytics.categorizationservice.messaging;

import org.springframework.stereotype.Component;

@Component
public class TransactionBatchMessageValidator {
    private static final int MAX_TRANSACTIONS_PER_BATCH = 50;

    public void validate(TransactionBatchMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Transaction batch message is required.");
        }

        if (message.analysisId() == null) {
            throw new IllegalArgumentException("Transaction batch must contain analysisId.");
        }

        if (message.statementFileId() == null) {
            throw new IllegalArgumentException("Transaction batch must contain statementFileId.");
        }

        if (message.batchNumber() <= 0) {
            throw new IllegalArgumentException("Transaction batch number must be greater than zero.");
        }

        if (message.totalBatches() <= 0) {
            throw new IllegalArgumentException("Transaction batch total must be greater than zero.");
        }

        if (message.batchNumber() > message.totalBatches()) {
            throw new IllegalArgumentException("Transaction batch number must not exceed total batches.");
        }

        if (message.transactions() == null) {
            throw new IllegalArgumentException("Transaction batch transactions are required.");
        }

        if (message.transactions().isEmpty()) {
            throw new IllegalArgumentException("Transaction batch must not be empty.");
        }

        if (message.transactions().size() > MAX_TRANSACTIONS_PER_BATCH) {
            throw new IllegalArgumentException("Transaction batch must contain at most 50 transactions.");
        }

        for (int index = 0; index < message.transactions().size(); index++) {
            validateTransaction(message.transactions().get(index), index);
        }
    }

    private static void validateTransaction(TransactionBatchMessage.TransactionMessage transaction, int index) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction batch transaction " + index + " is required.");
        }

        if (transaction.description() == null || transaction.description().isBlank()) {
            throw new IllegalArgumentException("Transaction batch transaction " + index + " must contain description.");
        }

        if (transaction.amount() == null) {
            throw new IllegalArgumentException("Transaction batch transaction " + index + " must contain amount.");
        }

        if (transaction.transactionDate() == null) {
            throw new IllegalArgumentException("Transaction batch transaction " + index + " must contain transactionDate.");
        }
    }
}
