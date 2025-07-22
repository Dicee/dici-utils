package com.dici.logging;

import lombok.*;
import org.apache.logging.log4j.ThreadContext;

import java.util.Map;

public class ThreadContextUtils {
    public static Snapshot snapshotCurrent() {
        return new Snapshot(ThreadContext.getImmutableContext());
    }

    /// Black box snapshot of the context which doesn't expose the context directly. Its role is to contain a copy
    /// of the context from the current thread and then dump in new threads to keep the context on the logs produced
    /// within these threads
    @Value
    @Getter(AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Snapshot {
        @NonNull private final Map<String, String> context;

        /// Copies the snapshot values to the current thread's context
        public void copyToCurrentThreadContext() {
            ThreadContext.putAll(context);
        }
    }
}
