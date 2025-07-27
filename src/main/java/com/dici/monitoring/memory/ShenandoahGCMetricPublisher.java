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
import static com.dici.monitoring.memory.GCMetricPublisherHelper.addGcMetrics;
import static com.dici.monitoring.memory.GCMetricPublisherHelper.addMemoryPoolMetrics;
import static com.dici.monitoring.memory.GCMetricPublisherHelper.validateConsistentTypes;

@RequiredArgsConstructor
public class ShenandoahGCMetricPublisher implements GarbageCollectionMetricPublisher {
    private static final Map<String, Type> PAUSES_NAME_TO_TYPE = Map.of("Shenandoah Pauses", Type.SHENANDOAH);
    private static final Map<String, Type> CYCLES_NAME_TO_TYPE = Map.of("Shenandoah Cycles", Type.SHENANDOAH);
    private static final Map<String, Type> POOL_NAME_TO_TYPE = Map.of("Shenandoah", Type.SHENANDOAH);

    /// Attempts constructing a [ShenandoahGCMetricPublisher], return an empty [Optional] if it couldn't. Indeed, the current JVM may not
    /// be using a generational GC.
    public static Optional<ShenandoahGCMetricPublisher> tryInstantiating(List<GarbageCollectorMXBean> collectors, List<MemoryPoolMXBean> memoryPools) {
        var pausesCollector = findCollector(collectors, PAUSES_NAME_TO_TYPE);
        var cyclesCollector = findCollector(collectors, CYCLES_NAME_TO_TYPE);
        validateCandidateCollectors(pausesCollector, cyclesCollector);

        if (pausesCollector.isEmpty()) return Optional.empty();

        var memoryPool = findMemoryPool(memoryPools, POOL_NAME_TO_TYPE);
        ensureMemoryPoolFound(memoryPool, Type.SHENANDOAH.name());
        validateConsistentTypes(pausesCollector.get(), memoryPool.get());

        return Optional.of(new ShenandoahGCMetricPublisher(
                new GarbageCollectorMonitor(pausesCollector.get().bean()),
                new GarbageCollectorMonitor(cyclesCollector.get().bean()),
                memoryPool.get().bean()
        ));
    }

    @NonNull private final GarbageCollectorMonitor pausesMonitor;
    @NonNull private final GarbageCollectorMonitor cyclesMonitor;
    @NonNull private final MemoryPoolMXBean memoryPool;

    @Override
    public void publishMetrics(MetricPublisher metricPublisher) {
        metricPublisher.publishMetrics("Shenandoah", metrics -> {
            addGcMetrics(pausesMonitor, "Pauses", metrics);
            addGcMetrics(cyclesMonitor, "Cycles", metrics);
            addMemoryPoolMetrics(memoryPool, "", metrics);
        });
    }

    private static void validateCandidateCollectors(
            Optional<CandidateBean<GarbageCollectorMXBean, Type>> pausesCollector,
            Optional<CandidateBean<GarbageCollectorMXBean, Type>> cyclesCollector
    ) {
        Validate.equalTo(
                pausesCollector.isPresent(),
                cyclesCollector.isPresent(),
                IllegalStateException::new,
                "Either both the pauses and cycle collectors should be present or none, but we found: pauses=%s and cycles=%s",
                getSafeBeanName(pausesCollector), getSafeBeanName(cyclesCollector)
        );
        if (pausesCollector.isEmpty()) return;

        validateConsistentTypes(List.of(pausesCollector.get(), cyclesCollector.get()));
    }

    private enum Type {
        SHENANDOAH
    }
}
