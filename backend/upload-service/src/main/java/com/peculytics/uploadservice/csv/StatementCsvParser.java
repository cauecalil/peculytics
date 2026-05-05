package com.peculytics.uploadservice.csv;

public interface StatementCsvParser {
    boolean supports(CsvSample sample);
    ParsedStatement parse(UploadedCsvFile file);
    String parserName();
}
