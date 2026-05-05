package com.peculytics.uploadservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String URN_PREFIX = "urn:peculytics:uploadservice:error:";

    @ExceptionHandler(InvalidAnalysisRequestException.class)
    public ProblemDetail handleInvalidAnalysisRequest(InvalidAnalysisRequestException e) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-analysis-request",
                "Invalid Analysis Request",
                e.getMessage()
        );
    }

    @ExceptionHandler(UnsupportedCsvFormatException.class)
    public ProblemDetail handleUnsupportedCsvFormat(UnsupportedCsvFormatException e) {
        ProblemDetail p = problem(e.status(), e.errorCode(), e.title(), e.getMessage());

        if (e.analysisId() != null) {
            p.setProperty("analysisId", e.analysisId().toString());
        }

        return p;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return validationProblem(firstFieldErrorMessage(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceeded() {
        return problem(
                HttpStatus.CONTENT_TOO_LARGE,
                "file-too-large",
                "File Too Large",
                "Each uploaded file must respect the configured size limits."
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipart() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-multipart-request",
                "Invalid Multipart Request",
                "The upload request must be valid multipart form data."
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-analysis-request",
                "Invalid Analysis Request",
                e.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception e) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-server-error",
                "Internal Server Error",
                "An unexpected error occurred while processing the request."
        );
    }

    private static ProblemDetail validationProblem(String detail) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "validation-failed",
                "Validation Failed",
                detail
        );
    }

    private static String firstFieldErrorMessage(FieldError fieldError) {
        if (fieldError == null || fieldError.getDefaultMessage() == null || fieldError.getDefaultMessage().isBlank()) {
            return "The request contains invalid fields.";
        }

        return fieldError.getDefaultMessage();
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);

        p.setType(URI.create(URN_PREFIX + type));
        p.setTitle(title);

        return p;
    }
}
