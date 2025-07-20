package com.dici.insights.timeline;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * {@link Timeline} implementation which gives no behaviour guarantee in a multi-threaded environment.
 */
public class SingleThreadedTimeline<EVENT extends Enum<EVENT>> implements Timeline<EVENT> {
    /**
     * Creates a new timeline with a given clock and immediately starts measuring total execution time.
     */
    public static <EVENT extends Enum<EVENT>> Timeline<EVENT> createStarted(Clock clock) {
        return new SingleThreadedTimeline<>(clock, clock.instant());
    }

    private final Clock clock;
    private final Instant timelineStart;

    private final List<EventExecution<EVENT>> topLevelEvents = new ArrayList<>();
    private final Deque<List<EventExecution<EVENT>>> eventsByDepth = new ArrayDeque<>();

    public SingleThreadedTimeline(Clock clock, Instant timelineStart) {
        this.clock = Objects.requireNonNull(clock);
        this.timelineStart = Objects.requireNonNull(timelineStart);
    }

    @Override
    public <T> T recordEvent(EVENT event, Supplier<T> supplier) {
        Instant start = clock.instant();
        boolean isSuccess = false;
        try {
            eventsByDepth.push(new ArrayList<>());
            T t = supplier.get();
            isSuccess = true;
            return t;
        } finally {
            List<EventExecution<EVENT>> children = List.copyOf(eventsByDepth.pop());
            recordEventExecution(start, clock.instant(), event, isSuccess, children);
        }
    }

    private void recordEventExecution(Instant start, Instant end, EVENT event, boolean isSuccess, List<EventExecution<EVENT>> children) {
        Duration executionTime = Duration.between(start, end);
        EventExecution<EVENT> eventExecution = new EventExecution<>(event, start, executionTime, children, isSuccess);
        Optional.ofNullable(eventsByDepth.peek()).orElse(topLevelEvents).add(eventExecution);
    }

    @Override
    public TimelineSnapshot<EVENT> getSnapshot(boolean success) {
        Duration totalExecutionTime = Duration.between(timelineStart, clock.instant());
        return new TimelineSnapshot<>(timelineStart, totalExecutionTime, List.copyOf(topLevelEvents), success);
    }
}
