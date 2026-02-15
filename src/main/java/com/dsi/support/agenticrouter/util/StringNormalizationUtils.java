package com.dsi.support.agenticrouter.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public final class StringNormalizationUtils {

    private StringNormalizationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String trimToNull(
        String value
    ) {
        return StringUtils.trimToNull(value);
    }

    public static String trimToEmpty(
        String value
    ) {
        return StringUtils.trimToEmpty(value);
    }

    public static String lowerTrimmedOrNull(
        String value
    ) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return null;
        }

        return normalizedValue.toLowerCase(Locale.ROOT);
    }

    public static String upperTrimmedOrNull(
        String value
    ) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return null;
        }

        return normalizedValue.toUpperCase(Locale.ROOT);
    }

    public static String lowerTrimmedOrEmpty(
        String value
    ) {
        return trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    public static String upperTrimmedOrEmpty(
        String value
    ) {
        return trimToEmpty(value).toUpperCase(Locale.ROOT);
    }
}
