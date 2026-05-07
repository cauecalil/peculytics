package com.peculytics.categorizationservice.service;

import com.peculytics.categorizationservice.categorization.CategorizationResult;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.Analysis;
import com.peculytics.categorizationservice.model.ProcessedTransactionBatch;
import com.peculytics.categorizationservice.model.StatementFile;
import com.peculytics.categorizationservice.model.StatementFileStatus;
import com.peculytics.categorizationservice.model.Transaction;
import com.peculytics.categorizationservice.repository.AnalysisRepository;
import com.peculytics.categorizationservice.repository.ProcessedTransactionBatchRepository;
import com.peculytics.categorizationservice.repository.StatementFileRepository;
import com.peculytics.categorizationservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionBatchPersistenceService {
    private final AnalysisRepository analysisRepository;
    private final StatementFileRepository statementFileRepository;
    private final TransactionRepository transactionRepository;
    private final ProcessedTransactionBatchRepository processedTransactionBatchRepository;
    private final TransactionFactory transactionFactory;

    @Transactional(readOnly = true)
    public boolean isProcessed(TransactionBatchMessage message) {
        boolean processed = isProcessedBatch(message);

        if (processed) {
            logDuplicateBatch(message);
        }

        return processed;
    }

    @Transactional
    public void persistProcessedBatch(
            TransactionBatchMessage message,
            Map<Integer, CategorizationResult> results
    ) {
        Analysis analysis = analysisRepository.findByIdForUpdate(message.analysisId())
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + message.analysisId()));

        StatementFile statementFile = statementFileRepository.findByIdForUpdate(message.statementFileId())
                .orElseThrow(() -> new IllegalArgumentException("Statement file not found: " + message.statementFileId()));

        validateStatementFileBelongsToAnalysis(message, analysis, statementFile);

        if (isProcessedBatch(message)) {
            logDuplicateBatch(message);
            return;
        }

        List<Transaction> transactions = transactionFactory.createAll(message, analysis, statementFile, results);
        transactionRepository.saveAll(transactions);
        processedTransactionBatchRepository.save(ProcessedTransactionBatch.processed(analysis, statementFile, message.batchNumber()));

        statementFile.registerProcessedBatch();
        boolean hasFailedFiles = statementFileRepository.countByAnalysisIdAndStatus(analysis.getId(), StatementFileStatus.FAILED) > 0;
        analysis.registerProcessedBatch(hasFailedFiles);

        statementFileRepository.save(statementFile);
        analysisRepository.save(analysis);
    }

    private boolean isProcessedBatch(TransactionBatchMessage message) {
        return processedTransactionBatchRepository.existsByAnalysisIdAndStatementFileIdAndBatchNumber(
                message.analysisId(),
                message.statementFileId(),
                message.batchNumber()
        );
    }

    private static void logDuplicateBatch(TransactionBatchMessage message) {
        log.info(
                "Ignoring already processed transaction categorization batch: analysisId={} statementFileId={} batchNumber={}",
                message.analysisId(),
                message.statementFileId(),
                message.batchNumber()
        );
    }

    private static void validateStatementFileBelongsToAnalysis(
            TransactionBatchMessage message,
            Analysis analysis,
            StatementFile statementFile
    ) {
        UUID analysisId = analysis.getId();
        UUID messageAnalysisId = message.analysisId();
        UUID statementFileAnalysisId = statementFileAnalysisId(statementFile);

        if (!Objects.equals(messageAnalysisId, analysisId)) {
            throw new IllegalArgumentException("Message analysis " + messageAnalysisId + " does not match loaded analysis " + analysisId);
        }

        if (!Objects.equals(statementFileAnalysisId, analysisId)) {
            throw new IllegalArgumentException("Statement file " + message.statementFileId() + " belongs to analysis " + statementFileAnalysisId + ", not analysis " + analysisId);
        }
    }

    private static UUID statementFileAnalysisId(StatementFile statementFile) {
        return statementFile.getAnalysis() == null ? null : statementFile.getAnalysis().getId();
    }
}
