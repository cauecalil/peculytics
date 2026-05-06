package com.peculytics.categorizationservice.messaging;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DeadLetterFailureReasonExtractorTest {
    private final DeadLetterFailureReasonExtractor extractor = new DeadLetterFailureReasonExtractor();

    @Test
    void shouldReturnUnavailableWhenHeadersAreNullOrEmpty() {
        assertThat(extractor.extract(null)).isEqualTo("unavailable");
        assertThat(extractor.extract(Map.of())).isEqualTo("unavailable");
    }

    @Test
    void shouldExtractExceptionMessage() {
        String reason = extractor.extract(Map.of("x-exception-message", "validation failed"));

        assertThat(reason).isEqualTo("exception=validation failed");
    }

    @Test
    void shouldExtractDetailsFromXDeathListWithMap() {
        String reason = extractor.extract(Map.of(
                "x-death",
                List.of(Map.of(
                        "reason", "rejected",
                        "queue", "categorization.queue",
                        "count", 2L
                ))
        ));

        assertThat(reason).isEqualTo("x-death(reason=rejected, queue=categorization.queue, count=2)");
    }

    @Test
    void shouldHandleUnexpectedXDeathFormatWithoutBreaking() {
        assertThatCode(() -> extractor.extract(Map.of("x-death", List.of("unexpected"))))
                .doesNotThrowAnyException();
        assertThat(extractor.extract(Map.of("x-death", List.of("unexpected"))))
                .isEqualTo("x-death(unexpected)");
    }
}
