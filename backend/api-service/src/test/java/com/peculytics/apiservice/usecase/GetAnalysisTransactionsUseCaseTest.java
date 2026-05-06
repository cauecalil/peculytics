package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.dto.GetAnalysisTransactionsResponse;
import com.peculytics.apiservice.exception.AnalysisNotFoundException;
import com.peculytics.apiservice.model.StatementFile;
import com.peculytics.apiservice.model.Transaction;
import com.peculytics.apiservice.model.TransactionCategory;
import com.peculytics.apiservice.model.TransactionCategorySource;
import com.peculytics.apiservice.repository.AnalysisRepository;
import com.peculytics.apiservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAnalysisTransactionsUseCaseTest {
    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GetAnalysisTransactionsUseCase useCase;

    @Test
    void shouldMapTransactionsAndRequestExpectedPageSort() {
        UUID analysisId = UUID.randomUUID();
        Transaction transaction = transaction();
        when(analysisRepository.existsById(analysisId)).thenReturn(true);
        when(transactionRepository.findByAnalysisId(
                org.mockito.ArgumentMatchers.eq(analysisId),
                org.mockito.ArgumentMatchers.any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(transaction), PageRequest.of(1, 25), 51));

        GetAnalysisTransactionsResponse response = useCase.execute(analysisId, 1, 25);

        assertThat(response.totalElements()).isEqualTo(51);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(25);
        assertThat(response.content()).singleElement().satisfies(mapped -> {
            assertThat(mapped.id()).isEqualTo(transaction.getId());
            assertThat(mapped.description()).isEqualTo("Market");
            assertThat(mapped.amount()).isEqualByComparingTo("-42.30");
            assertThat(mapped.transactionDate()).isEqualTo(LocalDate.of(2026, 1, 20));
            assertThat(mapped.category()).isEqualTo("Groceries");
            assertThat(mapped.categorySource()).isEqualTo(TransactionCategorySource.RULE);
            assertThat(mapped.createdAt()).isEqualTo(Instant.parse("2026-01-21T10:15:30Z"));
            assertThat(mapped.sourceFile().id()).isEqualTo(transaction.getStatementFile().getId());
            assertThat(mapped.sourceFile().title()).isEqualTo("January statement");
        });

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(transactionRepository).findByAnalysisId(org.mockito.ArgumentMatchers.eq(analysisId), pageRequestCaptor.capture());
        PageRequest pageRequest = pageRequestCaptor.getValue();

        assertThat(pageRequest.getPageNumber()).isEqualTo(1);
        assertThat(pageRequest.getPageSize()).isEqualTo(25);
        assertThat(pageRequest.getSort().getOrderFor("transactionDate"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
        assertThat(pageRequest.getSort().getOrderFor("id"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldThrowWhenAnalysisDoesNotExist() {
        UUID analysisId = UUID.randomUUID();
        when(analysisRepository.existsById(analysisId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(analysisId, 0, 50))
                .isInstanceOf(AnalysisNotFoundException.class)
                .hasMessageContaining(analysisId.toString());

        verify(transactionRepository, never()).findByAnalysisId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private static Transaction transaction() {
        UUID transactionId = UUID.randomUUID();
        UUID statementFileId = UUID.randomUUID();

        StatementFile statementFile = mock(StatementFile.class);
        when(statementFile.getId()).thenReturn(statementFileId);
        when(statementFile.getTitle()).thenReturn("January statement");

        Transaction transaction = mock(Transaction.class);
        when(transaction.getId()).thenReturn(transactionId);
        when(transaction.getDescription()).thenReturn("Market");
        when(transaction.getAmount()).thenReturn(new BigDecimal("-42.30"));
        when(transaction.getTransactionDate()).thenReturn(LocalDate.of(2026, 1, 20));
        when(transaction.getCategory()).thenReturn(TransactionCategory.GROCERIES);
        when(transaction.getCategorySource()).thenReturn(TransactionCategorySource.RULE);
        when(transaction.getCreatedAt()).thenReturn(Instant.parse("2026-01-21T10:15:30Z"));
        when(transaction.getStatementFile()).thenReturn(statementFile);

        return transaction;
    }
}
