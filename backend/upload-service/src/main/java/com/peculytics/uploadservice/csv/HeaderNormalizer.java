package com.peculytics.uploadservice.csv;

import java.text.Normalizer;
import java.util.Locale;

public class HeaderNormalizer {
    private HeaderNormalizer() {
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
