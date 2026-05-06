package com.peculytics.uploadservice.service;

import com.peculytics.uploadservice.model.Analysis;
import com.peculytics.uploadservice.model.AnalysisStatus;
import com.peculytics.uploadservice.model.StatementFile;
import com.peculytics.uploadservice.model.StatementFileStatus;
import com.peculytics.uploadservice.repository.AnalysisRepository;
import com.peculytics.uploadservice.repository.StatementFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishFailureHandlerTest {
    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private StatementFileRepository statementFileRepository;

    @InjectMocks
    private PublishFailureHandler handler;

    @Test
    void shouldMarkAnalysisAndDistinctStatementFilesAsFailed() {
        UUID analysisId = UUID.randomUUID();
        UUID statementFileId = UUID.randomUUID();
        Analysis analysis = Analysis.builder()
                .id(analysisId)
                .status(AnalysisStatus.PROCESSING)
                .build();
        StatementFile statementFile = StatementFile.builder()
                .id(statementFileId)
                .status(StatementFileStatus.PROCESSING)
                .build();
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));
        when(statementFileRepository.findAllById(List.of(statementFileId))).thenReturn(List.of(statementFile));

        handler.markPublishingFailed(analysisId, List.of(statementFileId, statementFileId), "publish failed");

        ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        verify(analysisRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysisCaptor.getValue().getErrorMessage()).isEqualTo("publish failed");
        assertThat(analysisCaptor.getValue().getCompletedAt()).isNotNull();

        ArgumentCaptor<StatementFile> statementFileCaptor = ArgumentCaptor.forClass(StatementFile.class);
        verify(statementFileRepository).save(statementFileCaptor.capture());
        assertThat(statementFileCaptor.getValue().getStatus()).isEqualTo(StatementFileStatus.FAILED);
        assertThat(statementFileCaptor.getValue().getTotalTransactions()).isZero();
        assertThat(statementFileCaptor.getValue().getErrorMessage()).isEqualTo("publish failed");
    }
}
