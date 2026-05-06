package com.peculytics.apiservice.dto;

import com.peculytics.apiservice.model.AnalysisStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ListAnalysesResponse(
        UUID id,
        String title,
        AnalysisStatus status,
        int totalFiles,
        int totalTransactions,
        int processedBatches,
        int totalBatches,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {}
