package com.dici.service.metrics;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.Callable;

/// This class is package private because it is meant to be exclusively used by [ServiceCall]. Making this class public may invite people to wrap their
/// [Callable] outside of [ServiceCall], leading to double counting in our metrics. Besides, this class is stateful and can only be correctly used as
/// part of a retry decorator.
@RequiredArgsConstructor
class RetriedPublishingCallable<T> implements Callable<T>  {
    private final Callable<T> underlyingCallable;
    private final Metrics metrics;

    private int attemptId = 0;
    private Throwable latestFailure;

    @Override
    public T call() throws Exception {
        if (attemptId >= 1) {
            metrics.addCount("RetryCount", 1);
            metrics.addCount("RetryOn-" + latestFailure.getClass().getSimpleName(), 1);
            metrics.addCount("RetryAttemptId-" + attemptId, 1);
        }

        attemptId++;
        try {
            return underlyingCallable.call();
        } catch (Throwable t) {
            latestFailure = t;
            throw t;
        }
    }
}
