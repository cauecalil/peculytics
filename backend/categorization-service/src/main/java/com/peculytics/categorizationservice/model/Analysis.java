package com.peculytics.categorizationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "analyses")
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AnalysisStatus status;

    @Column(name = "total_files", nullable = false)
    private int totalFiles;

    @Column(name = "total_transactions", nullable = false)
    private int totalTransactions;

    @Column(name = "processed_batches", nullable = false)
    private int processedBatches;

    @Column(name = "total_batches", nullable = false)
    private int totalBatches;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public void registerProcessedBatch(boolean hasFailedFiles) {
        if (processedBatches < totalBatches) {
            processedBatches++;
        }

        if (totalBatches > 0 && processedBatches >= totalBatches) {
            status = hasFailedFiles ? AnalysisStatus.COMPLETED_WITH_ERRORS : AnalysisStatus.COMPLETED;
            completedAt = Instant.now();
        }
    }

    public void registerFailedBatch(boolean hasPersistedTransactions, String errorMessage) {
        if (processedBatches < totalBatches) {
            processedBatches++;
        }

        this.errorMessage = errorMessage;

        if (totalBatches == 0 || processedBatches >= totalBatches) {
            status = hasPersistedTransactions ? AnalysisStatus.COMPLETED_WITH_ERRORS : AnalysisStatus.FAILED;
            completedAt = Instant.now();
        }
    }

    public void registerUnrecoverableFailedBatch(String errorMessage) {
        if (processedBatches < totalBatches) {
            processedBatches++;
        }

        this.errorMessage = errorMessage;
        status = AnalysisStatus.FAILED;
        completedAt = Instant.now();
    }
}
