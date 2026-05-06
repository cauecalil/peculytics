package com.peculytics.apiservice.exception;

import java.util.UUID;

public class AnalysisNotFoundException extends RuntimeException {
    public AnalysisNotFoundException(UUID analysisId) {
        super("Analysis not found: " + analysisId);
    }
}
