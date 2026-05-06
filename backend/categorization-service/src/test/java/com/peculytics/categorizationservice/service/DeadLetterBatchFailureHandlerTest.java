package com.peculytics.categorizationservice.service;

import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.Analysis;
import com.peculytics.categorizationservice.model.StatementFile;
import com.peculytics.categorizationservice.repository.AnalysisRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterBatchFailureHandlerTest {
    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private StatementFileRepository statementFileRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DeadLetterBatchFailureHandler handler;

    @Test
    void shouldNotSaveAnythingWhenAnalysisDoesNotExist() {
        TransactionBatchMessage message = message(UUID.randomUUID(), UUID.randomUUID());
        when(analysisRepository.findByIdForUpdate(message.analysisId())).thenReturn(Optional.empty());

        handler.handle(message, "failure");

        verify(analysisRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(statementFileRepository, transactionRepository);
    }

    @Test
    void shouldNotSaveAnythingWhenStatementFileDoesNotExist() {
        TransactionBatchMessage message = message(UUID.randomUUID(), UUID.randomUUID());
        Analysis analysis = mock(Analysis.class);
        when(analysisRepository.findByIdForUpdate(message.analysisId())).thenReturn(Optional.of(analysis));
        when(statementFileRepository.findByIdForUpdate(message.statementFileId())).thenReturn(Optional.empty());

        handler.handle(message, "failure");

        verify(analysisRepository, never()).save(analysis);
        verify(statementFileRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldMarkAnalysisAsUnrecoverableWhenStatementFileDoesNotBelongToAnalysis() {
        UUID analysisId = UUID.randomUUID();
        UUID statementFileId = UUID.randomUUID();
        Analysis analysis = analysis(analysisId);
        StatementFile statementFile = statementFile(analysis(UUID.randomUUID()));
        TransactionBatchMessage message = message(analysisId, statementFileId);
        when(analysisRepository.findByIdForUpdate(analysisId)).thenReturn(Optional.of(analysis));
        when(statementFileRepository.findByIdForUpdate(statementFileId)).thenReturn(Optional.of(statementFile));

        handler.handle(message, "routing failure");

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(analysis).registerUnrecoverableFailedBatch(errorCaptor.capture());
        assertThat(errorCaptor.getValue())
                .startsWith("Categorization batch failed: Statement file does not belong to analysis")
                .contains("routing failure");
        verify(analysisRepository).save(analysis);
        verify(statementFileRepository, never()).save(statementFile);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldMarkStatementFileAndAnalysisAsFailedWhenEntitiesAreLinked() {
        UUID analysisId = UUID.randomUUID();
        UUID statementFileId = UUID.randomUUID();
        Analysis analysis = analysis(analysisId);
        StatementFile statementFile = statementFile(analysis);
        TransactionBatchMessage message = message(analysisId, statementFileId);
        when(analysisRepository.findByIdForUpdate(analysisId)).thenReturn(Optional.of(analysis));
        when(statementFileRepository.findByIdForUpdate(statementFileId)).thenReturn(Optional.of(statementFile));
        when(transactionRepository.countByAnalysisId(analysisId)).thenReturn(0L);

        handler.handle(message, "consumer failed");

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(statementFile).registerFailedBatch(errorCaptor.capture());
        String errorMessage = errorCaptor.getValue();
        assertThat(errorMessage).isEqualTo("Categorization batch failed: consumer failed");
        verify(analysis).registerFailedBatch(false, errorMessage);
        verify(statementFileRepository).save(statementFile);
        verify(analysisRepository).save(analysis);
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
