package com.peculytics.uploadservice.dto;

import com.peculytics.uploadservice.model.AnalysisStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateAnalysisResponse(
    UUID id,
    String title,
    AnalysisStatus status,
    int totalFiles,
    int acceptedFiles,
    int rejectedFiles,
    String message
) {}
