package com.peculytics.uploadservice.messaging;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record TransactionBatchMessage(
        UUID analysisId,
        UUID statementFileId,
        int batchNumber,
        int totalBatches,
        List<TransactionMessage> transactions
) {
    @Builder
    public record TransactionMessage(
            String description,
            BigDecimal amount,
            LocalDate transactionDate
    ) {}
}
