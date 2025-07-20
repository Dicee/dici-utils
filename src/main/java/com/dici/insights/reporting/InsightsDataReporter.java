package com.dici.insights.reporting;

import com.dici.insights.InsightsData;
import com.dici.insights.auditing.AuditKey;
import com.dici.insights.auditing.AuditMetadata;

/// This interface allows defining what needs to be done with the data collected by [InsightsData]
public interface InsightsDataReporter<METADATA extends AuditMetadata, EVENT extends Enum<EVENT>, AUDIT_KEY extends AuditKey> {
    void report(METADATA metadata, InsightsData.Snapshot<EVENT, AUDIT_KEY> snapshot);

    /// Simple reporter which simply pretty-prints the timeline to stdout
    static <M extends AuditMetadata, E extends Enum<E>, K extends AuditKey> InsightsDataReporter<M, E, K> dumpTimeline() {
        return ((metadata, snapshot) -> System.out.println(snapshot.timelineSnapshot().toPrettyString(metadata.describe())));
    }
}

