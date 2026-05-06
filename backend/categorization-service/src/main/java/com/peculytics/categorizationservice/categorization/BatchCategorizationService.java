package com.peculytics.categorizationservice.categorization;

import com.peculytics.categorizationservice.ai.CategorizationPrompt;
import com.peculytics.categorizationservice.ai.CategorizationResponse;
import com.peculytics.categorizationservice.ai.TransactionCategorizerAi;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.CategorizationRule;
import com.peculytics.categorizationservice.model.TransactionCategory;
import com.peculytics.categorizationservice.model.TransactionCategorySource;
import com.peculytics.categorizationservice.repository.CategorizationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchCategorizationService {
    private final CategorizationRuleRepository categorizationRuleRepository;
    private final RuleBasedCategorizer ruleBasedCategorizer;
    private final TransactionCategorizerAi transactionCategorizerAi;

    public Map<Integer, CategorizationResult> categorize(TransactionBatchMessage message) {
        List<CategorizationRule> rules = categorizationRuleRepository.findByActiveTrueOrderByKeywordAsc();
        Map<Integer, CategorizationResult> results = new HashMap<>();
        List<CategorizationPrompt.TransactionPromptItem> unresolved = new ArrayList<>();

        for (int index = 0; index < message.transactions().size(); index++) {
            TransactionBatchMessage.TransactionMessage transaction = message.transactions().get(index);
            Optional<CategorizationResult> ruleResult = ruleBasedCategorizer.categorize(transaction, rules);

            if (ruleResult.isPresent()) {
                results.put(index, ruleResult.get());
            } else {
                unresolved.add(toPromptItem(index, transaction));
            }
        }

        CategorizationResponse aiResponse = categorizeWithAi(message, unresolved);
        applyAiResultsOrFallback(results, unresolved, aiResponse);

        return results;
    }

    private static CategorizationPrompt.TransactionPromptItem toPromptItem(
            int index,
            TransactionBatchMessage.TransactionMessage transaction
    ) {
        return CategorizationPrompt.TransactionPromptItem.builder()
                .index(index)
                .description(transaction.description())
                .amount(transaction.amount())
                .transactionDate(transaction.transactionDate())
                .build();
    }

    private CategorizationResponse categorizeWithAi(
            TransactionBatchMessage message,
            List<CategorizationPrompt.TransactionPromptItem> unresolved
    ) {
        if (unresolved.isEmpty()) {
            return new CategorizationResponse(Map.of());
        }

        try {
            return transactionCategorizerAi.categorize(new CategorizationPrompt(unresolved));
        } catch (Exception exception) {
            log.warn(
                    "AI categorization failed; analysisId={}, statementFileId={}, aiTransactionCount={}, exceptionType={}, exceptionMessage={}",
                    message.analysisId(),
                    message.statementFileId(),
                    unresolved.size(),
                    exception.getClass().getName(),
                    exception.getMessage()
            );
            return new CategorizationResponse(Map.of());
        }
    }

    private static void applyAiResultsOrFallback(
            Map<Integer, CategorizationResult> results,
            List<CategorizationPrompt.TransactionPromptItem> unresolved,
            CategorizationResponse aiResponse
    ) {
        for (CategorizationPrompt.TransactionPromptItem transaction : unresolved) {
            CategorizationResult result = aiResponse.categoryFor(transaction.index())
                    .map(category ->
                            CategorizationResult.builder()
                                    .category(category)
                                    .source(TransactionCategorySource.AI)
                                    .build()
                    )
                    .orElseGet(() ->
                            CategorizationResult.builder()
                                    .category(TransactionCategory.UNCATEGORIZED)
                                    .source(TransactionCategorySource.FALLBACK)
                                    .build()
                    );

            results.put(transaction.index(), result);
        }
    }
}
