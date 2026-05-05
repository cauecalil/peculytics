package com.peculytics.uploadservice.service;

import com.peculytics.uploadservice.csv.NormalizedTransaction;
import com.peculytics.uploadservice.csv.ParsedStatement;
import com.peculytics.uploadservice.csv.StatementCsvParser;
import com.peculytics.uploadservice.csv.StatementCsvParserResolver;
import com.peculytics.uploadservice.csv.UploadedCsvFile;
import com.peculytics.uploadservice.dto.CreateAnalysisRequest;
import com.peculytics.uploadservice.dto.CreateAnalysisResponse;
import com.peculytics.uploadservice.exceptions.InvalidAnalysisRequestException;
import com.peculytics.uploadservice.exceptions.UnsupportedCsvFormatException;
import com.peculytics.uploadservice.messaging.TransactionBatchMessage;
import com.peculytics.uploadservice.messaging.TransactionBatchPublisher;
import com.peculytics.uploadservice.model.Analysis;
import com.peculytics.uploadservice.model.StatementFile;
import com.peculytics.uploadservice.repository.AnalysisRepository;
import com.peculytics.uploadservice.repository.StatementFileRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisService {
    private static final int BATCH_SIZE = 50;
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_DATABASE_TEXT_LENGTH = 255;
    private static final Set<String> CSV_CONTENT_TYPES = Set.of("text/csv", "application/csv", "application/vnd.ms-excel", "text/plain");

    private final AnalysisRepository analysisRepository;
    private final StatementFileRepository statementFileRepository;
    private final StatementCsvParserResolver parserResolver;
    private final TransactionBatchPublisher batchPublisher;
    private final PublishFailureHandler publishFailureHandler;

    @Transactional(noRollbackFor = UnsupportedCsvFormatException.class)
    public CreateAnalysisResponse createAnalysis(CreateAnalysisRequest request) {
        validateRequest(request);

        List<ParsedUploadedFile> parsedFiles = parseFiles(request);
        List<ParsedUploadedFile> acceptedFiles = parsedFiles.stream()
                .filter(ParsedUploadedFile::accepted)
                .toList();

        if (acceptedFiles.isEmpty()) {
            Analysis failedAnalysis = persistFailedAnalysis(request, parsedFiles);
            throw noAcceptedFilesException(parsedFiles, failedAnalysis.getId());
        }

        int totalTransactions = acceptedFiles.stream()
                .mapToInt(file -> file.transactions().size())
                .sum();

        int totalBatches = acceptedFiles.stream()
                .mapToInt(file -> batchCount(file.transactions().size()))
                .sum();

        Analysis analysis = Analysis.create(request.title());
        analysis.updateProcessingTotals(request.files().size(), totalTransactions, totalBatches);
        analysis = analysisRepository.save(analysis);

        List<TransactionBatchMessage> pendingMessages = new ArrayList<>();
        List<UUID> acceptedStatementFileIds = new ArrayList<>();
        for (ParsedUploadedFile parsedFile : parsedFiles) {
            StatementFile statementFile = StatementFile.create(
                    analysis,
                    parsedFile.title(),
                    parsedFile.originalFilename()
            );

            if (parsedFile.accepted()) {
                int fileTotalBatches = batchCount(parsedFile.transactions().size());
                statementFile.markProcessing(parsedFile.parserName(), parsedFile.transactions().size(), fileTotalBatches);
                statementFile = statementFileRepository.save(statementFile);
                acceptedStatementFileIds.add(statementFile.getId());
                pendingMessages.addAll(batchMessages(analysis, statementFile, parsedFile.transactions(), fileTotalBatches));
            } else {
                statementFile.markFailed(parsedFile.errorMessage());
                statementFileRepository.save(statementFile);
            }
        }

        publishAfterCommit(pendingMessages, analysis.getId(), acceptedStatementFileIds);

        return CreateAnalysisResponse.builder()
                .id(analysis.getId())
                .title(analysis.getTitle())
                .status(analysis.getStatus())
                .totalFiles(request.files().size())
                .acceptedFiles(acceptedFiles.size())
                .rejectedFiles(parsedFiles.size() - acceptedFiles.size())
                .message("Analysis created. Files are being processed asynchronously.")
                .build();
    }

    private Analysis persistFailedAnalysis(CreateAnalysisRequest request, List<ParsedUploadedFile> parsedFiles) {
        Analysis analysis = Analysis.create(request.title());
        analysis.markFailed(request.files().size(), "None of the uploaded CSV files could be parsed. Each file must contain transaction date, description and amount information.");
        analysis = analysisRepository.save(analysis);

        for (ParsedUploadedFile parsedFile : parsedFiles) {
            StatementFile statementFile = StatementFile.create(
                    analysis,
                    parsedFile.title(),
                    parsedFile.originalFilename()
            );
            statementFile.markFailed(parsedFile.errorMessage());
            statementFileRepository.save(statementFile);
        }

        return analysis;
    }

    private static UnsupportedCsvFormatException noAcceptedFilesException(
            List<ParsedUploadedFile> parsedFiles,
            UUID analysisId
    ) {
        UploadRejectionReason reason = dominantRejectionReason(parsedFiles);
        String message = reason.noAcceptedFilesMessage();

        return switch (reason) {
            case FILE_TOO_LARGE -> UnsupportedCsvFormatException.fileTooLarge(message, analysisId);
            case UNSUPPORTED_FILE_TYPE -> UnsupportedCsvFormatException.unsupportedFileType(message, analysisId);
            case EMPTY_FILE -> UnsupportedCsvFormatException.emptyCsvFile(message, analysisId);
            case UNSUPPORTED_CSV_FORMAT -> new UnsupportedCsvFormatException(message, analysisId);
        };
    }

    private static UploadRejectionReason dominantRejectionReason(List<ParsedUploadedFile> parsedFiles) {
        if (parsedFiles.stream().anyMatch(file -> file.rejectionReason() == UploadRejectionReason.FILE_TOO_LARGE)) {
            return UploadRejectionReason.FILE_TOO_LARGE;
        }

        if (parsedFiles.stream().allMatch(file -> file.rejectionReason() == UploadRejectionReason.UNSUPPORTED_FILE_TYPE)) {
            return UploadRejectionReason.UNSUPPORTED_FILE_TYPE;
        }

        if (parsedFiles.stream().allMatch(file -> file.rejectionReason() == UploadRejectionReason.EMPTY_FILE)) {
            return UploadRejectionReason.EMPTY_FILE;
        }

        return UploadRejectionReason.UNSUPPORTED_CSV_FORMAT;
    }

    private static void validateRequest(CreateAnalysisRequest request) {
        if (request.files() == null || request.files().isEmpty()) {
            throw new InvalidAnalysisRequestException("At least one CSV file is required.");
        }

        if (request.files().size() > 10) {
            throw new InvalidAnalysisRequestException("An analysis can contain at most 10 files.");
        }

        if (request.fileTitles() != null && request.fileTitles().size() != request.files().size()) {
            throw new InvalidAnalysisRequestException("File titles size must match uploaded files size.");
        }
    }

    private List<ParsedUploadedFile> parseFiles(CreateAnalysisRequest request) {
        List<ParsedUploadedFile> parsedFiles = new ArrayList<>();

        for (int index = 0; index < request.files().size(); index++) {
            MultipartFile file = request.files().get(index);
            String originalFilename = originalFilename(file);
            String title = fileTitle(request.fileTitles(), index, originalFilename);
            parsedFiles.add(parseFile(file, title, originalFilename));
        }

        return parsedFiles;
    }

    private ParsedUploadedFile parseFile(MultipartFile file, String title, String originalFilename) {
        if (file == null || file.isEmpty()) {
            return ParsedUploadedFile.failed(title, originalFilename, "CSV file must not be empty.", UploadRejectionReason.EMPTY_FILE);
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ParsedUploadedFile.failed(title, originalFilename, "Each CSV file must be at most 5 MB.", UploadRejectionReason.FILE_TOO_LARGE);
        }

        if (!isCsvUpload(originalFilename, file.getContentType())) {
            return ParsedUploadedFile.failed(title, originalFilename, "Only CSV files are accepted.", UploadRejectionReason.UNSUPPORTED_FILE_TYPE);
        }

        try {
            UploadedCsvFile uploadedCsvFile = UploadedCsvFile.builder()
                    .originalFilename(originalFilename)
                    .content(file.getBytes())
                    .build();

            StatementCsvParser parser = parserResolver.resolve(uploadedCsvFile);
            ParsedStatement parsedStatement = parser.parse(uploadedCsvFile);

            if (parsedStatement.transactions().isEmpty()) {
                return ParsedUploadedFile.failed(
                        title,
                        originalFilename,
                        "CSV file does not contain valid transactions.",
                        UploadRejectionReason.UNSUPPORTED_CSV_FORMAT
                );
            }

            return ParsedUploadedFile.accepted(
                    title,
                    originalFilename,
                    parsedStatement.parserName(),
                    parsedStatement.transactions()
            );
        } catch (IOException e) {
            return ParsedUploadedFile.failed(title, originalFilename, "CSV file could not be read.", UploadRejectionReason.UNSUPPORTED_CSV_FORMAT);
        } catch (UnsupportedCsvFormatException e) {
            return ParsedUploadedFile.failed(title, originalFilename, e.getMessage(), UploadRejectionReason.UNSUPPORTED_CSV_FORMAT);
        }
    }

    private List<TransactionBatchMessage> batchMessages(Analysis analysis, StatementFile statementFile, List<NormalizedTransaction> transactions, int totalBatches) {
        List<TransactionBatchMessage> messages = new ArrayList<>();
        for (int start = 0; start < transactions.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, transactions.size());
            int batchNumber = start / BATCH_SIZE + 1;

            List<TransactionBatchMessage.TransactionMessage> batchTransactions = transactions.subList(start, end).stream()
                    .map(transaction -> TransactionBatchMessage.TransactionMessage.builder()
                            .description(databaseText(transaction.description()))
                            .amount(transaction.amount())
                            .transactionDate(transaction.transactionDate())
                            .build())
                    .toList();

            messages.add(TransactionBatchMessage.builder()
                    .analysisId(analysis.getId())
                    .statementFileId(statementFile.getId())
                    .batchNumber(batchNumber)
                    .totalBatches(totalBatches)
                    .transactions(batchTransactions)
                    .build());
        }

        return messages;
    }

    private void publishAfterCommit(
            List<TransactionBatchMessage> messages,
            UUID analysisId,
            List<UUID> acceptedStatementFileIds
    ) {
        if (messages.isEmpty()) {
            return;
        }

        List<TransactionBatchMessage> messagesToPublish = List.copyOf(messages);
        List<UUID> statementFileIds = List.copyOf(acceptedStatementFileIds);
        Runnable publish = () -> {
            try {
                messagesToPublish.forEach(batchPublisher::publish);
            } catch (RuntimeException e) {
                publishFailureHandler.markPublishingFailed(analysisId, statementFileIds, publishFailureMessage(e));
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }

    private static String publishFailureMessage(RuntimeException e) {
        String reason = e.getMessage();

        if (reason == null || reason.isBlank()) {
            reason = e.getClass().getSimpleName();
        }

        return databaseText("Transaction batches could not be published: " + reason);
    }

    private static String databaseText(String value) {
        if (value == null || value.length() <= MAX_DATABASE_TEXT_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_DATABASE_TEXT_LENGTH);
    }

    private static int batchCount(int transactions) {
        return (int) Math.ceil(transactions / (double) BATCH_SIZE);
    }

    private static String fileTitle(List<String> fileTitles, int index, String originalFilename) {
        if (fileTitles != null) {
            String providedTitle = fileTitles.get(index);

            if (providedTitle != null && !providedTitle.isBlank()) {
                return providedTitle.trim();
            }
        }

        return filenameWithoutExtension(originalFilename);
    }

    private static String filenameWithoutExtension(String filename) {
        int extensionStart = filename.lastIndexOf('.');

        if (extensionStart <= 0) {
            return filename;
        }

        return filename.substring(0, extensionStart);
    }

    private static String originalFilename(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return "statement.csv";
        }

        return file.getOriginalFilename();
    }

    private static boolean isCsvFilename(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private static boolean isCsvUpload(String filename, String contentType) {
        if (!isCsvFilename(filename)) {
            return false;
        }

        if (contentType == null || contentType.isBlank()) {
            return true;
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        int parametersStart = normalizedContentType.indexOf(';');
        if (parametersStart >= 0) {
            normalizedContentType = normalizedContentType.substring(0, parametersStart);
        }

        return CSV_CONTENT_TYPES.contains(normalizedContentType.trim());
    }

    private enum UploadRejectionReason {
        FILE_TOO_LARGE("Each CSV file must be at most 5 MB."),
        UNSUPPORTED_FILE_TYPE("Only CSV files are accepted."),
        EMPTY_FILE("CSV file must not be empty."),
        UNSUPPORTED_CSV_FORMAT("None of the uploaded CSV files could be parsed. Each file must contain transaction date, description and amount information.");

        private final String noAcceptedFilesMessage;

        UploadRejectionReason(String noAcceptedFilesMessage) {
            this.noAcceptedFilesMessage = noAcceptedFilesMessage;
        }

        String noAcceptedFilesMessage() {
            return noAcceptedFilesMessage;
        }
    }

    @Builder
    private record ParsedUploadedFile(
            String title,
            String originalFilename,
            boolean accepted,
            String parserName,
            List<NormalizedTransaction> transactions,
            String errorMessage,
            UploadRejectionReason rejectionReason
    ) {
        static ParsedUploadedFile accepted(
                String title,
                String originalFilename,
                String parserName,
                List<NormalizedTransaction> transactions
        ) {
            return ParsedUploadedFile.builder()
                    .title(title)
                    .originalFilename(originalFilename)
                    .accepted(true)
                    .parserName(parserName)
                    .transactions(transactions)
                    .build();
        }

        static ParsedUploadedFile failed(
                String title,
                String originalFilename,
                String errorMessage,
                UploadRejectionReason rejectionReason
        ) {
            return ParsedUploadedFile.builder()
                    .title(title)
                    .originalFilename(originalFilename)
                    .accepted(false)
                    .transactions(List.of())
                    .errorMessage(errorMessage)
                    .rejectionReason(rejectionReason)
                    .build();
        }
    }
}
