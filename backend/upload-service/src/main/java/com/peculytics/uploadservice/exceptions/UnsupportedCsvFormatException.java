package com.peculytics.uploadservice.exceptions;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UnsupportedCsvFormatException extends RuntimeException {
    private final UUID analysisId;
    private final HttpStatus status;
    private final String errorCode;
    private final String title;

    public UnsupportedCsvFormatException(String message) {
        this(message, null);
    }

    public UnsupportedCsvFormatException(String message, UUID analysisId) {
        this(message, analysisId, HttpStatus.UNPROCESSABLE_CONTENT, "unsupported-csv-format", "Unsupported CSV Format");
    }

    public UnsupportedCsvFormatException(
            String message,
            UUID analysisId,
            HttpStatus status,
            String errorCode,
            String title
    ) {
        super(message);
        this.analysisId = analysisId;
        this.status = status;
        this.errorCode = errorCode;
        this.title = title;
    }

    public static UnsupportedCsvFormatException fileTooLarge(String message, UUID analysisId) {
        return new UnsupportedCsvFormatException(
                message,
                analysisId,
                HttpStatus.CONTENT_TOO_LARGE,
                "file-too-large",
                "File Too Large"
        );
    }

    public static UnsupportedCsvFormatException unsupportedFileType(String message, UUID analysisId) {
        return new UnsupportedCsvFormatException(
                message,
                analysisId,
                HttpStatus.UNPROCESSABLE_CONTENT,
                "unsupported-file-type",
                "Unsupported File Type"
        );
    }

    public static UnsupportedCsvFormatException emptyCsvFile(String message, UUID analysisId) {
        return new UnsupportedCsvFormatException(
                message,
                analysisId,
                HttpStatus.UNPROCESSABLE_CONTENT,
                "empty-csv-file",
                "Empty CSV File"
        );
    }

    public UUID analysisId() {
        return analysisId;
    }

    public HttpStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }

    public String title() {
        return title;
    }
}
