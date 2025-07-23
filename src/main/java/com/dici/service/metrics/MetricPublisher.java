package com.dici.service.metrics;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

/// Abstraction for a metric publisher, just to compile the rest of the code against. Could be backed by Datadog,
/// Cloudwatch etc
public interface MetricPublisher {
    void publishCount(String operation, String metricName, int count);

    void publishDuration(String operation, String metricName, Duration duration);

    void publishMetrics(String operation, Consumer<Metrics> addMetrics);

    <T> T getWithMetrics(String operation, Function<Metrics, T> function);
}
