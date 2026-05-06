package com.peculytics.categorizationservice.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peculytics.categorizationservice.model.TransactionCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
public class CategorizationResponseParser {
    private final ObjectMapper objectMapper;

    public CategorizationResponse parse(String rawResponse, Set<Integer> allowedIndexes) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return CategorizationResponse.builder()
                    .categoriesByIndex(Map.of())
                    .build();
        }

        try {
            JsonNode root = objectMapper.readTree(extractJson(rawResponse));
            JsonNode categories = root.isArray() ? root : root.path("categories");
            Map<Integer, TransactionCategory> parsed = new HashMap<>();

            if (categories.isArray()) {
                for (JsonNode item : categories) {
                    int index = item.path("index").asInt(-1);
                    String category = item.path("category").asText(null);

                    if (allowedIndexes.contains(index)) {
                        parseCategory(index, category)
                                .ifPresent(parsedCategory -> parsed.put(index, parsedCategory));
                    }
                }
            }

            return CategorizationResponse.builder()
                    .categoriesByIndex(parsed)
                    .build();
        } catch (Exception exception) {
            log.warn(
                    "Could not parse AI categorization response; falling back to unresolved categories. responseLength={} errorType={} errorMessage={}",
                    rawResponse.length(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );

            return CategorizationResponse.builder()
                    .categoriesByIndex(Map.of())
                    .build();
        }
    }

    private static Optional<TransactionCategory> parseCategory(int index, String category) {
        if (category == null || category.isBlank()) {
            log.warn("Ignoring invalid AI categorization item. index={} category={}", index, category);
            return Optional.empty();
        }

        String normalizedCategory = category.trim().toUpperCase(Locale.ROOT);
        if (!TransactionCategory.isAllowedForAi(normalizedCategory)) {
            log.warn("Ignoring invalid AI categorization item. index={} category={}", index, category);
            return Optional.empty();
        }

        return Optional.of(TransactionCategory.valueOf(normalizedCategory));
    }

    private static String extractJson(String rawResponse) {
        String cleaned = rawResponse.strip();
        int objectStart = cleaned.indexOf('{');
        int arrayStart = cleaned.indexOf('[');

        if (objectStart == -1 && arrayStart == -1) {
            return cleaned;
        }

        int start;
        char endChar;
        if (objectStart == -1 || (arrayStart != -1 && arrayStart < objectStart)) {
            start = arrayStart;
            endChar = ']';
        } else {
            start = objectStart;
            endChar = '}';
        }

        int end = cleaned.lastIndexOf(endChar);
        if (end <= start) {
            return cleaned;
        }

        return cleaned.substring(start, end + 1);
    }
}
