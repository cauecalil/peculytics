package com.peculytics.uploadservice.repository;

import com.peculytics.uploadservice.model.StatementFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StatementFileRepository extends JpaRepository<StatementFile, UUID> {
}
