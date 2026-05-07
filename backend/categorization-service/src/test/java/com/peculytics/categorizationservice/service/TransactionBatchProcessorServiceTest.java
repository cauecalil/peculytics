package com.peculytics.categorizationservice.service;

import com.peculytics.categorizationservice.categorization.BatchCategorizationService;
import com.peculytics.categorizationservice.categorization.CategorizationResult;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessageValidator;
import com.peculytics.categorizationservice.model.TransactionCategory;
import com.peculytics.categorizationservice.model.TransactionCategorySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionBatchProcessorServiceTest {
    @Mock
    private TransactionBatchMessageValidator transactionBatchMessageValidator;

    @Mock
    private BatchCategorizationService batchCategorizationService;

    @Mock
    private TransactionBatchPersistenceService transactionBatchPersistenceService;

    @InjectMocks
    private TransactionBatchProcessorService service;

    @Test
    void shouldValidateBeforeCategorizingAndPersisting() {
        TransactionBatchMessage message = message();
        Map<Integer, CategorizationResult> results = Map.of(0, result());
        when(transactionBatchPersistenceService.isProcessed(message)).thenReturn(false);
        when(batchCategorizationService.categorize(message)).thenReturn(results);

        service.process(message);

        InOrder inOrder = inOrder(
                transactionBatchMessageValidator,
                batchCategorizationService,
                transactionBatchPersistenceService
        );
        inOrder.verify(transactionBatchMessageValidator).validate(message);
        inOrder.verify(transactionBatchPersistenceService).isProcessed(message);
        inOrder.verify(batchCategorizationService).categorize(message);
        inOrder.verify(transactionBatchPersistenceService).persistProcessedBatch(message, results);
    }

    @Test
    void shouldNotCategorizeOrPersistWhenBatchWasAlreadyProcessed() {
        TransactionBatchMessage message = message();
        when(transactionBatchPersistenceService.isProcessed(message)).thenReturn(true);

        service.process(message);

        InOrder inOrder = inOrder(transactionBatchMessageValidator, transactionBatchPersistenceService);
        inOrder.verify(transactionBatchMessageValidator).validate(message);
        inOrder.verify(transactionBatchPersistenceService).isProcessed(message);
        verifyNoInteractions(batchCategorizationService);
        verify(transactionBatchPersistenceService, never()).persistProcessedBatch(any(), any());
    }

    @Test
    void shouldNotCategorizeOrPersistWhenValidationFails() {
        TransactionBatchMessage message = message();
        doThrow(new IllegalArgumentException("invalid"))
                .when(transactionBatchMessageValidator)
                .validate(message);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.process(message))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(batchCategorizationService, transactionBatchPersistenceService);
    }

    private static CategorizationResult result() {
        return CategorizationResult.builder()
                .category(TransactionCategory.FOOD)
                .source(TransactionCategorySource.RULE)
                .build();
    }

    private static TransactionBatchMessage message() {
        return TransactionBatchMessage.builder()
                .analysisId(UUID.randomUUID())
                .statementFileId(UUID.randomUUID())
                .batchNumber(1)
                .totalBatches(1)
                .transactions(List.of(TransactionBatchMessage.TransactionMessage.builder()
                        .description("Coffee")
                        .amount(new BigDecimal("9.90"))
                        .transactionDate(LocalDate.of(2026, 5, 5))
                        .build()))
                .build();
    }
}
