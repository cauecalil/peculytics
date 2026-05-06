package com.peculytics.categorizationservice.service;

import com.peculytics.categorizationservice.categorization.CategorizationResult;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.Analysis;
import com.peculytics.categorizationservice.model.StatementFile;
import com.peculytics.categorizationservice.model.Transaction;
import com.peculytics.categorizationservice.model.TransactionCategory;
import com.peculytics.categorizationservice.model.TransactionCategorySource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionFactoryTest {
    private final TransactionFactory factory = new TransactionFactory();

    @Test
    void shouldCreateTransactionWithExpectedFields() {
        Analysis analysis = new Analysis();
        StatementFile statementFile = new StatementFile();
        TransactionBatchMessage message = message(transaction("Market", "32.40", LocalDate.of(2026, 5, 1)));
        CategorizationResult categorization = result(TransactionCategory.GROCERIES, TransactionCategorySource.RULE);

        List<Transaction> transactions = factory.createAll(message, analysis, statementFile, Map.of(0, categorization));

        assertThat(transactions).hasSize(1);
        Transaction created = transactions.getFirst();
        assertThat(created.getAnalysis()).isSameAs(analysis);
        assertThat(created.getStatementFile()).isSameAs(statementFile);
        assertThat(created.getDescription()).isEqualTo("Market");
        assertThat(created.getAmount()).isEqualByComparingTo("32.40");
        assertThat(created.getTransactionDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(created.getCategory()).isEqualTo(TransactionCategory.GROCERIES);
        assertThat(created.getCategorySource()).isEqualTo(TransactionCategorySource.RULE);
        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldCreateOneTransactionPerMessageTransaction() {
        TransactionBatchMessage message = message(
                transaction("Market", "32.40", LocalDate.of(2026, 5, 1)),
                transaction("Bus", "4.80", LocalDate.of(2026, 5, 2))
        );

        List<Transaction> transactions = factory.createAll(
                message,
                new Analysis(),
                new StatementFile(),
                Map.of(
                        0, result(TransactionCategory.GROCERIES, TransactionCategorySource.RULE),
                        1, result(TransactionCategory.TRANSPORT, TransactionCategorySource.AI)
                )
        );

        assertThat(transactions).hasSize(2);
    }

    private static CategorizationResult result(
            TransactionCategory category,
            TransactionCategorySource source
    ) {
        return CategorizationResult.builder()
                .category(category)
                .source(source)
                .build();
    }

    private static TransactionBatchMessage message(TransactionBatchMessage.TransactionMessage... transactions) {
        return TransactionBatchMessage.builder()
                .analysisId(UUID.randomUUID())
                .statementFileId(UUID.randomUUID())
                .batchNumber(1)
                .totalBatches(1)
                .transactions(List.of(transactions))
                .build();
    }

    private static TransactionBatchMessage.TransactionMessage transaction(
            String description,
            String amount,
            LocalDate date
    ) {
        return TransactionBatchMessage.TransactionMessage.builder()
                .description(description)
                .amount(new BigDecimal(amount))
                .transactionDate(date)
                .build();
    }
}
