package com.dici.monitoring.memory;

import com.dici.service.metrics.MetricPublisher;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.management.GarbageCollectorMXBean;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dici.monitoring.memory.GCMetricPublisherHelper.addGcMetrics;
import static com.dici.monitoring.memory.GCMetricPublisherHelper.findCollector;

@RequiredArgsConstructor
public class EpsilonGCMetricPublisher implements GarbageCollectionMetricPublisher {
    private static final Map<String, Type> COLLECTOR_NAME_TO_TYPE = Map.of("Epsilon Heap", Type.EPSILON);

    /// Attempts constructing a [EpsilonGCMetricPublisher], return an empty [Optional] if it couldn't. Indeed, the current JVM may not
    /// be using a generational GC.
    public static Optional<EpsilonGCMetricPublisher> tryInstantiating(List<GarbageCollectorMXBean> collectors) {
        var collector = findCollector(collectors, COLLECTOR_NAME_TO_TYPE);
        return collector.map(candidate -> new EpsilonGCMetricPublisher(new GarbageCollectorMonitor(candidate.bean())));
    }

    @NonNull private final GarbageCollectorMonitor collectorMonitor;

    @Override
    public void publishMetrics(MetricPublisher metricPublisher) {
        metricPublisher.publishMetrics("EpsilonGC", metrics -> {
            addGcMetrics(collectorMonitor, "", metrics);
        });
    }

    private enum Type {
        EPSILON
    }
}
