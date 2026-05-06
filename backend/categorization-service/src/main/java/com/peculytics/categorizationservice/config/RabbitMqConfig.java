package com.peculytics.categorizationservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String TRANSACTIONS_CATEGORIZE_QUEUE = "transactions.categorize";
    public static final String TRANSACTIONS_CATEGORIZE_DLQ = "transactions.categorize.dlq";

    @Bean
    Queue transactionsCategorizeQueue() {
        return QueueBuilder.durable(TRANSACTIONS_CATEGORIZE_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(TRANSACTIONS_CATEGORIZE_DLQ)
                .build();
    }

    @Bean
    Queue transactionsCategorizeDlq() {
        return QueueBuilder.durable(TRANSACTIONS_CATEGORIZE_DLQ).build();
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
