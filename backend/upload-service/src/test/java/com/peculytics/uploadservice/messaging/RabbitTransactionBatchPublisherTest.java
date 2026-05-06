package com.peculytics.uploadservice.messaging;

import com.peculytics.uploadservice.config.RabbitMqConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitTransactionBatchPublisherTest {
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitTransactionBatchPublisher publisher;

    @Test
    void shouldPublishMessageWhenBrokerAcknowledgesIt() {
        TransactionBatchMessage message = message();
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(2);
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.TRANSACTIONS_CATEGORIZE_QUEUE),
                same(message),
                any(CorrelationData.class)
        );

        publisher.publish(message);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.TRANSACTIONS_CATEGORIZE_QUEUE),
                same(message),
                any(CorrelationData.class)
        );
    }

    @Test
    void shouldRejectPublishWhenBrokerDoesNotAcknowledgeMessage() {
        TransactionBatchMessage message = message();
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(2);
            correlationData.getFuture().complete(new CorrelationData.Confirm(false, "nack reason"));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.TRANSACTIONS_CATEGORIZE_QUEUE),
                same(message),
                any(CorrelationData.class)
        );

        assertThatThrownBy(() -> publisher.publish(message))
                .isInstanceOf(AmqpException.class)
                .hasMessageContaining("not acknowledged")
                .hasMessageContaining("nack reason");
    }

    @Test
    void shouldRejectPublishWhenBrokerReturnsMessage() {
        TransactionBatchMessage message = message();
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(2);
            correlationData.setReturned(new ReturnedMessage(
                    new Message(new byte[0], new MessageProperties()),
                    312,
                    "NO_ROUTE",
                    "",
                    "transactions.categorize"
            ));
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.TRANSACTIONS_CATEGORIZE_QUEUE),
                same(message),
                any(CorrelationData.class)
        );

        assertThatThrownBy(() -> publisher.publish(message))
                .isInstanceOf(AmqpException.class)
                .hasMessageContaining("was returned by broker")
                .hasMessageContaining("NO_ROUTE");
    }

    private static TransactionBatchMessage message() {
        return TransactionBatchMessage.builder()
                .analysisId(UUID.randomUUID())
                .statementFileId(UUID.randomUUID())
                .batchNumber(1)
                .totalBatches(1)
                .transactions(List.of(TransactionBatchMessage.TransactionMessage.builder()
                        .description("Coffee")
                        .amount(new BigDecimal("10.00"))
                        .transactionDate(LocalDate.of(2026, 5, 6))
                        .build()))
                .build();
    }
}
