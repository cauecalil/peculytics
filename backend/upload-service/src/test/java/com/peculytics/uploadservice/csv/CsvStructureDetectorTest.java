package com.peculytics.uploadservice.csv;

import com.peculytics.uploadservice.exceptions.UnsupportedCsvFormatException;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvStructureDetectorTest {
    private final CsvStructureDetector detector = new CsvStructureDetector();

    @Test
    void shouldDetectSemicolonDelimitedCsvAndNormalizeHeaders() {
        UploadedCsvFile file = csvFile("statement.csv",
                "Data;Descricao;Valor\n06/05/2026;Padaria;R$ 12,50\n".getBytes(StandardCharsets.UTF_8));

        CsvDocument document = detector.detect(file);

        assertThat(document.delimiter()).isEqualTo(';');
        assertThat(document.headers()).containsExactly("data", "descricao", "valor");
        assertThat(document.rows()).hasSize(1);
        assertThat(document.rows().getFirst())
                .containsEntry("data", "06/05/2026")
                .containsEntry("descricao", "Padaria")
                .containsEntry("valor", "R$ 12,50");
    }

    @Test
    void shouldDecodeWindows1252WhenUtf8DecodingFails() {
        UploadedCsvFile file = csvFile("statement.csv",
                "Data;Descrição;Valor\n06/05/2026;Café;10,00\n".getBytes(Charset.forName("windows-1252")));

        CsvDocument document = detector.detect(file);

        assertThat(document.headers()).containsExactly("data", "descricao", "valor");
        assertThat(document.rows().getFirst()).containsEntry("descricao", "Café");
    }

    @Test
    void shouldRejectCsvWhenStructureCannotBeDetected() {
        UploadedCsvFile file = csvFile("statement.csv", "onlyonecolumn\nvalue\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> detector.detect(file))
                .isInstanceOf(UnsupportedCsvFormatException.class)
                .hasMessageContaining("CSV structure could not be detected");
    }

    private static UploadedCsvFile csvFile(String fileName, byte[] content) {
        return UploadedCsvFile.builder()
                .fileName(fileName)
                .content(content)
                .build();
    }
}
