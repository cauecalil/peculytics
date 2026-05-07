package com.peculytics.apiservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnNotFoundProblemForMissingAnalysis() {
        UUID analysisId = UUID.randomUUID();

        ProblemDetail problem = handler.handleAnalysisNotFound(new AnalysisNotFoundException(analysisId));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getType()).isEqualTo(URI.create("urn:peculytics:apiservice:error:analysis-not-found"));
        assertThat(problem.getTitle()).isEqualTo("Analysis Not Found");
        assertThat(problem.getDetail()).contains(analysisId.toString());
    }

    @Test
    void shouldReturnConflictProblemWhenDeletionIsNotAllowed() {
        ProblemDetail problem = handler.handleAnalysisDeletionNotAllowed(new AnalysisDeletionNotAllowedException());

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getType()).isEqualTo(URI.create("urn:peculytics:apiservice:error:analysis-deletion-not-allowed"));
        assertThat(problem.getTitle()).isEqualTo("Analysis Deletion Not Allowed");
        assertThat(problem.getDetail()).isEqualTo("Analysis cannot be deleted while processing.");
    }

    @Test
    void shouldReturnBadRequestProblemForInvalidPaginationParameter() {
        ProblemDetail problem = handler.handleInvalidPaginationParameter(
                new InvalidPaginationParameterException("Size must be greater than zero.")
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getType()).isEqualTo(URI.create("urn:peculytics:apiservice:error:invalid-pagination-parameter"));
        assertThat(problem.getTitle()).isEqualTo("Invalid Pagination Parameter");
        assertThat(problem.getDetail()).isEqualTo("Size must be greater than zero.");
    }

    @Test
    void shouldReturnInternalServerErrorProblemForUnexpectedExceptions() {
        ProblemDetail problem = handler.handleGeneric(new RuntimeException("database details"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getType()).isEqualTo(URI.create("urn:peculytics:apiservice:error:internal-server-error"));
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred while processing the request.");
    }
}
