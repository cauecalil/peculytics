package com.peculytics.uploadservice.service;

import com.peculytics.uploadservice.model.Analysis;
import com.peculytics.uploadservice.model.StatementFile;
import com.peculytics.uploadservice.repository.AnalysisRepository;
import com.peculytics.uploadservice.repository.StatementFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishFailureHandler {
    private final AnalysisRepository analysisRepository;
    private final StatementFileRepository statementFileRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishingFailed(UUID analysisId, List<UUID> statementFileIds, String errorMessage) {
        analysisRepository.findById(analysisId)
                .ifPresent(analysis -> failAnalysis(analysis, errorMessage));

        statementFileRepository.findAllById(statementFileIds.stream().distinct().toList())
                .forEach(statementFile -> failStatementFile(statementFile, errorMessage));
    }

    private void failAnalysis(Analysis analysis, String errorMessage) {
        analysis.markFailed(errorMessage);
        analysisRepository.save(analysis);
    }

    private void failStatementFile(StatementFile statementFile, String errorMessage) {
        statementFile.markFailed(errorMessage);
        statementFileRepository.save(statementFile);
    }
}
