package com.peculytics.categorizationservice.ai;

import java.util.Map;

public class FallbackTransactionCategorizerAi implements TransactionCategorizerAi {
    @Override
    public CategorizationResponse categorize(CategorizationPrompt prompt) {
        return CategorizationResponse.builder()
                .categoriesByIndex(Map.of())
                .build();
    }
}
