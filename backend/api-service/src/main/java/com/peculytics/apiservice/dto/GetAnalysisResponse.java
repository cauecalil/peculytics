package com.peculytics.apiservice.dto;

import com.peculytics.apiservice.model.AnalysisStatus;
import com.peculytics.apiservice.model.StatementFileStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record GetAnalysisResponse(
        UUID id,
        String title,
        AnalysisStatus status,
        int totalFiles,
        int totalTransactions,
        int processedBatches,
        int totalBatches,
        String errorMessage,
        Instant createdAt,
        Instant completedAt,
        List<StatementFile> files
) {
    @Builder
    public record StatementFile(
            UUID id,
            String title,
            String fileName,
            StatementFileStatus status,
            int totalTransactions,
            Instant createdAt
    ) {}
}
