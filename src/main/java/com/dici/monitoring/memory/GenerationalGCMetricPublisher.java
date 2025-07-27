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
import static com.dici.monitoring.memory.GCMetricPublisherHelper.validateConsistentTypes;

@RequiredArgsConstructor
public class GenerationalGCMetricPublisher implements GarbageCollectionMetricPublisher {
    private static final Map<String, Type> MINOR_COLLECTOR_NAME_TO_TYPE = Map.of(
            "Copy", Type.SERIAL,
            "PS Scavenge", Type.PARALLEL,
            "ParNew", Type.CMS,
            "G1 Young Generation", Type.G1
    );
    private static final Map<String, Type> MAJOR_COLLECTOR_NAME_TO_TYPE = Map.of(
            "MarkSweepCompact", Type.SERIAL,
            "PS MarkSweep", Type.PARALLEL,
            "ConcurrentMarkSweep", Type.CMS,
            "G1 Old Generation", Type.G1
    );
    private static final Map<String, Type> EDEN_POOL_NAME_TO_TYPE = Map.of(
            "Eden Space", Type.SERIAL,
            "PS Eden Space", Type.PARALLEL,
            "Par Eden Space", Type.CMS,
            "G1 Eden Space", Type.G1
    );
    private static final Map<String, Type> SURVIVOR_POOL_NAME_TO_TYPE = Map.of(
            "Survivor Space", Type.SERIAL,
            "PS Survivor Space", Type.PARALLEL,
            "Par Survivor Space", Type.CMS,
            "G1 Survivor Space", Type.G1
    );
    private static final Map<String, Type> OLD_POOL_NAME_TO_TYPE = Map.of(
            "Tenured Gen", Type.SERIAL,
            "PS Old Gen", Type.PARALLEL,
            "CMS Old Gen", Type.CMS,
            "G1 Old Gen", Type.G1
    );

    /// Attempts constructing a [GenerationalGCMetricPublisher], return an empty [Optional] if it couldn't. Indeed, the current JVM may not
    /// be using a generational GC.
    public static Optional<GenerationalGCMetricPublisher> tryInstantiating(List<GarbageCollectorMXBean> collectors, List<MemoryPoolMXBean> memoryPools) {
        var minorCollector = findCollector(collectors, MINOR_COLLECTOR_NAME_TO_TYPE);
        var majorCollector = findCollector(collectors, MAJOR_COLLECTOR_NAME_TO_TYPE);
        validateCandidateCollectors(minorCollector, majorCollector);

        if (minorCollector.isEmpty()) return Optional.empty();

        var edenPool = findMemoryPool(memoryPools, EDEN_POOL_NAME_TO_TYPE);
        var survivorPool = findMemoryPool(memoryPools, SURVIVOR_POOL_NAME_TO_TYPE);
        var oldPool = findMemoryPool(memoryPools, OLD_POOL_NAME_TO_TYPE);
        validateCandidatePools(edenPool, survivorPool, oldPool);
        ensureMemoryPoolFound(edenPool, "generational");

        Type type = validateConsistentTypes(minorCollector.get(), edenPool.get());
        return Optional.of(new GenerationalGCMetricPublisher(
                type,
                new GarbageCollectorMonitor(minorCollector.get().bean()),
                new GarbageCollectorMonitor(majorCollector.get().bean()),
                edenPool.get().bean(),
                survivorPool.get().bean(),
                oldPool.get().bean()
        ));
    }

    @NonNull private final Type type;
    @NonNull private final GarbageCollectorMonitor minorCollectionsMonitor;
    @NonNull private final GarbageCollectorMonitor majorCollectionsMonitor;
    @NonNull private final MemoryPoolMXBean edenMemoryPool;
    @NonNull private final MemoryPoolMXBean survivorMemoryPool;
    @NonNull private final MemoryPoolMXBean oldGenMemoryPool;

    @Override
    public void publishMetrics(MetricPublisher metricPublisher) {
        metricPublisher.publishMetrics("GenerationalGC." + type, metrics -> {
            addGcMetrics(minorCollectionsMonitor, "MinorCollector", metrics);
            addGcMetrics(majorCollectionsMonitor, "MajorCollector", metrics);

            addMemoryPoolMetrics(edenMemoryPool, "Eden", metrics);
            addMemoryPoolMetrics(survivorMemoryPool, "Survivor", metrics);
            addMemoryPoolMetrics(oldGenMemoryPool, "OldGen", metrics);
        });
    }

    private static void validateCandidateCollectors(
            Optional<CandidateBean<GarbageCollectorMXBean, Type>> minorCollector,
            Optional<CandidateBean<GarbageCollectorMXBean, Type>> majorCollector
    ) {
        Validate.equalTo(
                minorCollector.isPresent(),
                majorCollector.isPresent(),
                IllegalStateException::new,
                "Either both the minor and major collectors should be present or none, but we found: minor=%s and major=%s",
                getSafeBeanName(minorCollector), getSafeBeanName(majorCollector)
        );
        if (minorCollector.isEmpty()) return;

        validateConsistentTypes(List.of(minorCollector.get(), majorCollector.get()));
    }

    private static void validateCandidatePools(
            Optional<CandidateBean<MemoryPoolMXBean, Type>> edenPool,
            Optional<CandidateBean<MemoryPoolMXBean, Type>> survivorPool,
            Optional<CandidateBean<MemoryPoolMXBean, Type>> oldPool
    ) {
        Validate.that(
                edenPool.isPresent() == survivorPool.isPresent() && edenPool.isPresent() == oldPool.isPresent(),
                IllegalStateException::new,
                "Either all generational memory pools should be present or none, but we found: Eden=%s, Survivor=%s, Old=%s",
                getSafeBeanName(edenPool), getSafeBeanName(survivorPool), getSafeBeanName(oldPool)
        );
        if (edenPool.isEmpty()) return;

        validateConsistentTypes(List.of(edenPool.get(), survivorPool.get(), oldPool.get()));
    }

    private enum Type {
        SERIAL,
        PARALLEL,
        CMS,
        G1
    }
}
