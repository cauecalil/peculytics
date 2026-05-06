package com.peculytics.categorizationservice.categorization;

import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.CategorizationRule;
import com.peculytics.categorizationservice.model.TransactionCategorySource;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class RuleBasedCategorizer {
    private static final Comparator<RuleMatch> MATCH_SPECIFICITY = Comparator
            .comparingInt((RuleMatch match) -> match.normalizedKeyword().length())
            .reversed()
            .thenComparing(Comparator.comparingInt(RuleMatch::tokenCount).reversed())
            .thenComparing(match -> safeText(match.rule().getKeyword()), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(match -> safeText(match.rule().getCategory().name()), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(match -> match.rule().getId(), Comparator.nullsLast(Long::compareTo));

    public Optional<CategorizationResult> categorize(
            TransactionBatchMessage.TransactionMessage transaction,
            List<CategorizationRule> rules
    ) {
        List<String> descriptionTokens = tokenize(transaction.description());

        return rules.stream()
                .filter(CategorizationRule::isActive)
                .map(rule -> match(rule, descriptionTokens))
                .flatMap(Optional::stream).min(MATCH_SPECIFICITY)
                .map(match ->
                        CategorizationResult.builder()
                                .category(match.rule().getCategory())
                                .source(TransactionCategorySource.RULE)
                                .build()
                );
    }

    private static Optional<RuleMatch> match(CategorizationRule rule, List<String> descriptionTokens) {
        List<String> keywordTokens = tokenize(rule.getKeyword());

        if (keywordTokens.isEmpty() || !containsTokenSequence(descriptionTokens, keywordTokens)) {
            return Optional.empty();
        }

        String normalizedKeyword = String.join(" ", keywordTokens);
        RuleMatch match = new RuleMatch(rule, normalizedKeyword, keywordTokens.size());

        return Optional.of(match);
    }

    private static boolean containsTokenSequence(List<String> descriptionTokens, List<String> keywordTokens) {
        if (keywordTokens.isEmpty() || keywordTokens.size() > descriptionTokens.size()) {
            return false;
        }

        for (int start = 0; start <= descriptionTokens.size() - keywordTokens.size(); start++) {
            if (matchesTokenSequenceAt(descriptionTokens, keywordTokens, start)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesTokenSequenceAt(
            List<String> descriptionTokens,
            List<String> keywordTokens,
            int start
    ) {
        for (int offset = 0; offset < keywordTokens.size(); offset++) {
            if (!descriptionTokens.get(start + offset).equals(keywordTokens.get(offset))) {
                return false;
            }
        }

        return true;
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);

        return normalized
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private static List<String> tokenize(String value) {
        String normalizedValue = normalize(value);

        if (normalizedValue.isBlank()) {
            return List.of();
        }

        return List.of(normalizedValue.split(" "));
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private record RuleMatch(
            CategorizationRule rule,
            String normalizedKeyword,
            int tokenCount
    ) {}
}
