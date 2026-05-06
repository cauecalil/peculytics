package com.peculytics.categorizationservice.repository;

import com.peculytics.categorizationservice.model.StatementFile;
import com.peculytics.categorizationservice.model.StatementFileStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatementFileRepository extends JpaRepository<StatementFile, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT sf
        FROM StatementFile sf
        WHERE sf.id = :id
    """)
    Optional<StatementFile> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        SELECT COUNT(sf)
        FROM StatementFile sf
        WHERE sf.analysis.id = :analysisId AND sf.status = :status
    """)
    long countByAnalysisIdAndStatus(@Param("analysisId") UUID analysisId, @Param("status") StatementFileStatus status);
}
