package com.peculytics.uploadservice.csv;

import com.peculytics.uploadservice.exceptions.UnsupportedCsvFormatException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvStructureDetector {
    private static final char[] CANDIDATE_DELIMITERS = { ',', ';', '\t' };
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    public CsvDocument detect(UploadedCsvFile file) {
        String content = decode(file.content());
        CsvDocument bestDocument = null;
        int bestScore = -1;

        for (char delimiter : CANDIDATE_DELIMITERS) {
            try {
                CsvDocument document = parse(content, delimiter);
                int score = score(document);
                if (score > bestScore) {
                    bestScore = score;
                    bestDocument = document;
                }
            } catch (IOException | IllegalArgumentException ignored) {
                // Try the next delimiter candidate.
            }
        }

        if (bestDocument == null || bestDocument.headers().size() < 2) {
            throw new UnsupportedCsvFormatException("CSV structure could not be detected for file: " + file.originalFilename());
        }

        return bestDocument;
    }

    private static CsvDocument parse(String content, char delimiter) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .get();

        try (CSVParser parser = format.parse(new StringReader(content))) {
            List<String> originalHeaders = new ArrayList<>(parser.getHeaderMap().keySet());
            List<String> normalizedHeaders = originalHeaders.stream()
                    .map(HeaderNormalizer::normalize)
                    .toList();
            List<Map<String, String>> rows = new ArrayList<>();

            for (CSVRecord record : parser) {
                Map<String, String> row = new HashMap<>();
                for (String originalHeader : originalHeaders) {
                    row.put(HeaderNormalizer.normalize(originalHeader), record.get(originalHeader));
                }
                rows.add(row);
            }

            return CsvDocument.builder()
                    .delimiter(delimiter)
                    .headers(normalizedHeaders)
                    .rows(rows)
                    .build();
        }
    }

    private static int score(CsvDocument document) {
        int rowScore = document.rows().stream()
                .mapToInt(row -> (int) row.values().stream().filter(value -> value != null && !value.isBlank()).count())
                .sum();

        return document.headers().size() * 10 + rowScore;
    }

    private static String decode(byte[] content) {
        if (content.length >= 3
                && (content[0] & 0xFF) == 0xEF
                && (content[1] & 0xFF) == 0xBB
                && (content[2] & 0xFF) == 0xBF) {
            return new String(content, 3, content.length - 3, StandardCharsets.UTF_8);
        }

        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(content, WINDOWS_1252);
        }
    }
}
