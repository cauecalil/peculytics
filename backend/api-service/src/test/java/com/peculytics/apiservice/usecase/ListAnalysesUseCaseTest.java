package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.dto.ListAnalysesResponse;
import com.peculytics.apiservice.model.Analysis;
import com.peculytics.apiservice.model.AnalysisStatus;
import com.peculytics.apiservice.repository.AnalysisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAnalysesUseCaseTest {
    @Mock
    private AnalysisRepository analysisRepository;

    @InjectMocks
    private ListAnalysesUseCase useCase;

    @Test
    void shouldMapAnalysesReturnedByRepository() {
        UUID analysisId = UUID.randomUUID();
        Analysis analysis = analysis(analysisId);
        when(analysisRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(analysis));

        List<ListAnalysesResponse> response = useCase.execute();

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(analysisId);
            assertThat(item.title()).isEqualTo("April analysis");
            assertThat(item.status()).isEqualTo(AnalysisStatus.FAILED);
            assertThat(item.totalFiles()).isEqualTo(1);
            assertThat(item.totalTransactions()).isEqualTo(12);
            assertThat(item.processedBatches()).isEqualTo(1);
            assertThat(item.totalBatches()).isEqualTo(2);
            assertThat(item.errorMessage()).isEqualTo("Invalid file");
            assertThat(item.createdAt()).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
            assertThat(item.completedAt()).isEqualTo(Instant.parse("2026-04-01T00:05:00Z"));
        });
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryHasNoAnalyses() {
        when(analysisRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<ListAnalysesResponse> response = useCase.execute();

        assertThat(response).isEmpty();
    }

    private static Analysis analysis(UUID analysisId) {
        Analysis analysis = mock(Analysis.class);
        when(analysis.getId()).thenReturn(analysisId);
        when(analysis.getTitle()).thenReturn("April analysis");
        when(analysis.getStatus()).thenReturn(AnalysisStatus.FAILED);
        when(analysis.getTotalFiles()).thenReturn(1);
        when(analysis.getTotalTransactions()).thenReturn(12);
        when(analysis.getProcessedBatches()).thenReturn(1);
        when(analysis.getTotalBatches()).thenReturn(2);
        when(analysis.getErrorMessage()).thenReturn("Invalid file");
        when(analysis.getCreatedAt()).thenReturn(Instant.parse("2026-04-01T00:00:00Z"));
        when(analysis.getCompletedAt()).thenReturn(Instant.parse("2026-04-01T00:05:00Z"));
        return analysis;
    }
}
