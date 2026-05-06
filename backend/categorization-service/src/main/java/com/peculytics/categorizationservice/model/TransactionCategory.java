package com.peculytics.categorizationservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;

@AllArgsConstructor
@Getter
public enum TransactionCategory {
    FOOD("Food"),
    TRANSPORT("Transport"),
    GROCERIES("Groceries"),
    HEALTH("Health"),
    SUBSCRIPTIONS("Subscriptions"),
    HOUSING("Housing"),
    UTILITIES("Utilities"),
    SHOPPING("Shopping"),
    EDUCATION("Education"),
    INCOME("Income"),
    OTHER("Other"),
    UNCATEGORIZED("Uncategorized");

    private final String label;

    public static String[] allowedAiCategories() {
        return Arrays.stream(TransactionCategory.values())
                .filter(category -> category != UNCATEGORIZED)
                .map(TransactionCategory::name)
                .toArray(String[]::new);
    }

    public static boolean isAllowedForAi(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(TransactionCategory.values())
                .filter(category -> category != UNCATEGORIZED)
                .anyMatch(category -> category.name().equals(normalizedValue));
    }
}
