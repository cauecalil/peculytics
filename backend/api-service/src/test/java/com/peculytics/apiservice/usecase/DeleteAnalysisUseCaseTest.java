package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.exception.AnalysisDeletionNotAllowedException;
import com.peculytics.apiservice.exception.AnalysisNotFoundException;
import com.peculytics.apiservice.model.Analysis;
import com.peculytics.apiservice.model.AnalysisStatus;
import com.peculytics.apiservice.repository.AnalysisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAnalysisUseCaseTest {
    @Mock
    private AnalysisRepository analysisRepository;

    @InjectMocks
    private DeleteAnalysisUseCase useCase;

    @Test
    void shouldDeleteCompletedAnalysis() {
        UUID analysisId = UUID.randomUUID();
        Analysis analysis = analysisWithStatus(AnalysisStatus.COMPLETED);
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));

        useCase.execute(analysisId);

        verify(analysisRepository).delete(analysis);
    }

    @Test
    void shouldRejectProcessingAnalysisDeletion() {
        UUID analysisId = UUID.randomUUID();
        Analysis analysis = analysisWithStatus(AnalysisStatus.PROCESSING);
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));

        assertThatThrownBy(() -> useCase.execute(analysisId))
                .isInstanceOf(AnalysisDeletionNotAllowedException.class);

        verify(analysisRepository, never()).delete(analysis);
    }

    @Test
    void shouldThrowWhenAnalysisDoesNotExist() {
        UUID analysisId = UUID.randomUUID();
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(analysisId))
                .isInstanceOf(AnalysisNotFoundException.class)
                .hasMessageContaining(analysisId.toString());

        verify(analysisRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private static Analysis analysisWithStatus(AnalysisStatus status) {
        Analysis analysis = mock(Analysis.class);
        when(analysis.getStatus()).thenReturn(status);
        return analysis;
    }
}
