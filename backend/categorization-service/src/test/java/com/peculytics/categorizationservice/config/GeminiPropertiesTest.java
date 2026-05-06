package com.peculytics.categorizationservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiPropertiesTest {
    @Test
    void shouldUseDefaultModelWhenModelIsNull() {
        GeminiProperties properties = new GeminiProperties("api-key", null);

        assertThat(properties.model()).isEqualTo("gemini-flash-lite-latest");
    }

    @Test
    void shouldUseDefaultModelWhenModelIsBlank() {
        GeminiProperties properties = new GeminiProperties("api-key", "   ");

        assertThat(properties.model()).isEqualTo("gemini-flash-lite-latest");
    }

    @Test
    void shouldPreserveConfiguredModelWhenValid() {
        GeminiProperties properties = new GeminiProperties("api-key", "gemini-custom");

        assertThat(properties.model()).isEqualTo("gemini-custom");
    }
}
