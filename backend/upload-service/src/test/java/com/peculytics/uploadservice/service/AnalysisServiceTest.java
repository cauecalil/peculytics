package com.peculytics.uploadservice.service;

import com.peculytics.uploadservice.csv.NormalizedTransaction;
import com.peculytics.uploadservice.csv.ParsedStatement;
import com.peculytics.uploadservice.csv.StatementCsvParser;
import com.peculytics.uploadservice.csv.StatementCsvParserResolver;
import com.peculytics.uploadservice.csv.UploadedCsvFile;
import com.peculytics.uploadservice.dto.CreateAnalysisRequest;
import com.peculytics.uploadservice.exceptions.InvalidAnalysisRequestException;
import com.peculytics.uploadservice.exceptions.UnsupportedCsvFormatException;
import com.peculytics.uploadservice.messaging.TransactionBatchMessage;
import com.peculytics.uploadservice.messaging.TransactionBatchPublisher;
import com.peculytics.uploadservice.model.Analysis;
import com.peculytics.uploadservice.model.AnalysisStatus;
import com.peculytics.uploadservice.model.StatementFile;
import com.peculytics.uploadservice.model.StatementFileStatus;
import com.peculytics.uploadservice.repository.AnalysisRepository;
import com.peculytics.uploadservice.repository.StatementFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {
    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private StatementFileRepository statementFileRepository;

    @Mock
    private StatementCsvParserResolver parserResolver;

    @Mock
    private TransactionBatchPublisher batchPublisher;

    @Mock
    private PublishFailureHandler publishFailureHandler;

    @Mock
    private StatementCsvParser parser;

    @InjectMocks
    private AnalysisService service;

    @Test
    void shouldRejectRequestWithoutFiles() {
        CreateAnalysisRequest request = CreateAnalysisRequest.builder()
                .title("May statement")
                .files(List.of())
                .build();

        assertThatThrownBy(() -> service.createAnalysis(request))
                .isInstanceOf(InvalidAnalysisRequestException.class)
                .hasMessage("At least one CSV file is required.");

        verifyNoInteractions(analysisRepository, statementFileRepository, parserResolver, batchPublisher, publishFailureHandler);
    }

    @Test
    void shouldCreateAnalysisAndPublishTransactionsInBatches() {
        UUID analysisId = UUID.randomUUID();
        UUID statementFileId = UUID.randomUUID();
        List<NormalizedTransaction> transactions = transactions(51);
        mockSavedAnalysisWithId(analysisId);
        mockSavedStatementFileWithId(statementFileId);
        when(parserResolver.resolve(any(UploadedCsvFile.class))).thenReturn(parser);
        when(parser.parse(any(UploadedCsvFile.class))).thenReturn(ParsedStatement.builder()
                .parserName("GENERIC_HEADER_CSV")
                .transactions(transactions)
                .build());
        CreateAnalysisRequest request = CreateAnalysisRequest.builder()
                .title("May statement")
                .files(List.of(csv("statement.csv", "text/csv")))
                .fileTitles(List.of("Checking account"))
                .build();

        var response = service.createAnalysis(request);

        ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        verify(analysisRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.PROCESSING);
        assertThat(analysisCaptor.getValue().getTotalFiles()).isEqualTo(1);
        assertThat(analysisCaptor.getValue().getTotalTransactions()).isEqualTo(51);
        assertThat(analysisCaptor.getValue().getTotalBatches()).isEqualTo(2);

        ArgumentCaptor<StatementFile> statementFileCaptor = ArgumentCaptor.forClass(StatementFile.class);
        verify(statementFileRepository).save(statementFileCaptor.capture());
        assertThat(statementFileCaptor.getValue().getTitle()).isEqualTo("Checking account");
        assertThat(statementFileCaptor.getValue().getStatus()).isEqualTo(StatementFileStatus.PROCESSING);
        assertThat(statementFileCaptor.getValue().getParserName()).isEqualTo("GENERIC_HEADER_CSV");

        ArgumentCaptor<TransactionBatchMessage> messageCaptor = ArgumentCaptor.forClass(TransactionBatchMessage.class);
        verify(batchPublisher, org.mockito.Mockito.times(2)).publish(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues())
                .extracting(TransactionBatchMessage::batchNumber)
                .containsExactly(1, 2);
        assertThat(messageCaptor.getAllValues().get(0).analysisId()).isEqualTo(analysisId);
        assertThat(messageCaptor.getAllValues().get(0).statementFileId()).isEqualTo(statementFileId);
        assertThat(messageCaptor.getAllValues().get(0).totalBatches()).isEqualTo(2);
        assertThat(messageCaptor.getAllValues().get(0).transactions()).hasSize(50);
        assertThat(messageCaptor.getAllValues().get(1).transactions()).hasSize(1);

        assertThat(response.id()).isEqualTo(analysisId);
        assertThat(response.acceptedFiles()).isEqualTo(1);
        assertThat(response.rejectedFiles()).isZero();
    }

    @Test
    void shouldPersistFailedAnalysisAndThrowWhenNoFileIsAccepted() {
        UUID analysisId = UUID.randomUUID();
        mockSavedAnalysisWithId(analysisId);
        when(statementFileRepository.save(any(StatementFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateAnalysisRequest request = CreateAnalysisRequest.builder()
                .title("May statement")
                .files(List.of(csv("notes.txt", "text/plain")))
                .build();

        UnsupportedCsvFormatException exception = catchThrowableOfType(
                () -> service.createAnalysis(request),
                UnsupportedCsvFormatException.class
        );

        assertThat(exception).isNotNull();
        assertThat(exception.analysisId()).isEqualTo(analysisId);
        assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(exception.errorCode()).isEqualTo("unsupported-file-type");
        verify(parserResolver, never()).resolve(any(UploadedCsvFile.class));
        verify(batchPublisher, never()).publish(any(TransactionBatchMessage.class));

        ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        verify(analysisRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysisCaptor.getValue().getTotalFiles()).isEqualTo(1);

        ArgumentCaptor<StatementFile> statementFileCaptor = ArgumentCaptor.forClass(StatementFile.class);
        verify(statementFileRepository).save(statementFileCaptor.capture());
        assertThat(statementFileCaptor.getValue().getTitle()).isEqualTo("notes");
        assertThat(statementFileCaptor.getValue().getStatus()).isEqualTo(StatementFileStatus.FAILED);
        assertThat(statementFileCaptor.getValue().getErrorMessage()).isEqualTo("Only CSV files are accepted.");
    }

    @Test
    void shouldDelegatePublishingFailureToFailureHandler() {
        UUID analysisId = UUID.randomUUID();
        UUID statementFileId = UUID.randomUUID();
        mockSavedAnalysisWithId(analysisId);
        mockSavedStatementFileWithId(statementFileId);
        when(parserResolver.resolve(any(UploadedCsvFile.class))).thenReturn(parser);
        when(parser.parse(any(UploadedCsvFile.class))).thenReturn(ParsedStatement.builder()
                .parserName("GENERIC_HEADER_CSV")
                .transactions(transactions(1))
                .build());
        doThrow(new RuntimeException("broker down"))
                .when(batchPublisher)
                .publish(any(TransactionBatchMessage.class));
        CreateAnalysisRequest request = CreateAnalysisRequest.builder()
                .title("May statement")
                .files(List.of(csv("statement.csv", "text/csv")))
                .build();

        service.createAnalysis(request);

        verify(publishFailureHandler).markPublishingFailed(
                org.mockito.ArgumentMatchers.eq(analysisId),
                org.mockito.ArgumentMatchers.eq(List.of(statementFileId)),
                org.mockito.ArgumentMatchers.contains("broker down")
        );
    }

    private void mockSavedAnalysisWithId(UUID analysisId) {
        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis input = invocation.getArgument(0);
            return Analysis.builder()
                    .id(analysisId)
                    .title(input.getTitle())
                    .status(input.getStatus())
                    .totalFiles(input.getTotalFiles())
                    .totalTransactions(input.getTotalTransactions())
                    .processedBatches(input.getProcessedBatches())
                    .totalBatches(input.getTotalBatches())
                    .errorMessage(input.getErrorMessage())
                    .createdAt(input.getCreatedAt())
                    .completedAt(input.getCompletedAt())
                    .build();
        });
    }

    private void mockSavedStatementFileWithId(UUID statementFileId) {
        when(statementFileRepository.save(any(StatementFile.class))).thenAnswer(invocation -> {
            StatementFile input = invocation.getArgument(0);
            return StatementFile.builder()
                    .id(statementFileId)
                    .analysis(input.getAnalysis())
                    .title(input.getTitle())
                    .fileName(input.getFileName())
                    .status(input.getStatus())
                    .totalTransactions(input.getTotalTransactions())
                    .processedBatches(input.getProcessedBatches())
                    .totalBatches(input.getTotalBatches())
                    .parserName(input.getParserName())
                    .errorMessage(input.getErrorMessage())
                    .createdAt(input.getCreatedAt())
                    .completedAt(input.getCompletedAt())
                    .build();
        });
    }

    private static MockMultipartFile csv(String originalFilename, String contentType) {
        return new MockMultipartFile(
                "files",
                originalFilename,
                contentType,
                "Date,Description,Amount\n2026-05-06,Coffee,10.00\n".getBytes()
        );
    }

    private static List<NormalizedTransaction> transactions(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> NormalizedTransaction.builder()
                        .transactionDate(LocalDate.of(2026, 5, 6))
                        .description("Transaction " + index)
                        .amount(new BigDecimal("10.00"))
                        .build())
                .toList();
    }
}
