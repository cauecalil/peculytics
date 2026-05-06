package com.peculytics.apiservice.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record GetAnalysisSummaryResponse(
        UUID analysisId,
        BigDecimal totalExpenses,
        List<CategorySummary> categories
) {
    @Builder
    public record CategorySummary(
            String category,
            BigDecimal total,
            BigDecimal percentage
    ) {}
}
