package com.peculytics.categorizationservice.service;

import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.Analysis;
import com.peculytics.categorizationservice.model.StatementFile;
import com.peculytics.categorizationservice.repository.AnalysisRepository;
import com.peculytics.categorizationservice.repository.StatementFileRepository;
import com.peculytics.categorizationservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterBatchFailureHandler {
    private static final int MAX_ERROR_MESSAGE_LENGTH = 240;

    private final AnalysisRepository analysisRepository;
    private final StatementFileRepository statementFileRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void handle(TransactionBatchMessage message, String failureReason) {
        log.warn(
                "Received dead-lettered transaction categorization batch: analysisId={} statementFileId={} batchNumber={} failureReason={}",
                message.analysisId(),
                message.statementFileId(),
                message.batchNumber(),
                failureReason
        );

        Optional<Analysis> analysis = analysisRepository.findByIdForUpdate(message.analysisId());

        if (analysis.isEmpty()) {
            log.warn(
                    "Analysis not found for dead-lettered transaction categorization batch: analysisId={} statementFileId={} batchNumber={}",
                    message.analysisId(),
                    message.statementFileId(),
                    message.batchNumber()
            );

            return;
        }

        Optional<StatementFile> statementFile = statementFileRepository.findByIdForUpdate(message.statementFileId());

        if (statementFile.isEmpty()) {
            log.warn(
                    "Statement file not found for dead-lettered transaction categorization batch: analysisId={} statementFileId={} batchNumber={}",
                    message.analysisId(),
                    message.statementFileId(),
                    message.batchNumber()
            );

            return;
        }

        UUID statementFileAnalysisId = statementFileAnalysisId(statementFile.get());

        if (!Objects.equals(statementFileAnalysisId, analysis.get().getId()) || !Objects.equals(statementFileAnalysisId, message.analysisId())) {
            String errorMessage = errorMessage("Statement file does not belong to analysis; statementFileAnalysisId=" + statementFileAnalysisId + "; " + failureReason);

            log.warn(
                    "Statement file does not belong to analysis for dead-lettered transaction categorization batch: analysisId={} statementFileId={} statementFileAnalysisId={} batchNumber={}",
                    message.analysisId(),
                    message.statementFileId(),
                    statementFileAnalysisId,
                    message.batchNumber()
            );

            analysis.get().registerUnrecoverableFailedBatch(errorMessage);
            analysisRepository.save(analysis.get());

            return;
        }

        String errorMessage = errorMessage(failureReason);
        statementFile.get().registerFailedBatch(errorMessage);

        analysis.get().registerFailedBatch(
                transactionRepository.countByAnalysisId(message.analysisId()) > 0,
                errorMessage
        );

        statementFileRepository.save(statementFile.get());
        analysisRepository.save(analysis.get());
    }

    private static String errorMessage(String failureReason) {
        String message = "Categorization batch failed: " + failureReason;

        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }

        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH - 3) + "...";
    }

    private static UUID statementFileAnalysisId(StatementFile statementFile) {
        return statementFile.getAnalysis() == null ? null : statementFile.getAnalysis().getId();
    }
}
