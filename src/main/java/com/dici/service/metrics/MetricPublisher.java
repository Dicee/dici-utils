package com.dici.service.metrics;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

/// Abstraction for a metric publisher, just to compile the rest of the code against. Could be backed by Datadog,
/// Cloudwatch etc
public interface MetricPublisher {
    void publishCount(String operation, String metricName, int count);

    void publishMetric(String operation, String metricName, double value);

    void publishDuration(String operation, String metricName, Duration duration);

    void publishMetrics(String operation, Consumer<Metrics> addMetrics);

    <T> T getWithMetrics(String operation, Function<Metrics, T> function);

    MetricPublisher NOOP = new MetricPublisher() {
        @Override public void publishCount(String operation, String metricName, int count) {}
        @Override public void publishMetric(String operation, String metricName, double value) {}
        @Override public void publishDuration(String operation, String metricName, Duration duration) {}
        @Override public void publishMetrics(String operation, Consumer<Metrics> addMetrics) {}
        @Override public <T> T getWithMetrics(String operation, Function<Metrics, T> function) { return function.apply(new NullMetrics()); }
    };
}
