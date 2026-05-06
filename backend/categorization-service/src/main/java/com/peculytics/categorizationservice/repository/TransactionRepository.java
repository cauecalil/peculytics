package com.peculytics.categorizationservice.repository;

import com.peculytics.categorizationservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.analysis.id = :analysisId
    """)
    long countByAnalysisId(@Param("analysisId") UUID analysisId);
}
