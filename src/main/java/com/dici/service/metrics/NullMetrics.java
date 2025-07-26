package com.dici.service.metrics;

import java.io.IOException;
import java.time.Duration;

/// No-op implementation of [Metrics]. Can be useful in some tests or local execution.
public class NullMetrics implements Metrics {
    @Override public Metrics setOperation(String operation) { return this; }
    @Override public Metrics addCount(String metricName, int count) { return this; }
    @Override public Metrics addMetric(String metricName, double value) { return this; }
    @Override public Metrics addDuration(String metricName, Duration duration) { return this; }
    @Override public void close() throws IOException {}
}
