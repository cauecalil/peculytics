package com.peculytics.apiservice.repository;

import com.peculytics.apiservice.model.StatementFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StatementFileRepository extends JpaRepository<StatementFile, UUID> {
    List<StatementFile> findByAnalysisIdOrderByCreatedAtAscIdAsc(UUID analysisId);
}
