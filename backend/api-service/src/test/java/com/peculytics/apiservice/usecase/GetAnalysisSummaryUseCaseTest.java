package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.dto.GetAnalysisSummaryResponse;
import com.peculytics.apiservice.exception.AnalysisNotFoundException;
import com.peculytics.apiservice.model.TransactionCategory;
import com.peculytics.apiservice.repository.AnalysisRepository;
import com.peculytics.apiservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAnalysisSummaryUseCaseTest {
    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GetAnalysisSummaryUseCase useCase;

    @Test
    void shouldCalculateTotalExpensesAndCategoryPercentages() {
        UUID analysisId = UUID.randomUUID();
        when(analysisRepository.existsById(analysisId)).thenReturn(true);
        when(transactionRepository.summarizeExpensesByCategory(analysisId)).thenReturn(List.of(
                summary(TransactionCategory.FOOD, "10.005"),
                summary(TransactionCategory.TRANSPORT, "5.005")
        ));

        GetAnalysisSummaryResponse response = useCase.execute(analysisId);

        assertThat(response.analysisId()).isEqualTo(analysisId);
        assertThat(response.totalExpenses()).isEqualByComparingTo("15.01");
        assertThat(response.categories())
                .extracting(
                        GetAnalysisSummaryResponse.CategorySummary::category,
                        GetAnalysisSummaryResponse.CategorySummary::total,
                        GetAnalysisSummaryResponse.CategorySummary::percentage
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Food", new BigDecimal("10.01"), new BigDecimal("66.69")),
                        org.assertj.core.groups.Tuple.tuple("Transport", new BigDecimal("5.01"), new BigDecimal("33.38"))
                );
    }

    @Test
    void shouldReturnZeroTotalAndNoCategoriesWhenThereAreNoExpenses() {
        UUID analysisId = UUID.randomUUID();
        when(analysisRepository.existsById(analysisId)).thenReturn(true);
        when(transactionRepository.summarizeExpensesByCategory(analysisId)).thenReturn(List.of());

        GetAnalysisSummaryResponse response = useCase.execute(analysisId);

        assertThat(response.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(response.categories()).isEmpty();
    }

    @Test
    void shouldThrowWhenAnalysisDoesNotExist() {
        UUID analysisId = UUID.randomUUID();
        when(analysisRepository.existsById(analysisId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(analysisId))
                .isInstanceOf(AnalysisNotFoundException.class)
                .hasMessageContaining(analysisId.toString());

        verify(transactionRepository, never()).summarizeExpensesByCategory(analysisId);
    }

    private static TransactionRepository.CategoryExpenseSummaryProjection summary(
            TransactionCategory category,
            String total
    ) {
        return new TransactionRepository.CategoryExpenseSummaryProjection() {
            @Override
            public TransactionCategory getCategory() {
                return category;
            }

            @Override
            public BigDecimal getTotal() {
                return new BigDecimal(total);
            }
        };
    }
}
