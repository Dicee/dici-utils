package com.dici.monitoring.memory;

import com.dici.lang.extensions.Optionals;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.Optional;

public class GCMetricPublisherResolver {
    /// Attempts discovering a known [GarbageCollectionMetricPublisher], going through each GC type sequentially until finding a configured GC or
    /// exhausting all options. If this method returns an empty optional, it must mean that the JVM uses a garbage collector not yet supported by
    /// our code, which should then be added.
    public static Optional<GarbageCollectionMetricPublisher> findGCMetricPublisher() {
        List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();

        return Optionals.firstPresent(
                GenerationalGCMetricPublisher.tryInstantiating(collectors, memoryPools),
                RealTimeGCMetricPublisher.tryInstantiating(collectors, memoryPools),
                ZeroGCMetricPublisher.tryInstantiating(collectors, memoryPools),
                ShenandoahGCMetricPublisher.tryInstantiating(collectors, memoryPools),
                EpsilonGCMetricPublisher.tryInstantiating(collectors)
        );
    }
}
