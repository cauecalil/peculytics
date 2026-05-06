package com.peculytics.categorizationservice.messaging;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class DeadLetterFailureReasonExtractor {
    public String extract(Map<String, Object> headers) {
        if (headers == null || headers.isEmpty()) {
            return "unavailable";
        }

        List<String> reasons = new ArrayList<>();
        exceptionMessage(headers).ifPresent(reason -> reasons.add("exception=" + reason));
        xDeath(headers.get("x-death")).ifPresent(reasons::add);

        return reasons.isEmpty() ? "unavailable" : String.join("; ", reasons);
    }

    private static Optional<String> exceptionMessage(Map<String, Object> headers) {
        return Optional.ofNullable(headers.get("x-exception-message"))
                .map(Objects::toString)
                .filter(message -> !message.isBlank());
    }

    private static Optional<String> xDeath(Object value) {
        if (value instanceof List<?> deaths && !deaths.isEmpty()) {
            Object firstDeath = deaths.getFirst();

            if (firstDeath instanceof Map<?, ?> death) {
                return Optional.of("x-death(" + xDeathDetails(death) + ")");
            }

            return Optional.of("x-death(" + firstDeath + ")");
        }

        return Optional.ofNullable(value)
                .map(Objects::toString)
                .filter(death -> !death.isBlank())
                .map(death -> "x-death(" + death + ")");
    }

    private static String xDeathDetails(Map<?, ?> death) {
        List<String> details = new ArrayList<>();

        addDetail(details, death, "reason");
        addDetail(details, death, "queue");
        addDetail(details, death, "count");

        if (details.isEmpty()) {
            return death.toString();
        }

        return String.join(", ", details);
    }

    private static void addDetail(List<String> details, Map<?, ?> death, String key) {
        Object value = death.get(key);

        if (value != null) {
            details.add(key + "=" + value);
        }
    }
}
