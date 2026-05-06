package com.peculytics.apiservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "analyses")
public class Analysis {
    @Id
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
}
