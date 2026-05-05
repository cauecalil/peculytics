package com.peculytics.uploadservice.csv;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class MoneyParser {
    public Optional<BigDecimal> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String cleaned = value.trim()
                .replaceAll("[\\s\\u00A0]", "")
                .replaceAll("[^0-9,().+-]", "");

        if (cleaned.isBlank()) {
            return Optional.empty();
        }

        boolean negative = cleaned.startsWith("-") || cleaned.startsWith("(") && cleaned.endsWith(")");
        cleaned = cleaned.replace("(", "")
                .replace(")", "")
                .replace("+", "")
                .replace("-", "");

        int lastComma = cleaned.lastIndexOf(',');
        int lastDot = cleaned.lastIndexOf('.');

        String normalized;

        if (lastComma >= 0 && lastDot < 0 && containsOnlyThousandsGroups(cleaned, ',')) {
            normalized = cleaned.replace(",", "");
        } else if (lastDot >= 0 && lastComma < 0 && containsOnlyThousandsGroups(cleaned, '.')) {
            normalized = cleaned.replace(".", "");
        } else {
            char decimalSeparator = resolveDecimalSeparator(lastComma, lastDot);
            normalized = switch (decimalSeparator) {
                case ',' -> cleaned.replace(".", "").replace(',', '.');
                case '.' -> cleaned.replace(",", "");
                default -> cleaned;
            };
        }

        if (normalized.isBlank()) {
            return Optional.empty();
        }

        try {
            BigDecimal amount = new BigDecimal(normalized);
            return Optional.of(negative ? amount.negate() : amount);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static char resolveDecimalSeparator(int lastComma, int lastDot) {
        if (lastComma >= 0 && lastDot >= 0) {
            return lastComma > lastDot ? ',' : '.';
        }

        if (lastComma >= 0) {
            return ',';
        }

        if (lastDot >= 0) {
            return '.';
        }

        return 0;
    }

    private static boolean containsOnlyThousandsGroups(String value, char separator) {
        String[] groups = value.split(Pattern.quote(String.valueOf(separator)));

        if (groups.length < 2 || groups[0].isBlank() || groups[0].length() > 3) {
            return false;
        }

        for (int index = 1; index < groups.length; index++) {
            if (groups[index].length() != 3) {
                return false;
            }
        }

        return true;
    }
}
