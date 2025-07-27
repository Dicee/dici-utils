package com.dici.monitoring.memory;

import com.dici.service.metrics.MetricPublisher;

public interface GarbageCollectionMetricPublisher {
    String COLLECTIONS_COUNT = "CollectionsCount";
    String COLLECTION_TIME_MS = "CollectionTimeMs"; // accumulated on the sampling period, not for a single collection
    String SIZE_GB = "SizeGB";
    String SIZE_GB_AFTER_GC = "SizeGBAfterGC";
    String MAX_SIZE_GB = "MaxSizeGB";

    void publishMetrics(MetricPublisher metricPublisher);
}
