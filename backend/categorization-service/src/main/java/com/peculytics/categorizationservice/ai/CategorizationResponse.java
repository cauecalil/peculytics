package com.peculytics.categorizationservice.ai;

import com.peculytics.categorizationservice.model.TransactionCategory;
import lombok.Builder;

import java.util.Map;
import java.util.Optional;

@Builder
public record CategorizationResponse(
        Map<Integer, TransactionCategory> categoriesByIndex
) {
    public Optional<TransactionCategory> categoryFor(int index) {
        return Optional.ofNullable(categoriesByIndex.get(index));
    }
}
