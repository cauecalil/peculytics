package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.dto.GetAnalysisSummaryResponse;
import com.peculytics.apiservice.exception.AnalysisNotFoundException;
import com.peculytics.apiservice.repository.AnalysisRepository;
import com.peculytics.apiservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAnalysisSummaryUseCase {
    private final AnalysisRepository analysisRepository;
    private final TransactionRepository transactionRepository;

    public GetAnalysisSummaryResponse execute(UUID analysisId) {
        if (!analysisRepository.existsById(analysisId)) {
            throw new AnalysisNotFoundException(analysisId);
        }

        List<TransactionRepository.CategoryExpenseSummaryProjection> summaries = transactionRepository.summarizeExpensesByCategory(analysisId);

        BigDecimal totalExpenses = summaries.stream()
                .map(TransactionRepository.CategoryExpenseSummaryProjection::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<GetAnalysisSummaryResponse.CategorySummary> categories = summaries.stream()
                .map(summary -> {
                    BigDecimal total = summary.getTotal().setScale(2, RoundingMode.HALF_UP);

                    BigDecimal percentage = totalExpenses.signum() == 0
                            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            : total.multiply(BigDecimal.valueOf(100)).divide(totalExpenses, 2, RoundingMode.HALF_UP);

                    return GetAnalysisSummaryResponse.CategorySummary.builder()
                            .category(summary.getCategory().getLabel())
                            .total(total)
                            .percentage(percentage)
                            .build();
                })
                .toList();

        return GetAnalysisSummaryResponse.builder()
                .analysisId(analysisId)
                .totalExpenses(totalExpenses)
                .categories(categories)
                .build();
    }
}
