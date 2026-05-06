package com.peculytics.categorizationservice.categorization;

import com.peculytics.categorizationservice.ai.CategorizationPrompt;
import com.peculytics.categorizationservice.ai.CategorizationResponse;
import com.peculytics.categorizationservice.ai.TransactionCategorizerAi;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.CategorizationRule;
import com.peculytics.categorizationservice.model.TransactionCategory;
import com.peculytics.categorizationservice.model.TransactionCategorySource;
import com.peculytics.categorizationservice.repository.CategorizationRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchCategorizationServiceTest {
    @Mock
    private CategorizationRuleRepository categorizationRuleRepository;

    @Mock
    private RuleBasedCategorizer ruleBasedCategorizer;

    @Mock
    private TransactionCategorizerAi transactionCategorizerAi;

    @InjectMocks
    private BatchCategorizationService service;

    @Test
    void shouldUseRuleBeforeAiAndSendOnlyUnresolvedTransactionsToAi() {
        CategorizationRule rule = org.mockito.Mockito.mock(CategorizationRule.class);
        List<CategorizationRule> rules = List.of(rule);
        TransactionBatchMessage message = message("Coffee", "Taxi");
        CategorizationResult ruleResult = result(TransactionCategory.FOOD, TransactionCategorySource.RULE);
        when(categorizationRuleRepository.findByActiveTrueOrderByKeywordAsc()).thenReturn(rules);
        when(ruleBasedCategorizer.categorize(message.transactions().get(0), rules)).thenReturn(Optional.of(ruleResult));
        when(ruleBasedCategorizer.categorize(message.transactions().get(1), rules)).thenReturn(Optional.empty());
        when(transactionCategorizerAi.categorize(any()))
                .thenReturn(new CategorizationResponse(Map.of(1, TransactionCategory.TRANSPORT)));

        Map<Integer, CategorizationResult> results = service.categorize(message);

        ArgumentCaptor<CategorizationPrompt> promptCaptor = ArgumentCaptor.forClass(CategorizationPrompt.class);
        verify(transactionCategorizerAi).categorize(promptCaptor.capture());
        assertThat(promptCaptor.getValue().transactions())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.index()).isEqualTo(1);
                    assertThat(item.description()).isEqualTo("Taxi");
                });
        assertThat(results.get(0)).isEqualTo(ruleResult);
        assertThat(results.get(1).category()).isEqualTo(TransactionCategory.TRANSPORT);
        assertThat(results.get(1).source()).isEqualTo(TransactionCategorySource.AI);
    }

    @Test
    void shouldApplyFallbackWhenAiDoesNotReturnCategory() {
        TransactionBatchMessage message = message("Unknown");
        when(categorizationRuleRepository.findByActiveTrueOrderByKeywordAsc()).thenReturn(List.of());
        when(ruleBasedCategorizer.categorize(message.transactions().getFirst(), List.of())).thenReturn(Optional.empty());
        when(transactionCategorizerAi.categorize(any())).thenReturn(new CategorizationResponse(Map.of()));

        Map<Integer, CategorizationResult> results = service.categorize(message);

        assertThat(results.get(0).category()).isEqualTo(TransactionCategory.UNCATEGORIZED);
        assertThat(results.get(0).source()).isEqualTo(TransactionCategorySource.FALLBACK);
    }

    @Test
    void shouldApplyFallbackWhenAiThrowsException() {
        TransactionBatchMessage message = message("Unknown");
        when(categorizationRuleRepository.findByActiveTrueOrderByKeywordAsc()).thenReturn(List.of());
        when(ruleBasedCategorizer.categorize(message.transactions().getFirst(), List.of())).thenReturn(Optional.empty());
        when(transactionCategorizerAi.categorize(any())).thenThrow(new RuntimeException("AI unavailable"));

        Map<Integer, CategorizationResult> results = service.categorize(message);

        assertThat(results.get(0).category()).isEqualTo(TransactionCategory.UNCATEGORIZED);
        assertThat(results.get(0).source()).isEqualTo(TransactionCategorySource.FALLBACK);
    }

    @Test
    void shouldNotCallAiWhenAllTransactionsAreResolvedByRule() {
        TransactionBatchMessage message = message("Coffee");
        CategorizationResult ruleResult = result(TransactionCategory.FOOD, TransactionCategorySource.RULE);
        when(categorizationRuleRepository.findByActiveTrueOrderByKeywordAsc()).thenReturn(List.of());
        when(ruleBasedCategorizer.categorize(message.transactions().getFirst(), List.of())).thenReturn(Optional.of(ruleResult));

        Map<Integer, CategorizationResult> results = service.categorize(message);

        assertThat(results).containsEntry(0, ruleResult);
        verify(transactionCategorizerAi, never()).categorize(any());
    }

    private static CategorizationResult result(
            TransactionCategory category,
            TransactionCategorySource source
    ) {
        return CategorizationResult.builder()
                .category(category)
                .source(source)
                .build();
    }

    private static TransactionBatchMessage message(String... descriptions) {
        return TransactionBatchMessage.builder()
                .analysisId(UUID.randomUUID())
                .statementFileId(UUID.randomUUID())
                .batchNumber(1)
                .totalBatches(1)
                .transactions(java.util.Arrays.stream(descriptions)
                        .map(BatchCategorizationServiceTest::transaction)
                        .toList())
                .build();
    }

    private static TransactionBatchMessage.TransactionMessage transaction(String description) {
        return TransactionBatchMessage.TransactionMessage.builder()
                .description(description)
                .amount(new BigDecimal("10.00"))
                .transactionDate(LocalDate.of(2026, 5, 5))
                .build();
    }
}
