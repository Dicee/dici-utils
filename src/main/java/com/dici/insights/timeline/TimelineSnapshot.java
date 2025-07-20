package com.dici.insights.timeline;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static com.dici.time.TimeUtils.humanReadableDuration;

public record TimelineSnapshot<EVENT extends Enum<EVENT>>(
        Instant start,
        Duration totalExecutionTime,
        List<EventExecution<EVENT>> rootEvents,
        /**
         * Whether the series of events ended in a final success or not. Not that the overall request is correlated but
         * not equivalent to the conjunction of its independent events' success:
         * - a request with 0 recorded events can still have failed
         * - a request with a failed event might have recovered from it
         * - a request might fail outside of a recorded event
         */
        boolean success
) {
    public Instant end() {
        return start.plus(totalExecutionTime);
    }

    public List<EventExecution<EVENT>> eventExecutionsInOrder() {
        Stream.Builder<EventExecution<EVENT>> acc = Stream.<EventExecution<EVENT>>builder();
        rootEvents.forEach(root -> collectEventExecutions(root, acc));
        return acc.build().toList();
    }

    // we should not face too deep recursions here, and a typical JVM size can easily handle ~7000 stack frames
    private void collectEventExecutions(EventExecution<EVENT> event, Stream.Builder<EventExecution<EVENT>> acc) {
        acc.add(event);
        event.children().forEach(child -> collectEventExecutions(child, acc));
    }

    /**
     * Builds a human-readable string representing all events in the timeline in a nested fashion to help quickly visualizing
     * the execution path and track per-event and global execution time at every step. The string leaves no white space between
     * millis timestamps on the right and the event names on the left so that indentation is not lost when reading from e.g.
     * AWS Athena/HiveQL. Example output below:
     *
     * Dummy request @ 1970-01-01T00:00:00.002Z took 200 milliseconds
     * +0ms.....Timeline (total: 2 seconds) {
     * +5ms.......VALIDATE_INPUT (total: 5 milliseconds) {
     * +10ms......}
     * +12ms......CHECK_PERMISSIONS (total: 3 milliseconds) {
     * +15ms......}
     * +18ms......FETCH_USER_DATA (total: 135 milliseconds) {
     * +25ms........FETCH_REDIS_SESSION_DATA (total: 80 milliseconds) {
     * +105ms.......}
     * +106ms.......FETCH_PAYMENT_HISTORY (total: 20 milliseconds) {
     * +126ms.......}
     * +153ms.....}
     * +120ms.....FINALIZE_TRANSACTION (total: 30 milliseconds) {
     * +150ms.....}
     * +200ms..}
     */
    public String toPrettyString(String requestDescription) {
        StringBuilder sb = new StringBuilder()
                .append(requestDescription)
                .append(" @ ").append(start)
                .append(" took ").append(humanReadableDuration(totalExecutionTime)).append("\n");

        int timestampLength = getTimestampString(end()).length() + 1;
        int depth = 0;

        appendEventStart(start, "Timeline", totalExecutionTime, timestampLength, depth, sb);
        rootEvents.forEach(child -> appendPrettyString(child, timestampLength, depth + 1, sb));
        appendEventEnd(end(), timestampLength, depth, sb);

        return sb.toString().trim();
    }

    private void appendEventStart(Instant start, String eventName, Duration executionTime, int timestampLength, int depth, StringBuilder sb) {
        appendTimestampAndIndexPrefix(start, timestampLength, depth, sb);
        sb.append(eventName);
        sb.append(" (total: %s) {".formatted(humanReadableDuration(executionTime))).append('\n');
    }

    private void appendTimestampAndIndexPrefix(Instant instant, int timestampLength, int depth, StringBuilder sb) {
        String timestampText = getTimestampString(instant);
        sb.append('+').append(timestampText).append("ms");
        sb.append(".".repeat(timestampLength - timestampText.length() + 1)).append(".".repeat(2 * depth));
    }

    private void appendPrettyString(EventExecution<? extends Enum<?>> event, int timestampLength, int depth, StringBuilder sb) {
        appendEventStart(event.start(), event.event().name(), event.executionTime(), timestampLength, depth, sb);
        event.children().forEach(child -> appendPrettyString(child, timestampLength, depth + 1, sb));
        appendEventEnd(event.end(), timestampLength, depth, sb);
    }

    private void appendEventEnd(Instant end, int timestampLength, int depth, StringBuilder sb) {
        appendTimestampAndIndexPrefix(end, timestampLength, depth, sb);
        sb.append("}\n");
    }

    private String getTimestampString(Instant instant) {
        return Long.toString(Duration.between(start, instant).toMillis());
    }
}
