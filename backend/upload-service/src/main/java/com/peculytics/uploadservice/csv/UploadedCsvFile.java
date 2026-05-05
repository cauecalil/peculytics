package com.peculytics.uploadservice.csv;

import lombok.Builder;

@Builder
public record UploadedCsvFile(
        String fileName,
        byte[] content
) {}
