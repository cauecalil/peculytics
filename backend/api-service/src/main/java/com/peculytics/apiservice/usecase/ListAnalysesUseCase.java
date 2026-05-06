package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.dto.ListAnalysesResponse;
import com.peculytics.apiservice.model.Analysis;
import com.peculytics.apiservice.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListAnalysesUseCase {
    private final AnalysisRepository analysisRepository;

    public List<ListAnalysesResponse> execute() {
        List<Analysis> analyses = analysisRepository.findAllByOrderByCreatedAtDesc();

        return analyses.stream()
                .map(analysis ->
                        ListAnalysesResponse.builder()
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
                                .build()
                )
                .toList();
    }
}
