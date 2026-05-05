package com.peculytics.uploadservice.csv;

import lombok.Builder;

@Builder
public record UploadedCsvFile(
        String originalFilename,
        byte[] content
) {}
