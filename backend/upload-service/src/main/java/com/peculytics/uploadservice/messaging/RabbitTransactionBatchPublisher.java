package com.peculytics.uploadservice.messaging;

import com.peculytics.uploadservice.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class RabbitTransactionBatchPublisher implements TransactionBatchPublisher {
    private static final long PUBLISH_CONFIRM_TIMEOUT_MILLIS = 5_000;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(TransactionBatchMessage message) {
        CorrelationData correlationData = new CorrelationData();

        rabbitTemplate.convertAndSend(RabbitMqConfig.TRANSACTIONS_CATEGORIZE_QUEUE, message, correlationData);

        CorrelationData.Confirm confirm = waitForConfirm(correlationData);
        ReturnedMessage returned = correlationData.getReturned();

        if (returned != null) {
            throw new AmqpException("Transaction batch message was returned by broker: replyCode="
                    + returned.getReplyCode()
                    + " replyText="
                    + returned.getReplyText()
                    + " exchange="
                    + returned.getExchange()
                    + " routingKey="
                    + returned.getRoutingKey());
        }

        if (confirm == null || !confirm.ack()) {
            throw new AmqpException("Transaction batch message was not acknowledged by broker: "
                    + reason(confirm));
        }
    }

    private static CorrelationData.Confirm waitForConfirm(CorrelationData correlationData) {
        try {
            return correlationData.getFuture().get(PUBLISH_CONFIRM_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Interrupted while waiting for transaction batch publish confirmation", e);
        } catch (ExecutionException e) {
            throw new AmqpException("Transaction batch publish confirmation failed", e);
        } catch (TimeoutException e) {
            throw new AmqpException("Timed out waiting for transaction batch publish confirmation", e);
        }
    }

    private static String reason(CorrelationData.Confirm confirm) {
        if (confirm == null || confirm.reason() == null || confirm.reason().isBlank()) {
            return "no reason provided";
        }

        return confirm.reason();
    }
}
