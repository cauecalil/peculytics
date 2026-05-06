package com.peculytics.categorizationservice.config;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Builder
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model
) {
    private static final String DEFAULT_MODEL = "gemini-flash-lite-latest";

    public GeminiProperties {
        model = StringUtils.hasText(model) ? model : DEFAULT_MODEL;
    }
}
