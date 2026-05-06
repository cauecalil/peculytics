package com.peculytics.categorizationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "statement_files")
public class StatementFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatementFileStatus status;

    @Column(name = "total_transactions", nullable = false)
    private int totalTransactions;

    @Column(name = "processed_batches", nullable = false)
    private int processedBatches;

    @Column(name = "total_batches", nullable = false)
    private int totalBatches;

    @Column(name = "parser_name")
    private String parserName;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public void registerProcessedBatch() {
        if (processedBatches < totalBatches) {
            processedBatches++;
        }

        if (totalBatches > 0 && processedBatches >= totalBatches) {
            if (status != StatementFileStatus.FAILED) {
                status = StatementFileStatus.COMPLETED;
            }

            completedAt = Instant.now();
        }
    }

    public void registerFailedBatch(String errorMessage) {
        if (processedBatches < totalBatches) {
            processedBatches++;
        }

        status = StatementFileStatus.FAILED;
        this.errorMessage = errorMessage;

        if (totalBatches == 0 || processedBatches >= totalBatches) {
            completedAt = Instant.now();
        }
    }
}
