package com.peculytics.categorizationservice.categorization;

import com.peculytics.categorizationservice.model.TransactionCategory;
import com.peculytics.categorizationservice.model.TransactionCategorySource;
import lombok.Builder;

@Builder
public record CategorizationResult(
        TransactionCategory category,
        TransactionCategorySource source
) {}
