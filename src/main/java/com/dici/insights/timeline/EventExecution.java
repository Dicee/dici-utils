package com.dici.insights.timeline;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Tree structure storing information about a given timeline event and its nested children.
 */
public record EventExecution<EVENT extends Enum<EVENT>>(
        EVENT event,
        Instant start,
        Duration executionTime,
        List<EventExecution<EVENT>> children,
        boolean isSuccess
) {
    public Instant end() {
        return start.plus(executionTime);
    }
}
