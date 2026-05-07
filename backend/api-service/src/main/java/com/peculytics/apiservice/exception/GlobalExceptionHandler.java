package com.peculytics.apiservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String URN_PREFIX = "urn:peculytics:apiservice:error:";

    @ExceptionHandler(AnalysisNotFoundException.class)
    ProblemDetail handleAnalysisNotFound(AnalysisNotFoundException e) {
        return problem(
                HttpStatus.NOT_FOUND,
                "analysis-not-found",
                "Analysis Not Found",
                e.getMessage()
        );
    }

    @ExceptionHandler(AnalysisDeletionNotAllowedException.class)
    ProblemDetail handleAnalysisDeletionNotAllowed(AnalysisDeletionNotAllowedException e) {
        return problem(
                HttpStatus.CONFLICT,
                "analysis-deletion-not-allowed",
                "Analysis Deletion Not Allowed",
                e.getMessage()
        );
    }

    @ExceptionHandler(InvalidPaginationParameterException.class)
    ProblemDetail handleInvalidPaginationParameter(InvalidPaginationParameterException e) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-pagination-parameter",
                "Invalid Pagination Parameter",
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

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);

        p.setType(URI.create(URN_PREFIX + type));
        p.setTitle(title);

        return p;
    }
}
