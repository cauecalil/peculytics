package com.peculytics.uploadservice.csv;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class DateParser {
    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            strictFormatter("dd/MM/uuuu"),
            strictFormatter("dd-MM-uuuu")
    );

    public Optional<LocalDate> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String trimmed = value.trim();

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(trimmed, formatter));
            } catch (DateTimeParseException ignored) {
                // Try the next supported date format.
            }
        }

        return Optional.empty();
    }

    private static DateTimeFormatter strictFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .appendPattern(pattern)
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT);
    }
}
