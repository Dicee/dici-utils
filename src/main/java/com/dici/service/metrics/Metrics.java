package com.dici.service.metrics;

import java.io.Closeable;
import java.time.Duration;

/// Abstraction for an in-memory metric, just to compile the rest of the code against. It is intended to be connected
/// to metric reporters which react to [Metrics#close()] and persist the metric externally. Could be backed by Datadog,
/// Cloudwatch etc
public interface Metrics extends Closeable {
    Metrics setOperation(String operation);

    Metrics addCount(String metricName, long count);

    Metrics addMetric(String metricName, double value);

    Metrics addDuration(String metricName, Duration duration);

    interface Factory {
        Metrics newMetrics(String operation);

        void setReporters(MetricReporter... reporters);
    }
}
