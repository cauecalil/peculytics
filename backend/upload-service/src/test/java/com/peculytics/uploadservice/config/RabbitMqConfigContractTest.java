package com.peculytics.uploadservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqConfigContractTest {
    @Test
    void shouldUseCanonicalTransactionQueueAndDeadLetterQueueNames() {
        RabbitMqConfig config = new RabbitMqConfig();

        Queue queue = config.transactionsCategorizeQueue();
        Queue dlq = config.transactionsCategorizeDlq();

        assertThat(RabbitMqConfig.TRANSACTIONS_CATEGORIZE_QUEUE).isEqualTo("transactions.categorize");
        assertThat(RabbitMqConfig.TRANSACTIONS_CATEGORIZE_DLQ).isEqualTo("transactions.categorize.dlq");
        assertThat(queue.getName()).isEqualTo("transactions.categorize");
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "")
                .containsEntry("x-dead-letter-routing-key", "transactions.categorize.dlq");
        assertThat(dlq.getName()).isEqualTo("transactions.categorize.dlq");
    }
}
