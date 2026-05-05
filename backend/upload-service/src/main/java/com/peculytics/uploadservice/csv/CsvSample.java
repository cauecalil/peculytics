package com.peculytics.uploadservice.csv;

import lombok.Builder;

import java.util.List;

@Builder
public record CsvSample(
        List<String> headers,
        List<List<String>> rows,
        char delimiter
) {}
