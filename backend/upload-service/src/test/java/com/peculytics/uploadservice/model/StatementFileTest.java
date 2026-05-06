package com.peculytics.uploadservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatementFileTest {
    @Test
    void shouldCreateProcessingStatementFileUsingFilenameWhenTitleIsBlank() {
        Analysis analysis = Analysis.create("May statement");

        StatementFile statementFile = StatementFile.create(analysis, " ", "statement.csv");

        assertThat(statementFile.getAnalysis()).isSameAs(analysis);
        assertThat(statementFile.getTitle()).isEqualTo("statement.csv");
        assertThat(statementFile.getFileName()).isEqualTo("statement.csv");
        assertThat(statementFile.getStatus()).isEqualTo(StatementFileStatus.PROCESSING);
        assertThat(statementFile.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRejectMissingAnalysis() {
        assertThatThrownBy(() -> StatementFile.create(null, "Checking", "statement.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Analysis cannot be null");
    }

    @Test
    void shouldMarkProcessingWithParserTotalsAndResetProcessedBatches() {
        StatementFile statementFile = StatementFile.builder()
                .processedBatches(4)
                .status(StatementFileStatus.FAILED)
                .build();

        statementFile.markProcessing("GENERIC_HEADER_CSV", 75, 2);

        assertThat(statementFile.getStatus()).isEqualTo(StatementFileStatus.PROCESSING);
        assertThat(statementFile.getParserName()).isEqualTo("GENERIC_HEADER_CSV");
        assertThat(statementFile.getTotalTransactions()).isEqualTo(75);
        assertThat(statementFile.getTotalBatches()).isEqualTo(2);
        assertThat(statementFile.getProcessedBatches()).isZero();
    }

    @Test
    void shouldMarkFileAsFailedAndTruncateErrorMessage() {
        StatementFile statementFile = StatementFile.builder()
                .status(StatementFileStatus.PROCESSING)
                .totalTransactions(20)
                .totalBatches(1)
                .processedBatches(1)
                .build();
        String longMessage = "x".repeat(300);

        statementFile.markFailed(longMessage);

        assertThat(statementFile.getStatus()).isEqualTo(StatementFileStatus.FAILED);
        assertThat(statementFile.getTotalTransactions()).isZero();
        assertThat(statementFile.getTotalBatches()).isZero();
        assertThat(statementFile.getProcessedBatches()).isZero();
        assertThat(statementFile.getErrorMessage()).hasSize(255);
        assertThat(statementFile.getCompletedAt()).isNotNull();
    }
}
