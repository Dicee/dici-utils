package com.dici.insights;

import com.dici.insights.auditing.AuditData;
import com.dici.insights.auditing.AuditKey;
import com.dici.insights.auditing.ReadOnlyAuditData;
import com.dici.insights.timeline.SingleThreadedTimeline;
import com.dici.insights.timeline.Timeline;
import com.dici.insights.timeline.TimelineSnapshot;

import java.time.Clock;

public record InsightsData<EVENT extends Enum<EVENT>, AUDIT_KEY extends AuditKey>(
        Timeline<EVENT> timeline,
        AuditData<AUDIT_KEY> auditData
){
    public static  <EVENT extends Enum<EVENT>, AUDIT_KEY extends AuditKey> InsightsData<EVENT, AUDIT_KEY> from(Clock clock) {
        Timeline<EVENT> timeline = SingleThreadedTimeline.createStarted(clock);
        return new InsightsData<>(timeline, AuditData.empty());
    }

    public Snapshot<EVENT, AUDIT_KEY> getSnapshot(boolean success) {
        return new Snapshot<>(timeline.getSnapshot(success), auditData);
    }

    public record Snapshot<EVENT extends Enum<EVENT>, AUDIT_KEY extends AuditKey>(
            TimelineSnapshot<EVENT> timelineSnapshot,
            ReadOnlyAuditData<AUDIT_KEY> auditData
    ) {}
}
