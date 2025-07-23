package com.dici.testing.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.List;

import static com.dici.collection.CollectionUtils.map;
import static java.util.Collections.unmodifiableList;

/// An appender which stores all the events appended to it so that they can be compared to expectations in tests.
public class RecordingAppender extends AbstractAppender {
    public static final Logger ROOT_LOGGER = getRootLogger();

    public static RecordingAppender attachedToLogger(Logger logger) {
        var appender = new RecordingAppender();
        logger.addAppender(appender);
        appender.start();
        return appender;
    }

    private final List<SimpleLogEvent> events = new ArrayList<>();

    public RecordingAppender() {
        super("RecordingAppender", null, null, false, new Property[0]);
    }

    @Override
    public void append(LogEvent logEvent) {
        events.add(SimpleLogEvent.from(logEvent));
    }

    public void detachFromRootLogger(Logger logger) {
        logger.removeAppender(this);
    }

    public List<SimpleLogEvent> getEvents() {
        return unmodifiableList(events);
    }

    public List<String> getMessages() {
        return map(events, RecordingAppender::formatLogMessage);
    }

    private static String formatLogMessage(SimpleLogEvent event) {
        StringBuilder sb = new StringBuilder(event.getFormattedMessage());
        if (event.getThrown() != null) sb.append('\n').append(event.getThrown());
        return sb.toString();
    }

    public void clearEvents() {
        events.clear();
    }

    private static Logger getRootLogger() {
        return (Logger) LogManager.getRootLogger();
    }
}
