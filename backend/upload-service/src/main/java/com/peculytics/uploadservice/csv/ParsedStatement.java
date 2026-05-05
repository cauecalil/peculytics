package com.peculytics.uploadservice.csv;

import lombok.Builder;

import java.util.List;

@Builder
public record ParsedStatement(
        List<NormalizedTransaction> transactions,
        String parserName
) {}
