package com.peculytics.uploadservice.messaging;

public interface TransactionBatchPublisher {
    void publish(TransactionBatchMessage message);
}
