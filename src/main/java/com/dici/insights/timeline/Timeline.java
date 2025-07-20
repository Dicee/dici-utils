package com.dici.insights.timeline;

import java.util.function.Supplier;

/**
 * This interface allows recording a series of executed events in order while collecting some stats about
 * execution time etc. Timeline reporters can be provided to execute reporting logic on the collected data.
 */
public interface Timeline<EVENT extends Enum<EVENT>> {
    /**
     * Executes the given runnable associated to an event type, and records this execution.
     */
    default void recordEvent(EVENT event, Runnable runnable) {
        recordEvent(event, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Executes the given supplier associated to an event type, records this execution and returns the
     * output of the supplier.
     */
    <T> T recordEvent(EVENT event, Supplier<T> supplier);

    /**
     * Takes an immutable snapshot of the current state of the timeline, allowing to report the events
     * executed so far.
     */
    TimelineSnapshot<EVENT> getSnapshot(boolean success);
}
