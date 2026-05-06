package com.peculytics.categorizationservice.ai;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record CategorizationPrompt(
        List<TransactionPromptItem> transactions
) {
    @Builder
    public record TransactionPromptItem(
            int index,
            String description,
            BigDecimal amount,
            LocalDate transactionDate
    ) {}
}
