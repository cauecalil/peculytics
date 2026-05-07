package com.peculytics.apiservice.usecase;

import com.peculytics.apiservice.dto.GetAnalysisTransactionsResponse;
import com.peculytics.apiservice.exception.AnalysisNotFoundException;
import com.peculytics.apiservice.exception.InvalidPaginationParameterException;
import com.peculytics.apiservice.model.Transaction;
import com.peculytics.apiservice.repository.AnalysisRepository;
import com.peculytics.apiservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAnalysisTransactionsUseCase {
    private final AnalysisRepository analysisRepository;
    private final TransactionRepository transactionRepository;

    public GetAnalysisTransactionsResponse execute(UUID analysisId, int page, int size) {
        if (page < 0) {
            throw new InvalidPaginationParameterException("Page must be greater than or equal to zero.");
        }

        if (size <= 0) {
            throw new InvalidPaginationParameterException("Size must be greater than zero.");
        }

        if (!analysisRepository.existsById(analysisId)) {
            throw new AnalysisNotFoundException(analysisId);
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("transactionDate"),
                Sort.Order.desc("id")
        ));

        Page<Transaction> transactions = transactionRepository.findByAnalysisId(analysisId, pageRequest);

        List<GetAnalysisTransactionsResponse.Transaction> transactionsList = transactions.getContent().stream()
                .map(transaction -> GetAnalysisTransactionsResponse.Transaction.builder()
                        .id(transaction.getId())
                        .description(transaction.getDescription())
                        .amount(transaction.getAmount())
                        .transactionDate(transaction.getTransactionDate())
                        .category(transaction.getCategory().getLabel())
                        .categorySource(transaction.getCategorySource())
                        .createdAt(transaction.getCreatedAt())
                        .sourceFile(
                                GetAnalysisTransactionsResponse.SourceFile.builder()
                                        .id(transaction.getStatementFile().getId())
                                        .title(transaction.getStatementFile().getTitle())
                                        .build()
                        )
                        .build())
                .toList();

        return GetAnalysisTransactionsResponse.builder()
                .content(transactionsList)
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .page(transactions.getNumber())
                .size(transactions.getSize())
                .build();
    }
}
