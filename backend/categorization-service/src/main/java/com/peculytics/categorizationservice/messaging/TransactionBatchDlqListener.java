package com.peculytics.categorizationservice.messaging;

import com.peculytics.categorizationservice.config.RabbitMqConfig;
import com.peculytics.categorizationservice.service.DeadLetterBatchFailureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionBatchDlqListener {
    private final DeadLetterFailureReasonExtractor failureReasonExtractor;
    private final DeadLetterBatchFailureHandler failureHandler;

    @RabbitListener(queues = RabbitMqConfig.TRANSACTIONS_CATEGORIZE_DLQ)
    public void listen(TransactionBatchMessage message, @Headers Map<String, Object> headers) {
        String failureReason = failureReasonExtractor.extract(headers);

        if (message == null || message.analysisId() == null || message.statementFileId() == null) {
            log.warn(
                    "Ignoring malformed dead-lettered transaction categorization batch: analysisId={} statementFileId={} batchNumber={} failureReason={}",
                    message == null ? null : message.analysisId(),
                    message == null ? null : message.statementFileId(),
                    message == null ? null : message.batchNumber(),
                    failureReason
            );

            return;
        }

        failureHandler.handle(message, failureReason);
    }
}
