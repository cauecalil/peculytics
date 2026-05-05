package com.peculytics.uploadservice.csv;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record NormalizedTransaction(
        LocalDate transactionDate,
        String description,
        BigDecimal amount
) {}
