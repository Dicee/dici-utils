
package com.dici.service.metrics;

import com.dici.service.exception.DependencyException;
import com.dici.testing.logging.RecordingAppender;
import com.dici.testing.time.FakeTicker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static com.dici.testing.assertj.BetterAssertions.assertThatThrownBy;
import static com.dici.testing.logging.RecordingAppender.ROOT_LOGGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@MockitoSettings
class ServiceCallTest {
    private static final String SERVICE   = "Service";
    private static final String OPERATION = "Operation";
    public static final String RESULT = "result";
    public static final IOException EXCEPTION = new IOException("error");

    @Mock private MetricPublisher metricPublisher;
    @Mock private Metrics metrics;
    @Mock private Runnable runnable;
    @Mock private Callable<String> callable;

    private RecordingAppender recordingAppender;
    private FakeTicker fakeTicker;

    @BeforeEach
    void setUp() {
        when(metricPublisher.getWithMetrics(anyString(), any())).thenAnswer(invocation -> {
            Function<Metrics, String> handler = invocation.getArgument(1);
            return handler.apply(metrics);
        });
        recordingAppender = RecordingAppender.attachedToLogger(ROOT_LOGGER);
        fakeTicker = new FakeTicker();
    }

    @AfterEach
    void tearDown() {
        recordingAppender.detachFromRootLogger(ROOT_LOGGER);
    }

    @Nested
    class HappyPath {
        @Test
        void testCall_success() throws Exception {
            Duration latency = Duration.ofSeconds(2);
            when(callable.call()).thenAnswer(succeedWithLatency(latency));

            assertThat(ServiceCall.of(SERVICE, metricPublisher)
                    .withTicker(fakeTicker)
                    .call(OPERATION, callable)
            ).isEqualTo(RESULT);

            verify(metrics).addCount("TotalFailures", 0);
            verify(metrics).addDuration("Latency", latency);
            verify(metrics).addDuration("EndToEndLatency", latency);
            verifyNoMoreInteractions(metrics);
        }

        @Test
        void callVoid_success() {
            Duration latency = Duration.ofSeconds(2);
            doAnswer(__ -> fakeTicker.advance(latency)).when(runnable).run();

            ServiceCall.of(SERVICE, metricPublisher)
                    .withTicker(fakeTicker)
                    .callVoid(OPERATION, runnable);

            verify(runnable).run();
            verify(metrics).addCount("TotalFailures", 0);
            verify(metrics).addDuration("Latency", latency);
            verify(metrics).addDuration("EndToEndLatency", latency);
            verifyNoMoreInteractions(metrics);
        }
    }

    @Nested
    class ErrorHandling {
        @Test
        void testCall_failure() throws Exception {
            Exception e = EXCEPTION;
            when(callable.call()).thenThrow(e);

            assertThatThrownBy(() -> ServiceCall.of(SERVICE, metricPublisher).call(OPERATION, callable))
                    .isLike(new DependencyException(SERVICE, OPERATION, e));

            verify(metrics).addCount("Failure-IOException", 1);
            verify(metrics).addCount("TotalFailures", 1);
            verify(metrics).addDuration(eq("Latency"), any(Duration.class));
            verify(metrics).addDuration(eq("EndToEndLatency"), any(Duration.class));
            verifyNoMoreInteractions(metrics);
        }

        @Test
        void testCall_failure_customOnException() throws Exception {
            when(callable.call()).thenThrow(EXCEPTION);

            assertThatThrownBy(() -> ServiceCall.of(SERVICE, metricPublisher)
                .withOnException((service, operation, cause) ->
                        DependencyException.withAdditionalDetails(SERVICE, OPERATION, "This user has been banned", cause))
                .call(OPERATION, callable)
            ).isInstanceOf(DependencyException.class)
             .hasMessage("Error while calling dependency Service.Operation due to: This user has been banned")
             .hasCause(EXCEPTION);

            verify(metrics).addCount("Failure-IOException", 1);
            verify(metrics).addCount("TotalFailures", 1);
            verify(metrics).addDuration(eq("Latency"), any(Duration.class));
            verify(metrics).addDuration(eq("EndToEndLatency"), any(Duration.class));
            verifyNoMoreInteractions(metrics);
        }
    }

    @Nested
    class Retries {
        private static final int MAX_ATTEMPTS = 3;
        private final Retry retry = Retry.of("retry", RetryConfig.custom().maxAttempts(MAX_ATTEMPTS).build());
        private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("circuitBreaker");

        @Test
        void testCall_retryThenSuccess() throws Exception {
            Duration failureLatency = Duration.ofSeconds(MAX_ATTEMPTS);
            Duration successLatency = Duration.ofSeconds(1);

            when(callable.call())
                    .thenAnswer(failWithLatency(failureLatency))
                    .thenAnswer(succeedWithLatency(successLatency));

            assertThat(ServiceCall.of(SERVICE, metricPublisher)
                .retryWith(retry, circuitBreaker)
                .withTicker(fakeTicker)
                .call(OPERATION, callable)
            ).isEqualTo(RESULT);

            verify(callable, times(2)).call();
            verify(metrics).addCount("RetryCount", 1);
            verify(metrics).addCount("RetryOn-IOException", 1);
            verify(metrics).addCount("RetryAttemptId-1", 1);

            verify(metrics).addCount("TotalFailures", 0); // since it was eventually a success, we don't count it as a final failure

            verify(metrics).addDuration("Latency", failureLatency);
            verify(metrics).addDuration("Latency", successLatency);
            verify(metrics).addDuration("EndToEndLatency", failureLatency.plus(successLatency));

            verifyNoMoreInteractions(metrics);
        }

        @Test
        void testCall_retryAndContinuesFailing() throws Exception {
            Duration latency = Duration.ofSeconds(MAX_ATTEMPTS);

            when(callable.call()).thenAnswer(failWithLatency(latency));

            assertThatThrownBy(() -> ServiceCall.of(SERVICE, metricPublisher)
                    .retryWith(retry, circuitBreaker)
                    .withTicker(fakeTicker)
                    .call(OPERATION, callable)
            ).isLike(new DependencyException(SERVICE, OPERATION, EXCEPTION));

            verify(callable, times(MAX_ATTEMPTS)).call();
            verify(metrics, times(MAX_ATTEMPTS - 1)).addCount("RetryCount", 1);
            verify(metrics, times(MAX_ATTEMPTS - 1)).addCount("RetryOn-IOException", 1);
            verify(metrics).addCount("RetryAttemptId-1", 1);
            verify(metrics).addCount("RetryAttemptId-2", 1);

            verify(metrics).addCount("Failure-IOException", 1);
            verify(metrics).addCount("TotalFailures", 1);

            verify(metrics, times(MAX_ATTEMPTS)).addDuration("Latency", latency);
            verify(metrics).addDuration("EndToEndLatency", latency.multipliedBy(MAX_ATTEMPTS));

            verifyNoMoreInteractions(metrics);
        }
    }

    @Nested
    class LatencyLogging {
        @Test
        void testCall_logsLatencyWhenEnabled_success() throws Exception {
            when(callable.call()).thenAnswer(succeedWithLatency(Duration.ofMillis(3560)));

            ServiceCall.of(SERVICE, metricPublisher)
                    .enableLatencyLogging()
                    .withTicker(fakeTicker)
                    .call(OPERATION, callable);

            assertThat(recordingAppender.getMessages()).containsExactly("Operation Service.Operation succeeded in 3 seconds and 560 milliseconds");
        }

        @Test
        void testCall_logsLatencyWhenEnabled_failure() throws Exception {
            when(callable.call()).thenAnswer(failWithLatency(Duration.ofMillis(3560)));

            assertThatThrownBy(() -> ServiceCall.of(SERVICE, metricPublisher)
                    .enableLatencyLogging()
                    .withTicker(fakeTicker)
                    .call(OPERATION, callable)
            ).isLike(new DependencyException(SERVICE, OPERATION, EXCEPTION));

            assertThat(recordingAppender.getMessages()).containsExactly("Operation Service.Operation failed in 3 seconds and 560 milliseconds");
        }

        @Test
        void testCall_doesNotLogLatencyWhenDisabled() throws Exception {
            when(callable.call()).thenReturn(RESULT);

            ServiceCall.of(SERVICE, metricPublisher).call(OPERATION, callable);

            assertThat(recordingAppender.getEvents()).isEmpty();
        }
    }

    @NotNull
    private Answer<String> succeedWithLatency(Duration latency) {
        return __ -> {
            fakeTicker.advance(latency);
            return RESULT;
        };
    }

    private Answer<Object> failWithLatency(Duration failureLatency) {
        return __ -> {
            fakeTicker.advance(failureLatency);
            throw EXCEPTION;
        };
    }
}
