package com.peculytics.categorizationservice.messaging;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionBatchMessageValidatorTest {
    private final TransactionBatchMessageValidator validator = new TransactionBatchMessageValidator();

    @Test
    void shouldAcceptValidMessage() {
        TransactionBatchMessage message = validMessage(List.of(validTransaction()));

        assertThatCode(() -> validator.validate(message)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullMessage() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullAnalysisId() {
        TransactionBatchMessage message = validMessageBuilder()
                .analysisId(null)
                .build();

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullStatementFileId() {
        TransactionBatchMessage message = validMessageBuilder()
                .statementFileId(null)
                .build();

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBatchNumberLessThanOrEqualToZero() {
        TransactionBatchMessage message = validMessageBuilder()
                .batchNumber(0)
                .build();

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectTotalBatchesLessThanOrEqualToZero() {
        TransactionBatchMessage message = validMessageBuilder()
                .totalBatches(0)
                .build();

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBatchNumberGreaterThanTotalBatches() {
        TransactionBatchMessage message = validMessageBuilder()
                .batchNumber(3)
                .totalBatches(2)
                .build();

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullTransactions() {
        TransactionBatchMessage message = validMessageBuilder()
                .transactions(null)
                .build();

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectEmptyTransactions() {
        TransactionBatchMessage message = validMessage(List.of());

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMoreThanFiftyTransactions() {
        List<TransactionBatchMessage.TransactionMessage> transactions = new ArrayList<>();
        for (int index = 0; index < 51; index++) {
            transactions.add(validTransaction());
        }
        TransactionBatchMessage message = validMessage(transactions);

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullTransaction() {
        TransactionBatchMessage message = validMessage(Arrays.asList((TransactionBatchMessage.TransactionMessage) null));

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullDescription() {
        TransactionBatchMessage message = validMessage(List.of(validTransactionBuilder().description(null).build()));

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankDescription() {
        TransactionBatchMessage message = validMessage(List.of(validTransactionBuilder().description("   ").build()));

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullAmount() {
        TransactionBatchMessage message = validMessage(List.of(validTransactionBuilder().amount(null).build()));

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullTransactionDate() {
        TransactionBatchMessage message = validMessage(List.of(validTransactionBuilder().transactionDate(null).build()));

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static TransactionBatchMessage validMessage(List<TransactionBatchMessage.TransactionMessage> transactions) {
        return validMessageBuilder()
                .transactions(transactions)
                .build();
    }

    private static TransactionBatchMessage.TransactionMessage validTransaction() {
        return validTransactionBuilder().build();
    }

    private static TransactionBatchMessage.TransactionMessage.TransactionMessageBuilder validTransactionBuilder() {
        return TransactionBatchMessage.TransactionMessage.builder()
                .description("Market purchase")
                .amount(new BigDecimal("42.10"))
                .transactionDate(LocalDate.of(2026, 5, 5));
    }

    private static TransactionBatchMessage.TransactionBatchMessageBuilder validMessageBuilder() {
        return TransactionBatchMessage.builder()
                .analysisId(UUID.randomUUID())
                .statementFileId(UUID.randomUUID())
                .batchNumber(1)
                .totalBatches(1)
                .transactions(List.of(validTransaction()));
    }
}
