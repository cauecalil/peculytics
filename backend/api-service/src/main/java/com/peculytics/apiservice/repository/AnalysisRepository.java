package com.peculytics.apiservice.repository;

import com.peculytics.apiservice.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
    List<Analysis> findAllByOrderByCreatedAtDesc();
}
