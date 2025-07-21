package com.dici.testing.logging;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/// Simplified [LogEvent] that can easily be compared in tests
@Value
public class SimpleLogEvent {
    public static SimpleLogEvent forClass(Class<?> clazz, Level level, String msg) {
        return forClass(clazz, level, msg, Map.of());
    }

    public static SimpleLogEvent forClass(Class<?> clazz, Level level, String msg, Map<String, String> context) {
        return new SimpleLogEvent(normalizeLoggerName(clazz.getName()), level, msg, context, null);
    }

    public static SimpleLogEvent from(LogEvent event) {
        return new SimpleLogEvent(normalizeLoggerName(event.getLoggerName()), event.getLevel(),
                event.getMessage().getFormattedMessage(), event.getContextData().toMap(), event.getThrown());
    }

    @NonNull private final String loggerName;
    @NonNull private final Level level;
    @NonNull private final String formattedMessage;
    @NonNull private final Map<String, String> contextData;

    @EqualsAndHashCode.Exclude // not defined on most throwables
    @Nullable private final Throwable thrown;

    // handle inconsistencies between different versions of log4j relating to nested classes
    private static String normalizeLoggerName(String loggerName) {
        return loggerName.replace("$", ".");
    }
}
