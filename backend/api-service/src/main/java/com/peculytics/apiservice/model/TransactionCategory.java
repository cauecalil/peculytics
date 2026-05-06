package com.peculytics.apiservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
}
