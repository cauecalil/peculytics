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
@Table(name = "statement_files")
public class StatementFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "title")
    private String title;

    @Column(name = "file_name")
    private String fileName;

    @Enumerated(EnumType.STRING)
    private StatementFileStatus status;

    @Column(name = "total_transactions")
    private int totalTransactions;

    @Column(name = "processed_batches")
    private int processedBatches;

    @Column(name = "total_batches")
    private int totalBatches;

    @Column(name = "parser_name")
    private String parserName;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static StatementFile create(Analysis analysis, String title, String originalFilename) {
        if (analysis == null) {
            throw new IllegalArgumentException("Analysis cannot be null");
        }

        if (title == null || title.isBlank()) {
            title = originalFilename;
        }

        return StatementFile.builder()
                .analysis(analysis)
                .title(truncate(title))
                .fileName(truncate(originalFilename))
                .status(StatementFileStatus.PROCESSING)
                .createdAt(Instant.now())
                .build();
    }

    public void markProcessing(String parserName, int totalTransactions, int totalBatches) {
        this.parserName = parserName;
        this.totalTransactions = totalTransactions;
        this.totalBatches = totalBatches;
        this.processedBatches = 0;
        this.status = StatementFileStatus.PROCESSING;
    }

    public void markFailed(String errorMessage) {
        this.status = StatementFileStatus.FAILED;
        this.errorMessage = truncate(errorMessage);
        this.totalTransactions = 0;
        this.totalBatches = 0;
        this.processedBatches = 0;
        this.completedAt = Instant.now();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 255) {
            return value;
        }

        return value.substring(0, 255);
    }
}
