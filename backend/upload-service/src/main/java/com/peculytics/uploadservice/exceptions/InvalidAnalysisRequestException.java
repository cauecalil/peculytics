package com.peculytics.uploadservice.exceptions;

public class InvalidAnalysisRequestException extends RuntimeException {
    public InvalidAnalysisRequestException(String message) {
        super(message);
    }
}
