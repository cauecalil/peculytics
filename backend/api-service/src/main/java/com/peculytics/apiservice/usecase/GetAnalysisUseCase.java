package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.dto.GetAnalysisResponse;
import com.peculytics.apiservice.exception.AnalysisNotFoundException;
import com.peculytics.apiservice.model.Analysis;
import com.peculytics.apiservice.repository.AnalysisRepository;
import com.peculytics.apiservice.repository.StatementFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAnalysisUseCase {
    private final AnalysisRepository analysisRepository;
    private final StatementFileRepository statementFileRepository;

    public GetAnalysisResponse execute(UUID analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        List<GetAnalysisResponse.StatementFile> files = statementFileRepository.findByAnalysisIdOrderByCreatedAtAscIdAsc(analysisId).stream()
                .map(file ->
                        GetAnalysisResponse.StatementFile.builder()
                                .id(file.getId())
                                .title(file.getTitle())
                                .fileName(file.getFileName())
                                .status(file.getStatus())
                                .totalTransactions(file.getTotalTransactions())
                                .createdAt(file.getCreatedAt())
                                .build()
                )
                .toList();

        return GetAnalysisResponse.builder()
                .id(analysis.getId())
                .title(analysis.getTitle())
                .status(analysis.getStatus())
                .totalFiles(analysis.getTotalFiles())
                .totalTransactions(analysis.getTotalTransactions())
                .processedBatches(analysis.getProcessedBatches())
                .totalBatches(analysis.getTotalBatches())
                .errorMessage(analysis.getErrorMessage())
                .createdAt(analysis.getCreatedAt())
                .completedAt(analysis.getCompletedAt())
                .files(files)
                .build();
    }
}
