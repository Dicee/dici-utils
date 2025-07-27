package com.dici.monitoring.memory;

import java.lang.management.GarbageCollectorMXBean;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/// Implementation detail of this package allowing to take samples of the number of GC runs and their average duration since the last sample. The methods
/// of this class are expected to be called periodically and often enough (e.g. 1-5 minutes), and published to a system that supports aggregation on various
/// periods to make it more useful. If the data is not sampled at regular intervals or often enough, the returned data will be harder to interpret.
class GarbageCollectorMonitor {
    private final GarbageCollectorMXBean bean;
    private final Map<MeasureType, CollectionEvent> latestEvents = new EnumMap<>(MeasureType.class);

    GarbageCollectorMonitor(GarbageCollectorMXBean bean) {
        this.bean = bean;

        CollectionEvent event = getCollectionEvent();
        for (MeasureType measureType : MeasureType.values()) latestEvents.put(measureType, event);
    }

    public synchronized long recentCollectionsCount() {
        return updateLatestEvent(MeasureType.COUNT).deltaCount;
    }

    public synchronized Optional<Duration> recentAverageCollectionTime() {
        Delta delta = updateLatestEvent(MeasureType.TIME);
        return delta.deltaCount == 0 ? Optional.empty() : Optional.of(delta.deltaTime.dividedBy(delta.deltaCount));
    }

    private Delta updateLatestEvent(MeasureType measureType) {
        var before = latestEvents.get(measureType);
        var after = getCollectionEvent();
        latestEvents.put(measureType, after);

        return new Delta(
                after.collectionCount - before.collectionCount,
                after.collectionTime.minus(before.collectionTime)
        );
    }

    private CollectionEvent getCollectionEvent() {
        return new CollectionEvent(bean.getCollectionCount(), Duration.ofMinutes(bean.getCollectionTime()));
    }

    // both the count and time and the total since the JVM started, that's why we need to compute deltas by hand
    private record CollectionEvent(long collectionCount, Duration collectionTime) {}
    private record Delta(long deltaCount, Duration deltaTime) {}

    private enum MeasureType { COUNT, TIME }
}
