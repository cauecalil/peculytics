package com.peculytics.categorizationservice.repository;

import com.peculytics.categorizationservice.model.ProcessedTransactionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedTransactionBatchRepository extends JpaRepository<ProcessedTransactionBatch, UUID> {
    @Query("""
        SELECT COUNT(ptb) > 0
        FROM ProcessedTransactionBatch ptb
        WHERE ptb.analysis.id = :analysisId
          AND ptb.statementFile.id = :statementFileId
          AND ptb.batchNumber = :batchNumber
    """)
    boolean existsByAnalysisIdAndStatementFileIdAndBatchNumber(
            @Param("analysisId") UUID analysisId,
            @Param("statementFileId") UUID statementFileId,
            @Param("batchNumber") int batchNumber
    );
}
