package io.github.duckycashy.minepass.util;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern PATTERN =
            Pattern.compile("^(\\d+)\\s*(s|m|h|d|w)$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static Duration parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Duration cannot be null.");
        }

        Matcher matcher = PATTERN.matcher(input.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid duration. Use values such as 30m, 1h, 12h or 7d."
            );
        }

        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();

        return switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            case "w" -> Duration.ofDays(Math.multiplyExact(amount, 7));
            default -> throw new IllegalArgumentException(
                    "Unsupported duration unit."
            );
        };
    }
}