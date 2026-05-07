package com.peculytics.categorizationservice.service;

import com.peculytics.categorizationservice.categorization.CategorizationResult;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.Analysis;
import com.peculytics.categorizationservice.model.ProcessedTransactionBatch;
import com.peculytics.categorizationservice.model.StatementFile;
import com.peculytics.categorizationservice.model.StatementFileStatus;
import com.peculytics.categorizationservice.model.Transaction;
import com.peculytics.categorizationservice.model.TransactionCategory;
import com.peculytics.categorizationservice.model.TransactionCategorySource;
import com.peculytics.categorizationservice.repository.AnalysisRepository;
import com.peculytics.categorizationservice.repository.ProcessedTransactionBatchRepository;
import com.peculytics.categorizationservice.repository.StatementFileRepository;
import com.peculytics.categorizationservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionBatchPersistenceServiceTest {
    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private StatementFileRepository statementFileRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProcessedTransactionBatchRepository processedTransactionBatchRepository;

    @Mock
    private TransactionFactory transactionFactory;

    @InjectMocks
    private TransactionBatchPersistenceService service;

    @Test
    void shouldThrowWhenAnalysisDoesNotExist() {
        TransactionBatchMessage message = message(UUID.randomUUID(), UUID.randomUUID());
        when(analysisRepository.findByIdForUpdate(message.analysisId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.persistProcessedBatch(message, results()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(statementFileRepository, transactionRepository, transactionFactory);
    }

    @Test
    void shouldThrowWhenStatementFileDoesNotExist() {
        TransactionBatchMessage message = message(UUID.randomUUID(), UUID.randomUUID());
        Analysis analysis = mock(Analysis.class);
        when(analysisRepository.findByIdForUpdate(message.analysisId())).thenReturn(Optional.of(analysis));
        when(statementFileRepository.findByIdForUpdate(message.statementFileId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.persistProcessedBatch(message, results()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(transactionRepository, transactionFactory);
    }

    @Test
    void shouldThrowWhenStatementFileDoesNotBelongToAnalysis() {
        UUID analysisId = UUID.randomUUID();
        UUID statementFileId = UUID.randomUUID();
        UUID otherAnalysisId = UUID.randomUUID();
        TransactionBatchMessage message = message(analysisId, statementFileId);
        Analysis analysis = analysis(analysisId);
        Analysis otherAnalysis = analysis(otherAnalysisId);
        StatementFile statementFile = statementFile(otherAnalysis);
        when(analysisRepository.findByIdForUpdate(analysisId)).thenReturn(Optional.of(analysis));
        when(statementFileRepository.findByIdForUpdate(statementFileId)).thenReturn(Optional.of(statementFile));

        assertThatThrownBy(() -> service.persistProcessedBatch(message, results()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(transactionRepository, transactionFactory);
        verify(statementFileRepository, never()).save(statementFile);
        verify(analysisRepository, never()).save(analysis);
    }

    @Test
    void shouldSaveTransactionsAndUpdateProcessedBatchState() {
        UUID analysisId = UUID.randomUUID();
        UUID statementFileId = UUID.randomUUID();
        TransactionBatchMessage message = message(analysisId, statementFileId);
        Map<Integer, CategorizationResult> results = results();
        Analysis analysis = analysis(analysisId);
        StatementFile statementFile = statementFile(analysis);
        List<Transaction> transactions = List.of(mock(Transaction.class));
        when(analysisRepository.findByIdForUpdate(analysisId)).thenReturn(Optional.of(analysis));
        when(statementFileRepository.findByIdForUpdate(statementFileId)).thenReturn(Optional.of(statementFile));
        when(processedTransactionBatchRepository.existsByAnalysisIdAndStatementFileIdAndBatchNumber(analysisId, statementFileId, 1))
                .thenReturn(false);
        when(transactionFactory.createAll(message, analysis, statementFile, results)).thenReturn(transactions);
        when(statementFileRepository.countByAnalysisIdAndStatus(analysisId, StatementFileStatus.FAILED)).thenReturn(0L);

        service.persistProcessedBatch(message, results);

        verify(transactionRepository).saveAll(transactions);
        ArgumentCaptor<ProcessedTransactionBatch> processedBatchCaptor = ArgumentCaptor.forClass(ProcessedTransactionBatch.class);
        verify(processedTransactionBatchRepository).save(processedBatchCaptor.capture());
        ProcessedTransactionBatch processedBatch = processedBatchCaptor.getValue();
        assertThat(processedBatch.getAnalysis()).isSameAs(analysis);
        assertThat(processedBatch.getStatementFile()).isSameAs(statementFile);
        assertThat(processedBatch.getBatchNumber()).isEqualTo(1);
        assertThat(processedBatch.getCreatedAt()).isNotNull();
        verify(statementFile).registerProcessedBatch();
        verify(analysis).registerProcessedBatch(false);
        verify(statementFileRepository).save(statementFile);
        verify(analysisRepository).save(analysis);
    }

    @Test
    void shouldSkipAlreadyProcessedBatchWithoutSavingTransactionsOrUpdatingProcessedBatchState() {
        UUID analysisId = UUID.randomUUID();
        UUID statementFileId = UUID.randomUUID();
        TransactionBatchMessage message = message(analysisId, statementFileId);
        Analysis analysis = analysis(analysisId);
        StatementFile statementFile = statementFile(analysis);
        when(analysisRepository.findByIdForUpdate(analysisId)).thenReturn(Optional.of(analysis));
        when(statementFileRepository.findByIdForUpdate(statementFileId)).thenReturn(Optional.of(statementFile));
        when(processedTransactionBatchRepository.existsByAnalysisIdAndStatementFileIdAndBatchNumber(analysisId, statementFileId, 1))
                .thenReturn(true);

        service.persistProcessedBatch(message, results());

        verifyNoInteractions(transactionRepository, transactionFactory);
        verify(processedTransactionBatchRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(statementFile, never()).registerProcessedBatch();
        verify(analysis, never()).registerProcessedBatch(org.mockito.ArgumentMatchers.anyBoolean());
        verify(statementFileRepository, never()).countByAnalysisIdAndStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(statementFileRepository, never()).save(statementFile);
        verify(analysisRepository, never()).save(analysis);
    }

    private static Analysis analysis(UUID id) {
        Analysis analysis = mock(Analysis.class);
        when(analysis.getId()).thenReturn(id);
        return analysis;
    }

    private static StatementFile statementFile(Analysis analysis) {
        StatementFile statementFile = mock(StatementFile.class);
        when(statementFile.getAnalysis()).thenReturn(analysis);
        return statementFile;
    }

    private static Map<Integer, CategorizationResult> results() {
        return Map.of(0, CategorizationResult.builder()
                .category(TransactionCategory.FOOD)
                .source(TransactionCategorySource.RULE)
                .build());
    }

    private static TransactionBatchMessage message(UUID analysisId, UUID statementFileId) {
        return TransactionBatchMessage.builder()
                .analysisId(analysisId)
                .statementFileId(statementFileId)
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
