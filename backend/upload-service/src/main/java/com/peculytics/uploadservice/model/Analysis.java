package com.peculytics.uploadservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Table(name = "analyses")
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title")
    private String title;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AnalysisStatus status;

    @Column(name = "total_files")
    private int totalFiles;

    @Column(name = "total_transactions")
    private int totalTransactions;

    @Column(name = "processed_batches")
    private int processedBatches;

    @Column(name = "total_batches")
    private int totalBatches;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static Analysis create(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }

        return Analysis.builder()
                .title(title)
                .status(AnalysisStatus.PROCESSING)
                .createdAt(Instant.now())
                .build();
    }

    public void updateProcessingTotals(int totalFiles, int totalTransactions, int totalBatches) {
        this.totalFiles = totalFiles;
        this.totalTransactions = totalTransactions;
        this.totalBatches = totalBatches;
        this.processedBatches = 0;
        this.status = AnalysisStatus.PROCESSING;
    }

    public void markFailed(int totalFiles, String errorMessage) {
        this.totalFiles = totalFiles;
        this.totalTransactions = 0;
        this.totalBatches = 0;
        this.processedBatches = 0;
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = truncate(errorMessage);
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = truncate(errorMessage);
        this.completedAt = Instant.now();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 255) {
            return value;
        }

        return value.substring(0, 255);
    }
}
