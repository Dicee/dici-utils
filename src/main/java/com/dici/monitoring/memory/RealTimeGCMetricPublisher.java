package com.dici.monitoring.memory;

import com.dici.commons.Validate;
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
public class RealTimeGCMetricPublisher implements GarbageCollectionMetricPublisher {
    private static final Map<String, Type> PAUSES_NAME_TO_TYPE = Map.of("RtGC Pauses", Type.RTGC);
    private static final Map<String, Type> STALLS_NAME_TO_TYPE = Map.of("RtGC Stalls", Type.RTGC);
    private static final Map<String, Type> CYCLES_NAME_TO_TYPE = Map.of("RtGC Cycles", Type.RTGC);
    private static final Map<String, Type> POOL_NAME_TO_TYPE = Map.of("RtGC Heap", Type.RTGC);

    /// Attempts constructing a [RealTimeGCMetricPublisher], return an empty [Optional] if it couldn't. Indeed, the current JVM may not
    /// be using a generational GC.
    public static Optional<RealTimeGCMetricPublisher> tryInstantiating(List<GarbageCollectorMXBean> collectors, List<MemoryPoolMXBean> memoryPools) {
        var pausesCollector = findCollector(collectors, PAUSES_NAME_TO_TYPE);
        var stallsCollector = findCollector(collectors, STALLS_NAME_TO_TYPE);
        var cyclesCollector = findCollector(collectors, CYCLES_NAME_TO_TYPE);
        validateCandidateCollectors(pausesCollector, stallsCollector, cyclesCollector);

        if (pausesCollector.isEmpty()) return Optional.empty();
        var memoryPool = findMemoryPool(memoryPools, POOL_NAME_TO_TYPE);

        ensureMemoryPoolFound(memoryPool, Type.RTGC.name());
        validateConsistentTypes(pausesCollector.get(), memoryPool.get());

        return Optional.of(new RealTimeGCMetricPublisher(
                new GarbageCollectorMonitor(pausesCollector.get().bean()),
                new GarbageCollectorMonitor(stallsCollector.get().bean()),
                new GarbageCollectorMonitor(cyclesCollector.get().bean()),
                memoryPool.get().bean()
        ));
    }

    @NonNull private final GarbageCollectorMonitor pausesMonitor;
    @NonNull private final GarbageCollectorMonitor stallsMonitor;
    @NonNull private final GarbageCollectorMonitor cyclesMonitor;
    @NonNull private final MemoryPoolMXBean memoryPool;

    @Override
    public void publishMetrics(MetricPublisher metricPublisher) {
        metricPublisher.publishMetrics("RealTimeGC", metrics -> {
            addGcMetrics(pausesMonitor, "Pauses", metrics);
            addGcMetrics(stallsMonitor, "Stalls", metrics);
            addGcMetrics(cyclesMonitor, "Cycles", metrics);
            addMemoryPoolMetrics(memoryPool, "Heap", metrics);
        });
    }

    private static void validateCandidateCollectors(
            Optional<CandidateBean<GarbageCollectorMXBean, Type>> pausesCollector,
            Optional<CandidateBean<GarbageCollectorMXBean, Type>> stallsCollector,
            Optional<CandidateBean<GarbageCollectorMXBean, Type>> cyclesCollector
    ) {
        Validate.that(
                pausesCollector.isPresent() == stallsCollector.isPresent() && pausesCollector.isPresent() == cyclesCollector.isPresent(),
                IllegalStateException::new,
                "Either all the RtGC collectors should be present or none, but we found: pauses=%s, stalls=%s and cycles=%s",
                getSafeBeanName(stallsCollector), getSafeBeanName(pausesCollector), getSafeBeanName(cyclesCollector)
        );
        if (pausesCollector.isEmpty()) return;

        validateConsistentTypes(List.of(pausesCollector.get(), stallsCollector.get(), cyclesCollector.get()));
    }

    private enum Type {
        RTGC
    }
}
