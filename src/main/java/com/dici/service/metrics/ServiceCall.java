package com.dici.service.metrics;

import com.dici.service.exception.DependencyException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.concurrent.Callable;

import static com.dici.time.TimeUtils.humanReadableDuration;

/// Provides a consistent mechanism to decorate dependency calls with metrics and error handling.
@Log4j2
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceCall {
    private static final String TOTAL_FAILURES = "TotalFailures";
    private static final String PER_EXCEPTION_FAILURE_PREFIX = "Failure-";
    private static final String LATENCY = "Latency"; // single call latency
    private static final String END_TO_END_LATENCY = "EndToEndLatency"; // total latency including retries, if any

    public static ServiceCall of(String service, MetricPublisher metricPublisher) {
        return new ServiceCall(service, metricPublisher, DependencyException::new,
                RetryingCallableFactory.NO_RETRY, false, Ticker.systemTicker());
    }

    @NonNull private final String service;
    @NonNull private final MetricPublisher metricPublisher;

    @With
    @NonNull private final DependencyExceptionDecorator onException;

    @With(value = AccessLevel.PRIVATE)
    @NonNull private final RetryingCallableFactory retryingCallableFactory;

    @With(value = AccessLevel.PRIVATE)
    private final boolean shouldLogLatency;

    @VisibleForTesting
    @With(value = AccessLevel.PROTECTED)
    @NonNull private final Ticker ticker;

    public ServiceCall retryWith(Retry retry, CircuitBreaker circuitBreaker) {
        return withRetryingCallableFactory(new RetryingCallableFactory() {
            @Override
            public <T> Callable<T> wrap(Callable<T> callable, Metrics metrics) {
                return Decorators.ofCallable(new RetriedPublishingCallable<>(callable, metrics))
                        .withRetry(retry)
                        .withCircuitBreaker(circuitBreaker)
                        .decorate();
            }
        });
    }

    public ServiceCall enableLatencyLogging() {
        return withShouldLogLatency(true);
    }

    public void callVoid(String operation, Runnable runnable) {
        call(operation, () -> {
            runnable.run();
            return null;
        });
    }

    /// Calls the dependency and emits latency as well as success/failure metrics (both globally and exception-grained).
    public <T> T call(String operation, Callable<T> callable) {
        String fullOperationName = "%s.%s".formatted(service, operation);
        Stopwatch e2eStopwatch = Stopwatch.createStarted(ticker);

        return metricPublisher.getWithMetrics(fullOperationName, metrics -> {
            boolean success = false;
            try {
                TimedCallable<T> timedCallable = new TimedCallable<>(callable, metrics, ticker);
                T result = retryingCallableFactory.wrap(timedCallable, metrics).call();

                metrics.addCount(TOTAL_FAILURES, 0);
                success = true;

                return result;
            } catch (Exception e) {
                metrics.addCount(PER_EXCEPTION_FAILURE_PREFIX + e.getClass().getSimpleName(), 1);
                metrics.addCount(TOTAL_FAILURES, 1);
                throw onException.decorate(service, operation, e);
            } finally {
                Duration latency = e2eStopwatch.elapsed();
                metrics.addDuration(END_TO_END_LATENCY, latency);

                if (shouldLogLatency) {
                    log.info("Operation {} {} in {}", fullOperationName, success ? "succeeded" : "failed", humanReadableDuration(latency));
                }
            }
        });
    }

    /// Allows customizing the exception thrown by the dependency
    interface DependencyExceptionDecorator {
        DependencyException decorate(String service, String operation, Throwable cause);
    }

    private interface RetryingCallableFactory {
        RetryingCallableFactory NO_RETRY = new RetryingCallableFactory() {
            @Override
            public <T> Callable<T> wrap(Callable<T> callable, Metrics metrics) {
                return callable;
            }
        };

        <T> Callable<T> wrap(Callable<T> callable, Metrics metrics);
    }

    private record TimedCallable<T>(
            @NonNull Callable<T> underlyingCallable,
            @NonNull Metrics metrics,
            @NonNull Ticker ticker
    ) implements Callable<T> {
        @Override
        public T call() throws Exception {
            Stopwatch stopwatch = Stopwatch.createStarted(ticker);
            try {
                return underlyingCallable.call();
            } finally {
                metrics.addDuration(LATENCY, stopwatch.elapsed());
            }
        }
    }
}
