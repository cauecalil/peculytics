package com.peculytics.uploadservice.csv;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyParserTest {
    private final MoneyParser parser = new MoneyParser();

    @Test
    void shouldParseBrazilianCurrencyFormat() {
        var result = parser.parse("R$ 1.234,56");

        assertThat(result).contains(new BigDecimal("1234.56"));
    }

    @Test
    void shouldParseUsCurrencyFormat() {
        var result = parser.parse("$1,234.56");

        assertThat(result).contains(new BigDecimal("1234.56"));
    }

    @Test
    void shouldParseParenthesizedValueAsNegative() {
        var result = parser.parse("(1.234,56)");

        assertThat(result).contains(new BigDecimal("-1234.56"));
    }

    @Test
    void shouldTreatSingleSeparatorWithThousandsGroupsAsThousandsSeparator() {
        var result = parser.parse("1,234");

        assertThat(result).contains(new BigDecimal("1234"));
    }

    @Test
    void shouldReturnEmptyForInvalidMoneyValue() {
        var result = parser.parse("not money");

        assertThat(result).isEmpty();
    }
}
