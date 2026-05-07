package com.peculytics.categorizationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "processed_transaction_batches")
public class ProcessedTransactionBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_file_id", nullable = false)
    private StatementFile statementFile;

    @Column(name = "batch_number", nullable = false)
    private int batchNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static ProcessedTransactionBatch processed(
            Analysis analysis,
            StatementFile statementFile,
            int batchNumber
    ) {
        ProcessedTransactionBatch processedBatch = new ProcessedTransactionBatch();
        processedBatch.analysis = analysis;
        processedBatch.statementFile = statementFile;
        processedBatch.batchNumber = batchNumber;
        processedBatch.createdAt = Instant.now();
        return processedBatch;
    }
}
