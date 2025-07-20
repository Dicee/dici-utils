package com.dici.insights;

import com.dici.collection.CollectionUtils;
import com.dici.insights.auditing.AuditData;
import com.dici.insights.auditing.AuditKey;
import com.dici.insights.auditing.AuditMetadata;
import com.dici.insights.reporting.InsightsDataReporter;
import com.dici.insights.reporting.Reporting;

import java.time.Clock;
import java.util.List;
import java.util.function.Function;

import static com.dici.collection.CollectionUtils.map;

/**
 * This class allows running a workflow, collect data along the way(input request, dependency
 * calls' execution time, intermediary states created in-memory etc) and finally call a collection of
 * {@link InsightsDataReporter}s, typically for persisting the audit data.
 */
public record Insights<METADATA extends AuditMetadata, EVENT extends Enum<EVENT>, AUDIT_KEY extends AuditKey>(
    METADATA metadata,
    List<InsightsDataReporter<METADATA, EVENT, AUDIT_KEY>> reporters,
    Clock clock
) {
    public Insights(METADATA metadata, List<InsightsDataReporter<METADATA, EVENT, AUDIT_KEY>> reporters) {
        this(metadata, reporters, Clock.systemUTC());
    }

    public <T> T runWithReporting(Function<InsightsData<EVENT, AUDIT_KEY>, T> function) {
        InsightsData<EVENT, AUDIT_KEY> insightsData = InsightsData.from(clock);
        boolean success = false;
        try {
            T result = function.apply(insightsData);
            success = true;
            return result;
        } finally {
            Reporting.reportAll("insights data", metadata, insightsData.getSnapshot(success),
                    map(reporters, reporter -> reporter::report));
        }
    }
}
