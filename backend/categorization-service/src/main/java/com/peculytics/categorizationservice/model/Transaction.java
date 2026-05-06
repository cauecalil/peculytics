package com.peculytics.categorizationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_file_id", nullable = false)
    private StatementFile statementFile;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionCategory category;

    @Column(name = "category_source", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionCategorySource categorySource;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Transaction categorized(
            Analysis analysis,
            StatementFile statementFile,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            TransactionCategory category,
            TransactionCategorySource categorySource
    ) {
        Transaction transaction = new Transaction();

        transaction.analysis = analysis;
        transaction.statementFile = statementFile;
        transaction.description = description;
        transaction.amount = amount;
        transaction.transactionDate = transactionDate;
        transaction.category = category;
        transaction.categorySource = categorySource;
        transaction.createdAt = Instant.now();

        return transaction;
    }
}
