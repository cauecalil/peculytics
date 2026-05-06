package com.peculytics.categorizationservice.ai;

public interface TransactionCategorizerAi {
    CategorizationResponse categorize(CategorizationPrompt prompt);
}
