package com.peculytics.apiservice.exception;

public class AnalysisDeletionNotAllowedException extends RuntimeException {
    public AnalysisDeletionNotAllowedException() {
        super("Analysis cannot be deleted while processing.");
    }
}
