package com.peculytics.uploadservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisTest {
    @Test
    void shouldCreateProcessingAnalysisWithTitle() {
        Analysis analysis = Analysis.create("May statement");

        assertThat(analysis.getTitle()).isEqualTo("May statement");
        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.PROCESSING);
        assertThat(analysis.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThatThrownBy(() -> Analysis.create(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title cannot be null or blank");
    }

    @Test
    void shouldUpdateProcessingTotalsAndResetProcessedBatches() {
        Analysis analysis = Analysis.builder()
                .processedBatches(3)
                .status(AnalysisStatus.FAILED)
                .build();

        analysis.updateProcessingTotals(2, 120, 3);

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.PROCESSING);
        assertThat(analysis.getTotalFiles()).isEqualTo(2);
        assertThat(analysis.getTotalTransactions()).isEqualTo(120);
        assertThat(analysis.getTotalBatches()).isEqualTo(3);
        assertThat(analysis.getProcessedBatches()).isZero();
    }

    @Test
    void shouldMarkAnalysisAsFailedAndTruncateErrorMessage() {
        Analysis analysis = Analysis.create("May statement");
        String longMessage = "x".repeat(300);

        analysis.markFailed(2, longMessage);

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getTotalFiles()).isEqualTo(2);
        assertThat(analysis.getTotalTransactions()).isZero();
        assertThat(analysis.getTotalBatches()).isZero();
        assertThat(analysis.getProcessedBatches()).isZero();
        assertThat(analysis.getErrorMessage()).hasSize(255);
        assertThat(analysis.getCompletedAt()).isNotNull();
    }
}
