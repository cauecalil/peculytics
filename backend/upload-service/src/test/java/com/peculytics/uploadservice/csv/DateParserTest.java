package com.peculytics.uploadservice.csv;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DateParserTest {
    private final DateParser parser = new DateParser();

    @Test
    void shouldParseIsoLocalDate() {
        var result = parser.parse("2026-05-06");

        assertThat(result).contains(LocalDate.of(2026, 5, 6));
    }

    @Test
    void shouldParseBrazilianSlashDate() {
        var result = parser.parse("06/05/2026");

        assertThat(result).contains(LocalDate.of(2026, 5, 6));
    }

    @Test
    void shouldParseBrazilianDashDate() {
        var result = parser.parse("06-05-2026");

        assertThat(result).contains(LocalDate.of(2026, 5, 6));
    }

    @Test
    void shouldRejectInvalidCalendarDate() {
        var result = parser.parse("31/02/2026");

        assertThat(result).isEmpty();
    }
}
