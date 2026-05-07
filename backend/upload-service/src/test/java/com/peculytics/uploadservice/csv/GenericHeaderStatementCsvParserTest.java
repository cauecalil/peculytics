package com.peculytics.uploadservice.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericHeaderStatementCsvParserTest {
    @Mock
    private CsvStructureDetector structureDetector;

    @Spy
    private DateParser dateParser = new DateParser();

    @Spy
    private MoneyParser moneyParser = new MoneyParser();

    @InjectMocks
    private GenericHeaderStatementCsvParser parser;

    @Test
    void shouldSupportCsvWithKnownPortugueseHeaders() {
        CsvSample sample = CsvSample.builder()
                .headers(List.of("Data Lançamento", "Histórico", "Valor"))
                .rows(List.of())
                .delimiter(';')
                .build();

        boolean supported = parser.supports(sample);

        assertThat(supported).isTrue();
    }

    @Test
    void shouldNotSupportCsvWithoutAmountInformation() {
        CsvSample sample = CsvSample.builder()
                .headers(List.of("Data", "Descrição"))
                .rows(List.of())
                .delimiter(';')
                .build();

        boolean supported = parser.supports(sample);

        assertThat(supported).isFalse();
    }

    @Test
    void shouldParseAmountColumnAndApplyTransactionTypeSign() {
        UploadedCsvFile file = uploadedFile();
        CsvDocument document = CsvDocument.builder()
                .delimiter(';')
                .headers(List.of("data", "descricao", "valor", "tipo"))
                .rows(List.of(
                        row("data", "06/05/2026", "descricao", " Supermarket ", "valor", "12,34", "tipo", "débito"),
                        row("data", "07/05/2026", "descricao", "Salary", "valor", "55,00", "tipo", "crédito"),
                        row("data", "08/05/2026", "descricao", " ", "valor", "10,00", "tipo", "crédito")
                ))
                .build();
        when(structureDetector.detect(file)).thenReturn(document);

        ParsedStatement statement = parser.parse(file);

        assertThat(statement.parserName()).isEqualTo("GENERIC_HEADER_CSV");
        assertThat(statement.transactions()).hasSize(2);
        assertThat(statement.transactions().get(0).description()).isEqualTo("Supermarket");
        assertThat(statement.transactions().get(0).transactionDate()).isEqualTo(LocalDate.of(2026, 5, 6));
        assertThat(statement.transactions().get(0).amount()).isEqualByComparingTo(new BigDecimal("-12.34"));
        assertThat(statement.transactions().get(1).amount()).isEqualByComparingTo(new BigDecimal("55.00"));
    }

    @Test
    void shouldParseDebitAndCreditColumnsWhenAmountColumnIsAbsent() {
        UploadedCsvFile file = uploadedFile();
        CsvDocument document = CsvDocument.builder()
                .delimiter(';')
                .headers(List.of("data", "descricao", "debito", "credito"))
                .rows(List.of(
                        row("data", "06/05/2026", "descricao", "Rent", "debito", "1.200,00", "credito", ""),
                        row("data", "07/05/2026", "descricao", "Refund", "debito", "", "credito", "25,00")
                ))
                .build();
        when(structureDetector.detect(file)).thenReturn(document);

        ParsedStatement statement = parser.parse(file);

        assertThat(statement.transactions()).hasSize(2);
        assertThat(statement.transactions().get(0).amount()).isEqualByComparingTo(new BigDecimal("-1200.00"));
        assertThat(statement.transactions().get(1).amount()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void shouldResolveZeroDebitOrCreditAndIgnoreAmbiguousDebitCreditRows() {
        UploadedCsvFile file = uploadedFile();
        CsvDocument document = CsvDocument.builder()
                .delimiter(';')
                .headers(List.of("data", "descricao", "debito", "credito"))
                .rows(List.of(
                        row("data", "06/05/2026", "descricao", "Refund", "debito", "0,00", "credito", "25,00"),
                        row("data", "07/05/2026", "descricao", "Rent", "debito", "25,00", "credito", "0,00"),
                        row("data", "08/05/2026", "descricao", "Ambiguous", "debito", "10,00", "credito", "25,00")
                ))
                .build();
        when(structureDetector.detect(file)).thenReturn(document);

        ParsedStatement statement = parser.parse(file);

        assertThat(statement.transactions()).hasSize(2);
        assertThat(statement.transactions().get(0).description()).isEqualTo("Refund");
        assertThat(statement.transactions().get(0).amount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(statement.transactions().get(1).description()).isEqualTo("Rent");
        assertThat(statement.transactions().get(1).amount()).isEqualByComparingTo(new BigDecimal("-25.00"));
    }

    private static UploadedCsvFile uploadedFile() {
        return UploadedCsvFile.builder()
                .fileName("statement.csv")
                .content(new byte[] {1})
                .build();
    }

    private static Map<String, String> row(
            String key1, String value1,
            String key2, String value2,
            String key3, String value3,
            String key4, String value4
    ) {
        return Map.of(
                key1, value1,
                key2, value2,
                key3, value3,
                key4, value4
        );
    }
}
