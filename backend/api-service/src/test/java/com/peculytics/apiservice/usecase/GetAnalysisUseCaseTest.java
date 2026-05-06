package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.dto.GetAnalysisResponse;
import com.peculytics.apiservice.exception.AnalysisNotFoundException;
import com.peculytics.apiservice.model.Analysis;
import com.peculytics.apiservice.model.AnalysisStatus;
import com.peculytics.apiservice.model.StatementFile;
import com.peculytics.apiservice.model.StatementFileStatus;
import com.peculytics.apiservice.repository.AnalysisRepository;
import com.peculytics.apiservice.repository.StatementFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAnalysisUseCaseTest {
    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private StatementFileRepository statementFileRepository;

    @InjectMocks
    private GetAnalysisUseCase useCase;

    @Test
    void shouldMapAnalysisWithStatementFiles() {
        UUID analysisId = UUID.randomUUID();
        Analysis analysis = analysis(analysisId);
        StatementFile statementFile = statementFile();
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));
        when(statementFileRepository.findByAnalysisIdOrderByCreatedAtAscIdAsc(analysisId))
                .thenReturn(List.of(statementFile));

        GetAnalysisResponse response = useCase.execute(analysisId);

        assertThat(response.id()).isEqualTo(analysisId);
        assertThat(response.title()).isEqualTo("Personal analysis");
        assertThat(response.status()).isEqualTo(AnalysisStatus.COMPLETED_WITH_ERRORS);
        assertThat(response.totalFiles()).isEqualTo(2);
        assertThat(response.totalTransactions()).isEqualTo(30);
        assertThat(response.processedBatches()).isEqualTo(3);
        assertThat(response.totalBatches()).isEqualTo(4);
        assertThat(response.errorMessage()).isEqualTo("One file failed");
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(response.completedAt()).isEqualTo(Instant.parse("2026-01-01T00:10:00Z"));
        assertThat(response.files()).singleElement().satisfies(file -> {
            assertThat(file.id()).isEqualTo(statementFile.getId());
            assertThat(file.title()).isEqualTo("Bank file");
            assertThat(file.fileName()).isEqualTo("bank.csv");
            assertThat(file.status()).isEqualTo(StatementFileStatus.COMPLETED);
            assertThat(file.totalTransactions()).isEqualTo(20);
            assertThat(file.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:01:00Z"));
        });
    }

    @Test
    void shouldThrowWhenAnalysisDoesNotExist() {
        UUID analysisId = UUID.randomUUID();
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(analysisId))
                .isInstanceOf(AnalysisNotFoundException.class)
                .hasMessageContaining(analysisId.toString());

        verify(statementFileRepository, never()).findByAnalysisIdOrderByCreatedAtAscIdAsc(analysisId);
    }

    private static Analysis analysis(UUID analysisId) {
        Analysis analysis = mock(Analysis.class);
        when(analysis.getId()).thenReturn(analysisId);
        when(analysis.getTitle()).thenReturn("Personal analysis");
        when(analysis.getStatus()).thenReturn(AnalysisStatus.COMPLETED_WITH_ERRORS);
        when(analysis.getTotalFiles()).thenReturn(2);
        when(analysis.getTotalTransactions()).thenReturn(30);
        when(analysis.getProcessedBatches()).thenReturn(3);
        when(analysis.getTotalBatches()).thenReturn(4);
        when(analysis.getErrorMessage()).thenReturn("One file failed");
        when(analysis.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(analysis.getCompletedAt()).thenReturn(Instant.parse("2026-01-01T00:10:00Z"));
        return analysis;
    }

    private static StatementFile statementFile() {
        StatementFile statementFile = mock(StatementFile.class);
        when(statementFile.getId()).thenReturn(UUID.randomUUID());
        when(statementFile.getTitle()).thenReturn("Bank file");
        when(statementFile.getFileName()).thenReturn("bank.csv");
        when(statementFile.getStatus()).thenReturn(StatementFileStatus.COMPLETED);
        when(statementFile.getTotalTransactions()).thenReturn(20);
        when(statementFile.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:01:00Z"));
        return statementFile;
    }
}
