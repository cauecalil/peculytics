package com.peculytics.uploadservice.csv;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record CsvDocument(
        char delimiter,
        List<String> headers,
        List<Map<String, String>> rows
) {}
