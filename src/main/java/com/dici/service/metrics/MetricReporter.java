package com.dici.service.metrics;

import java.time.Duration;

/// Accumulates an in-memory representation of a set of metrics to publish and then flushes upon calling [MetricReporter#endReport()].
/// This abstracts the actual metrics store from the rest of the code, and allows providing multiple stores or swapping them easily
/// without changing application code.
public interface MetricReporter {
    void beginReport();

    void addCount(String metricName, int count);

    void addDuration(String metricName, Duration duration);

    void endReport();

    interface Factory {
        MetricReporter newReporter();
    }
}
