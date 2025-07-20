package com.dici.time;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TimeUtils {
    /**
     * Formats a {@link Duration} into a human-readable string like "2 days, 1 hour and 5 milliseconds".
     *
     * <p>This method will display up to years, months, days, hours, minutes, seconds, and milliseconds,
     * skipping any units with a value of 0. It also correctly handles pluralization.
     *
     * @param duration The duration to format.
     * @return A human-readable string representation of the duration.
     */
    public static String humanReadableDuration(Duration duration) {
        if (duration == null || duration.isZero()) {
            return "0 milliseconds";
        }

        long days = duration.toDays();
        long years = days / 365;
        days %= 365;
        long months = days / 30; // Approximation
        days %= 30;

        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.toSeconds() % 60;
        long milliseconds = duration.toMillis() % 1000;

        List<String> parts = new ArrayList<>();

        addPart(parts, years, "year");
        addPart(parts, months, "month");
        addPart(parts, days, "day");
        addPart(parts, hours, "hour");
        addPart(parts, minutes, "minute");
        addPart(parts, seconds, "second");
        addPart(parts, milliseconds, "millisecond");

        return formatParts(parts);
    }

    private static void addPart(List<String> parts, long value, String unit) {
        if (value > 0) {
            parts.add(value + " " + unit + (value > 1 ? "s" : ""));
        }
    }

    private static String formatParts(List<String> parts) {
        if (parts.isEmpty()) {
            return "0 milliseconds";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i));
            if (i < parts.size() - 2) {
                sb.append(", ");
            } else if (i == parts.size() - 2) {
                sb.append(" and ");
            }
        }
        return sb.toString();
    }
}
