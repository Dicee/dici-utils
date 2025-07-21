package com.dici.insights.timeline;

import com.dici.testing.time.FakeTicker;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static com.dici.insights.timeline.SingleThreadedTimelineTest.Events.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SingleThreadedTimelineTest {
    public enum Events {
        A, B, C
    }

    @Test
    void testRecordEvent_runnable() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10_000), ZoneId.of("UTC"));
        Timeline<Events> timeline = SingleThreadedTimeline.createStarted(clock);

        Runnable runnable = mock(Runnable.class);
        timeline.recordEvent(A, runnable);

        TimelineSnapshot<Events> snapshot = timeline.getSnapshot(true);
        verify(runnable).run();

        var expectedEvent = new EventExecution<>(A, Instant.ofEpochMilli(10_000), Duration.ofMillis(0), List.of(), true);
        assertThat(snapshot.rootEvents()).containsExactly(expectedEvent);
    }

    @Test
    void testRecordEvent_supplier() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10_000), ZoneId.of("UTC"));
        Timeline<Events> timeline = SingleThreadedTimeline.createStarted(clock);

        String result = timeline.recordEvent(A, () -> "hello");
        assertThat(result).isEqualTo("hello");

        TimelineSnapshot<Events> snapshot = timeline.getSnapshot(true);
        var expectedEvent = new EventExecution<>(A, Instant.ofEpochMilli(10_000), Duration.ofMillis(0), List.of(), true);
        assertThat(snapshot.rootEvents()).containsExactly(expectedEvent);
    }

    @Test
    void testRecordEvent_nested() {
        Instant initialTime = Instant.ofEpochMilli(10_000);
        FakeTicker ticker = new FakeTicker(initialTime);

        Timeline<Events> timeline = new SingleThreadedTimeline<>(ticker.asClock(), initialTime);

        timeline.recordEvent(A, () -> {
            timeline.recordEvent(B, () -> {
                ticker.advance(25, MILLISECONDS);
            });

            ticker.advance(50, MILLISECONDS);
            timeline.recordEvent(C, () -> {
                ticker.advance(100, MILLISECONDS);
            });

            ticker.advance(10, MILLISECONDS);
        });

        TimelineSnapshot<Events> snapshot = timeline.getSnapshot(true);

        var b = new EventExecution<>(B, initialTime, Duration.ofMillis(25), List.of(), true);
        var c = new EventExecution<>(C, initialTime.plusMillis(75), Duration.ofMillis(100), List.of(), true);
        var a = new EventExecution<>(A, initialTime, Duration.ofMillis(185), List.of(b, c), true);
        assertThat(snapshot.rootEvents()).containsExactly(a);
    }

    @Test
    void testRecordEventException() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(10_000), ZoneId.of("UTC"));
        Timeline<Events> timeline = SingleThreadedTimeline.createStarted(clock);

        assertThrows(RuntimeException.class, () -> timeline.recordEvent(A, () -> {
            throw new RuntimeException("test");
        }));

        TimelineSnapshot<Events> snapshot = timeline.getSnapshot(false);
        var expectedEvent = new EventExecution<>(A, Instant.ofEpochMilli(10_000), Duration.ofMillis(0), List.of(), false);
        assertThat(snapshot.rootEvents()).containsExactly(expectedEvent);
        assertThat(snapshot.success()).isFalse();
    }
}