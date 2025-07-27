package com.dici.monitoring.memory;

import com.dici.service.metrics.MetricPublisher;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dici.monitoring.memory.GCMetricPublisherHelper.*;

@RequiredArgsConstructor
public class ZeroGCMetricPublisher implements GarbageCollectionMetricPublisher {
    private static final Map<String, Type> COLLECTOR_NAME_TO_TYPE = Map.of("ZGC", Type.ZGC);
    private static final Map<String, Type> POOL_NAME_TO_TYPE = Map.of("ZHeap", Type.ZGC);

    /// Attempts constructing a [ZeroGCMetricPublisher], return an empty [Optional] if it couldn't. Indeed, the current JVM may not
    /// be using a generational GC.
    public static Optional<ZeroGCMetricPublisher> tryInstantiating(List<GarbageCollectorMXBean> collectors, List<MemoryPoolMXBean> memoryPools) {
        var collector = findCollector(collectors, COLLECTOR_NAME_TO_TYPE);
        if (collector.isEmpty()) return Optional.empty();

        var memoryPool = findMemoryPool(memoryPools, POOL_NAME_TO_TYPE);

        ensureMemoryPoolFound(memoryPool, Type.ZGC.name());
        validateConsistentTypes(collector.get(), memoryPool.get());

        return Optional.of(new ZeroGCMetricPublisher(
                new GarbageCollectorMonitor(collector.get().bean()),
                memoryPool.get().bean()
        ));
    }

    @NonNull private final GarbageCollectorMonitor collectorMonitor;
    @NonNull private final MemoryPoolMXBean memoryPool;

    @Override
    public void publishMetrics(MetricPublisher metricPublisher) {
        metricPublisher.publishMetrics("ZeroGC", metrics -> {
            addGcMetrics(collectorMonitor, "", metrics);
            addMemoryPoolMetrics(memoryPool, "", metrics);
        });
    }

    private enum Type {
        ZGC
    }
}
