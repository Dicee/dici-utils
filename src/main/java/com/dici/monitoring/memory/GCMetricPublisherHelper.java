package com.dici.monitoring.memory;

import com.dici.commons.Validate;
import com.dici.service.metrics.Metrics;
import org.jetbrains.annotations.NotNull;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.PlatformManagedObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.dici.monitoring.memory.GarbageCollectionMetricPublisher.*;

/// Intended for internal usage to this package, to put common logic in one place
class GCMetricPublisherHelper {
    private static final double GIGABYTE = 1024.0 * 1024.0 * 1024.0;

    static void addGcMetrics(GarbageCollectorMonitor monitor, String collectorType, Metrics metrics) {
        metrics.addCount(getPrefixedMetricName(collectorType, COLLECTIONS_COUNT), monitor.recentCollectionsCount());
        monitor.recentAverageCollectionTime().ifPresent(d ->
                metrics.addMetric(getPrefixedMetricName(collectorType, COLLECTION_TIME_MS), d.toMillis()));
    }

    static void addMemoryPoolMetrics(MemoryPoolMXBean pool, String poolName, Metrics metrics) {
        metrics.addMetric(getPrefixedMetricName(poolName, SIZE_GB), pool.getUsage().getUsed() / GIGABYTE);
        metrics.addMetric(getPrefixedMetricName(poolName, SIZE_GB_AFTER_GC), pool.getCollectionUsage().getUsed() / GIGABYTE);
        metrics.addMetric(getPrefixedMetricName(poolName, MAX_SIZE_GB), pool.getUsage().getMax() / GIGABYTE);
    }

    private static String getPrefixedMetricName(String prefix, String metricName) {
        if (prefix.isBlank()) return metricName;
        return prefix + "." + metricName;
    }

    static <T extends Enum<T>> Optional<CandidateBean<GarbageCollectorMXBean, T>> findCollector(List<GarbageCollectorMXBean> collectors, Map<String, T> collectorNameToType) {
        return findBean(collectors, GarbageCollectorMXBean::getName, collectorNameToType, "collector");
    }

    static <T extends Enum<T>> Optional<CandidateBean<MemoryPoolMXBean, T>> findMemoryPool(List<MemoryPoolMXBean> pools, Map<String, T> poolNameToType) {
        return findBean(pools, MemoryPoolMXBean::getName, poolNameToType, "memory pool");
    }

    static <B extends PlatformManagedObject, T extends Enum<T>> Optional<CandidateBean<B, T>> findBean(
            List<B> beans, Function<B, String> getName, Map<String, T> beanNameToType, String beanTypeDesc) {

        CandidateBean<B, T> candidate = null;

        // We iterate through all of them to make sure we are making the right assumptions and there's no conflict. We're not worried about performance,
        // this will be a small list.
        for (B bean : beans) {
            String name = getName.apply(bean);
            T type = beanNameToType.get(name);
            if (type != null) {
                if (candidate != null) {
                    throw new IllegalStateException("Conflicting %ss detected: %s with type %s and %s with type %s".formatted(
                            beanTypeDesc, name, type, getName.apply(candidate.bean), candidate.type));
                }
                candidate = new CandidateBean<>(name, type, bean);

            }
        }

        return Optional.ofNullable(candidate);
    }

    static <B extends PlatformManagedObject, T extends Enum<T>> String getSafeBeanName(Optional<CandidateBean<B, T>> candidate) {
        return candidate.map(CandidateBean::name).orElse("<missing>");
    }

    static <B extends PlatformManagedObject, T extends Enum<T>> void validateConsistentTypes(List<CandidateBean<B, T>> beans) {
        long distinctTypes = beans.stream().map(CandidateBean::type).distinct().count();
        Validate.equalTo(distinctTypes, 1, IllegalStateException::new, "Inconsistent GC types for: %s", beans);
    }

    static <T extends Enum<T>> void ensureMemoryPoolFound(Optional<CandidateBean<MemoryPoolMXBean, T>> memoryPool, String gcTypeDesc) {
        if (memoryPool.isEmpty()) {
            throw new IllegalStateException("Failed to find %s memory pools despite the GC clearly being identified as %s".formatted(gcTypeDesc, gcTypeDesc));
        }
    }

    static <T extends Enum<T>> T validateConsistentTypes(CandidateBean<GarbageCollectorMXBean, T> collector, CandidateBean<MemoryPoolMXBean, T> memoryPool) {
        T collectorType = collector.type();
        T memoryPoolType = memoryPool.type();
        Validate.equalTo(collectorType, memoryPoolType, IllegalStateException::new,
                "Inconsistent GC type between the collector(s) (%s) and memory pool(s) (%s)", collectorType, memoryPoolType);
        return collectorType;
    }

    record CandidateBean<B extends PlatformManagedObject, T extends Enum<T>>(String name, T type, B bean) {
        @NotNull
        @Override
        public String toString() {
            return "%s(name=%s, gcType=%s)".formatted(bean.getClass(), name, type);
        }
    }
}
