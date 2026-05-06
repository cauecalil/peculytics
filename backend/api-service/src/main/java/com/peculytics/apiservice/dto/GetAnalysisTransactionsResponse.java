package com.peculytics.apiservice.dto;

import com.peculytics.apiservice.model.TransactionCategorySource;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record GetAnalysisTransactionsResponse(
        List<Transaction> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    @Builder
    public record Transaction(
            UUID id,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            String category,
            TransactionCategorySource categorySource,
            Instant createdAt,
            SourceFile sourceFile
    ) {}

    @Builder
    public record SourceFile(
            UUID id,
            String title
    ) {}
}
