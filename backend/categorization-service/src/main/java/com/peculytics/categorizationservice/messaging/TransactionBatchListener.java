package com.peculytics.categorizationservice.messaging;

import com.peculytics.categorizationservice.config.RabbitMqConfig;
import com.peculytics.categorizationservice.service.TransactionBatchProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionBatchListener {
    private final TransactionBatchProcessorService processor;

    @RabbitListener(queues = RabbitMqConfig.TRANSACTIONS_CATEGORIZE_QUEUE)
    public void listen(TransactionBatchMessage message) {
        processor.process(message);
    }
}
