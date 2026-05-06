package com.peculytics.apiservice.repository;

import com.peculytics.apiservice.model.Transaction;
import com.peculytics.apiservice.model.TransactionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByAnalysisId(UUID analysisId, Pageable pageable);

    @Query("""
        SELECT
            t.category AS category,
            SUM(ABS(t.amount)) AS total
        FROM Transaction t
        WHERE t.analysis.id = :analysisId AND t.amount < 0
        GROUP BY t.category
        ORDER BY SUM(ABS(t.amount)) DESC, t.category ASC
    """)
    List<CategoryExpenseSummaryProjection> summarizeExpensesByCategory(@Param("analysisId") UUID analysisId);

    interface CategoryExpenseSummaryProjection {
        TransactionCategory getCategory();
        BigDecimal getTotal();
    }
}
