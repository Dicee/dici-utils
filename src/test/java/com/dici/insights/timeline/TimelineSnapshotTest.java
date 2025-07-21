package com.dici.insights.timeline;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.dici.insights.timeline.SingleThreadedTimelineTest.Events.*;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

class TimelineSnapshotTest {
    @Test
    void testToPrettyString() {
        var snapshot = getEventsTimelineSnapshot();

        String expected = """
            Dummy request @ 1970-01-01T00:00:10Z took 888 milliseconds
            +0ms......Timeline (total: 888 milliseconds) {
            +0ms........A (total: 500 milliseconds) {
            +100ms........B (total: 200 milliseconds) {
            +200ms..........C (total: 50 milliseconds) {
            +250ms..........}
            +300ms........}
            +500ms......}
            +500ms......A (total: 200 milliseconds) {
            +700ms......}
            +888ms....}""";
        assertThat(snapshot.toPrettyString("Dummy request")).isEqualTo(expected);
    }

    @Test
    void testEventExecutionsInOrder() {
        Instant start = Instant.ofEpochMilli(10_000);
        Duration totalExecutionTime = ofMillis(888);

        var c1 = new EventExecution<>(C, Instant.ofEpochMilli(10_200), Duration.ofMillis(50), List.of(), true);
        var b1 = new EventExecution<>(B, Instant.ofEpochMilli(10_100), Duration.ofMillis(200), List.of(c1), true);
        var a1 = new EventExecution<>(A, Instant.ofEpochMilli(10_000), Duration.ofMillis(500), List.of(b1), true);
        var a2 = new EventExecution<>(A, Instant.ofEpochMilli(10_500), Duration.ofMillis(200), List.of(), false);
        var snapshot = new TimelineSnapshot<>(start, totalExecutionTime, List.of(a1, a2), true);

        assertThat(snapshot.eventExecutionsInOrder()).containsExactly(a1, b1, c1, a2);
    }

    private static TimelineSnapshot<SingleThreadedTimelineTest.Events> getEventsTimelineSnapshot() {
        Instant start = Instant.ofEpochMilli(10_000);
        Duration totalExecutionTime = Duration.ofMillis(888);

        var c1 = new EventExecution<>(C, Instant.ofEpochMilli(10_200), Duration.ofMillis(50), List.of(), true);
        var b1 = new EventExecution<>(B, Instant.ofEpochMilli(10_100), Duration.ofMillis(200), List.of(c1), true);
        var a1 = new EventExecution<>(A, Instant.ofEpochMilli(10_000), Duration.ofMillis(500), List.of(b1), true);
        var a2 = new EventExecution<>(A, Instant.ofEpochMilli(10_500), Duration.ofMillis(200), List.of(), false);
        return new TimelineSnapshot<>(start, totalExecutionTime, List.of(a1, a2), true);
    }
}