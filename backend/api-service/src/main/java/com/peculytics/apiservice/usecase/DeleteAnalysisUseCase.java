package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.exception.AnalysisDeletionNotAllowedException;
import com.peculytics.apiservice.exception.AnalysisNotFoundException;
import com.peculytics.apiservice.model.Analysis;
import com.peculytics.apiservice.model.AnalysisStatus;
import com.peculytics.apiservice.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteAnalysisUseCase {
    private final AnalysisRepository analysisRepository;

    @Transactional
    public void execute(UUID analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        if (analysis.getStatus() == AnalysisStatus.PROCESSING) {
            throw new AnalysisDeletionNotAllowedException();
        }

        analysisRepository.delete(analysis);
    }
}
