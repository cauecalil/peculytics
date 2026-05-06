package com.peculytics.uploadservice.csv;

import com.peculytics.uploadservice.exceptions.UnsupportedCsvFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementCsvParserResolverTest {
    @Mock
    private StatementCsvParser firstParser;

    @Mock
    private StatementCsvParser secondParser;

    @Mock
    private CsvStructureDetector structureDetector;

    @Test
    void shouldResolveFirstParserThatSupportsSample() {
        CsvSample sample = CsvSample.builder()
                .headers(List.of("date", "description", "amount"))
                .rows(List.of())
                .delimiter(',')
                .build();
        when(firstParser.supports(sample)).thenReturn(false);
        when(secondParser.supports(sample)).thenReturn(true);
        StatementCsvParserResolver resolver = new StatementCsvParserResolver(List.of(firstParser, secondParser), structureDetector);

        StatementCsvParser resolved = resolver.resolve(sample);

        assertThat(resolved).isSameAs(secondParser);
    }

    @Test
    void shouldRejectSampleWhenNoParserSupportsIt() {
        CsvSample sample = CsvSample.builder()
                .headers(List.of("unknown"))
                .rows(List.of())
                .delimiter(',')
                .build();
        when(firstParser.supports(sample)).thenReturn(false);
        StatementCsvParserResolver resolver = new StatementCsvParserResolver(List.of(firstParser), structureDetector);

        assertThatThrownBy(() -> resolver.resolve(sample))
                .isInstanceOf(UnsupportedCsvFormatException.class)
                .hasMessage("The uploaded CSV format is not supported.");
    }

    @Test
    void shouldBuildSampleFromDetectedCsvFileUsingOnlyFirstFiveRows() {
        UploadedCsvFile file = UploadedCsvFile.builder()
                .fileName("statement.csv")
                .content(new byte[] {1})
                .build();
        CsvDocument document = CsvDocument.builder()
                .delimiter(';')
                .headers(List.of("date", "description", "amount"))
                .rows(List.of(
                        row("01"), row("02"), row("03"), row("04"), row("05"), row("06")
                ))
                .build();
        when(structureDetector.detect(file)).thenReturn(document);
        when(firstParser.supports(any(CsvSample.class))).thenReturn(true);
        StatementCsvParserResolver resolver = new StatementCsvParserResolver(List.of(firstParser), structureDetector);

        StatementCsvParser resolved = resolver.resolve(file);

        ArgumentCaptor<CsvSample> sampleCaptor = ArgumentCaptor.forClass(CsvSample.class);
        org.mockito.Mockito.verify(firstParser).supports(sampleCaptor.capture());
        assertThat(resolved).isSameAs(firstParser);
        assertThat(sampleCaptor.getValue().delimiter()).isEqualTo(';');
        assertThat(sampleCaptor.getValue().headers()).containsExactly("date", "description", "amount");
        assertThat(sampleCaptor.getValue().rows()).hasSize(5);
        assertThat(sampleCaptor.getValue().rows().getFirst()).containsExactly("01", "Description 01", "10.00");
    }

    private static Map<String, String> row(String suffix) {
        return Map.of(
                "date", suffix,
                "description", "Description " + suffix,
                "amount", "10.00"
        );
    }
}
