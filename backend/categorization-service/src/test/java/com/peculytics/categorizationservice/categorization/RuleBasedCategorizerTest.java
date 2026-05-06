package com.peculytics.categorizationservice.categorization;

import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.CategorizationRule;
import com.peculytics.categorizationservice.model.TransactionCategory;
import com.peculytics.categorizationservice.model.TransactionCategorySource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleBasedCategorizerTest {
    private final RuleBasedCategorizer categorizer = new RuleBasedCategorizer();

    @Test
    void shouldFindActiveRuleByKeyword() {
        Optional<CategorizationResult> result = categorizer.categorize(
                transaction("Payment at Coffee Shop"),
                List.of(rule(1L, "coffee", TransactionCategory.FOOD, true))
        );

        assertThat(result)
                .hasValueSatisfying(categorization -> {
                    assertThat(categorization.category()).isEqualTo(TransactionCategory.FOOD);
                    assertThat(categorization.source()).isEqualTo(TransactionCategorySource.RULE);
                });
    }

    @Test
    void shouldIgnoreInactiveRule() {
        Optional<CategorizationResult> result = categorizer.categorize(
                transaction("Payment at Coffee Shop"),
                List.of(rule(1L, "coffee", TransactionCategory.FOOD, false))
        );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNormalizeAccentsCaseAndPunctuation() {
        Optional<CategorizationResult> result = categorizer.categorize(
                transaction("Compra no MERCADO, São José!"),
                List.of(rule(1L, "mercado sao jose", TransactionCategory.GROCERIES, true))
        );

        assertThat(result)
                .hasValueSatisfying(categorization ->
                        assertThat(categorization.category()).isEqualTo(TransactionCategory.GROCERIES)
                );
    }

    @Test
    void shouldNotMatchPartialTokenSubstring() {
        Optional<CategorizationResult> result = categorizer.categorize(
                transaction("Payment at Shellfish Market"),
                List.of(rule(1L, "shell", TransactionCategory.TRANSPORT, true))
        );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldPrioritizeMostSpecificRuleWhenMultipleRulesMatch() {
        Optional<CategorizationResult> result = categorizer.categorize(
                transaction("Uber Trip Airport"),
                List.of(
                        rule(1L, "uber", TransactionCategory.TRANSPORT, true),
                        rule(2L, "uber trip airport", TransactionCategory.OTHER, true)
                )
        );

        assertThat(result)
                .hasValueSatisfying(categorization ->
                        assertThat(categorization.category()).isEqualTo(TransactionCategory.OTHER)
                );
    }

    @Test
    void shouldReturnEmptyWhenNoRuleMatches() {
        Optional<CategorizationResult> result = categorizer.categorize(
                transaction("Movie ticket"),
                List.of(rule(1L, "grocery", TransactionCategory.GROCERIES, true))
        );

        assertThat(result).isEmpty();
    }

    private static TransactionBatchMessage.TransactionMessage transaction(String description) {
        return TransactionBatchMessage.TransactionMessage.builder()
                .description(description)
                .amount(new BigDecimal("10.00"))
                .transactionDate(LocalDate.of(2026, 5, 5))
                .build();
    }

    private static CategorizationRule rule(
            Long id,
            String keyword,
            TransactionCategory category,
            boolean active
    ) {
        CategorizationRule rule = mock(CategorizationRule.class);
        when(rule.getId()).thenReturn(id);
        when(rule.getKeyword()).thenReturn(keyword);
        when(rule.getCategory()).thenReturn(category);
        when(rule.isActive()).thenReturn(active);
        return rule;
    }
}
