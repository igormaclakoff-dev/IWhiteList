package com.auracode.iwhitelist.util;

import java.util.Locale;

/**
 * Parses human-readable durations such as {@code 30m}, {@code 12h}, {@code 7d} or {@code 2d6h}.
 * A return value of {@code 0} means "permanent"; {@code null} means the input is invalid.
 */
public final class DurationUtil {

    private DurationUtil() {
    }

    /**
     * @return duration in milliseconds, {@code 0} for a permanent entry, or {@code null} if invalid.
     */
    public static Long parse(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        if (value.equals("perm") || value.equals("permanent") || value.equals("forever")
                || value.equals("беск") || value.equals("навсегда")) {
            return 0L;
        }

        long total = 0L;
        long number = 0L;
        boolean hasNumber = false;
        boolean hasUnit = false;

        for (int i = 0; i < value.length(); i++) {
            char symbol = value.charAt(i);
            if (Character.isDigit(symbol)) {
                number = number * 10L + (symbol - '0');
                hasNumber = true;
                continue;
            }
            long unit = unitMillis(symbol);
            if (unit <= 0L || !hasNumber) {
                return null;
            }
            total += unit * number;
            number = 0L;
            hasNumber = false;
            hasUnit = true;
        }

        if (hasNumber || !hasUnit) {
            return null;
        }
        return total;
    }

    private static long unitMillis(char symbol) {
        return switch (symbol) {
            case 's', 'с' -> 1_000L;
            case 'm', 'м' -> 60_000L;
            case 'h', 'ч' -> 3_600_000L;
            case 'd', 'д' -> 86_400_000L;
            case 'w', 'н' -> 604_800_000L;
            default -> 0L;
        };
    }
}
