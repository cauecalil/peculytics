package com.peculytics.uploadservice.csv;

import com.peculytics.uploadservice.exceptions.UnsupportedCsvFormatException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenericHeaderStatementCsvParser implements StatementCsvParser {
    private static final String PARSER_NAME = "GENERIC_HEADER_CSV";

    private static final Set<String> DATE_HEADERS = normalizedSet(
            "date",
            "data",
            "transaction_date",
            "transaction date",
            "data_transacao",
            "data transacao",
            "data transação",
            "data lançamento",
            "data lancamento",
            "posted date"
    );
    private static final Set<String> DESCRIPTION_HEADERS = normalizedSet(
            "description",
            "descricao",
            "descrição",
            "historico",
            "histórico",
            "details",
            "merchant",
            "memo"
    );
    private static final Set<String> AMOUNT_HEADERS = normalizedSet(
            "amount",
            "valor",
            "quantia",
            "value",
            "transaction_amount",
            "transaction amount"
    );
    private static final Set<String> DEBIT_HEADERS = normalizedSet(
            "debit",
            "débito",
            "debito",
            "withdrawal",
            "spent"
    );
    private static final Set<String> CREDIT_HEADERS = normalizedSet(
            "credit",
            "crédito",
            "credito",
            "deposit",
            "received"
    );
    private static final Set<String> TYPE_HEADERS = normalizedSet(
            "type",
            "tipo",
            "operation",
            "transaction_type"
    );
    private static final Set<String> DEBIT_TYPES = normalizedSet(
            "debit",
            "débito",
            "debito",
            "withdrawal",
            "spent"
    );
    private static final Set<String> CREDIT_TYPES = normalizedSet(
            "credit",
            "crédito",
            "credito",
            "deposit",
            "received"
    );

    private final CsvStructureDetector structureDetector;
    private final DateParser dateParser;
    private final MoneyParser moneyParser;

    @Override
    public boolean supports(CsvSample sample) {
        List<String> normalizedHeaders = sample.headers().stream()
                .map(HeaderNormalizer::normalize)
                .toList();

        try {
            resolveHeaders(normalizedHeaders);
            return true;
        } catch (UnsupportedCsvFormatException e) {
            return false;
        }
    }

    @Override
    public ParsedStatement parse(UploadedCsvFile file) {
        CsvDocument document = structureDetector.detect(file);
        HeaderMapping mapping = resolveHeaders(document.headers());

        List<NormalizedTransaction> transactions = new ArrayList<>();
        int rowNumber = 1;
        for (Map<String, String> row : document.rows()) {
            rowNumber++;
            Optional<NormalizedTransaction> transaction = parseRow(row, mapping);
            if (transaction.isPresent()) {
                transactions.add(transaction.get());
            } else {
                log.warn("Ignoring invalid CSV row {} from file {}", rowNumber, file.originalFilename());
            }
        }

        return ParsedStatement.builder()
                .transactions(transactions)
                .parserName(PARSER_NAME)
                .build();
    }

    @Override
    public String parserName() {
        return PARSER_NAME;
    }

    private Optional<NormalizedTransaction> parseRow(Map<String, String> row, HeaderMapping mapping) {
        String dateValue = row.get(mapping.dateHeader());
        String description = row.get(mapping.descriptionHeader());

        if (description == null || description.isBlank()) {
            return Optional.empty();
        }

        return dateParser.parse(dateValue)
                .flatMap(date -> resolveAmount(row, mapping)
                        .map(amount -> NormalizedTransaction.builder()
                                .transactionDate(date)
                                .description(description.trim())
                                .amount(amount)
                                .build()));
    }

    private Optional<BigDecimal> resolveAmount(Map<String, String> row, HeaderMapping mapping) {
        if (mapping.amountHeader() != null) {
            Optional<BigDecimal> parsedAmount = moneyParser.parse(row.get(mapping.amountHeader()));

            if (parsedAmount.isEmpty()) {
                return Optional.empty();
            }

            if (mapping.typeHeader() == null) {
                return parsedAmount;
            }

            String normalizedType = HeaderNormalizer.normalize(row.get(mapping.typeHeader()));

            if (DEBIT_TYPES.contains(normalizedType)) {
                return Optional.of(parsedAmount.get().abs().negate());
            }
            if (CREDIT_TYPES.contains(normalizedType)) {
                return Optional.of(parsedAmount.get().abs());
            }

            return parsedAmount;
        }

        Optional<BigDecimal> debit = moneyParser.parse(row.get(mapping.debitHeader()));

        if (debit.isPresent()) {
            return Optional.of(debit.get().abs().negate());
        }

        return moneyParser.parse(row.get(mapping.creditHeader()))
                .map(BigDecimal::abs);
    }

    private static HeaderMapping resolveHeaders(List<String> headers) {
        String dateHeader = findHeader(headers, DATE_HEADERS).orElseThrow(() -> new UnsupportedCsvFormatException("CSV must contain a transaction date column."));
        String descriptionHeader = findHeader(headers, DESCRIPTION_HEADERS).orElseThrow(() -> new UnsupportedCsvFormatException("CSV must contain a description column."));
        String amountHeader = findHeader(headers, AMOUNT_HEADERS).orElse(null);
        String debitHeader = findHeader(headers, DEBIT_HEADERS).orElse(null);
        String creditHeader = findHeader(headers, CREDIT_HEADERS).orElse(null);
        String typeHeader = findHeader(headers, TYPE_HEADERS).orElse(null);

        if (amountHeader == null && debitHeader == null && creditHeader == null) {
            throw new UnsupportedCsvFormatException("CSV must contain amount information.");
        }

        return HeaderMapping.builder()
                .dateHeader(dateHeader)
                .descriptionHeader(descriptionHeader)
                .amountHeader(amountHeader)
                .debitHeader(debitHeader)
                .creditHeader(creditHeader)
                .typeHeader(typeHeader)
                .build();
    }

    private static Optional<String> findHeader(List<String> headers, Set<String> synonyms) {
        return headers.stream()
                .filter(synonyms::contains)
                .findFirst();
    }

    private static Set<String> normalizedSet(String... values) {
        return java.util.Arrays.stream(values)
                .map(HeaderNormalizer::normalize)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Builder
    private record HeaderMapping(
            String dateHeader,
            String descriptionHeader,
            String amountHeader,
            String debitHeader,
            String creditHeader,
            String typeHeader
    ) {}
}
