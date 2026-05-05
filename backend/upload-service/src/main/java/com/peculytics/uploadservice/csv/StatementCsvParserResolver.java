package com.peculytics.uploadservice.csv;

import com.peculytics.uploadservice.exceptions.UnsupportedCsvFormatException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StatementCsvParserResolver {
    private final List<StatementCsvParser> parsers;
    private final CsvStructureDetector structureDetector;

    public StatementCsvParser resolve(CsvSample sample) {
        return parsers.stream()
                .filter(parser -> parser.supports(sample))
                .findFirst()
                .orElseThrow(() -> new UnsupportedCsvFormatException("The uploaded CSV format is not supported."));
    }

    public StatementCsvParser resolve(UploadedCsvFile file) {
        CsvDocument document = structureDetector.detect(file);

        return resolve(CsvSample.builder()
                .headers(document.headers())
                .rows(document.rows().stream()
                        .limit(5)
                        .map(row -> document.headers().stream()
                                .map(header -> row.getOrDefault(header, ""))
                                .toList())
                        .toList())
                .delimiter(document.delimiter())
                .build());
    }
}
