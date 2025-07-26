package com.dici.aws.kinesis;

import com.dici.aws.exception.AwsDependencyException;
import com.dici.aws.kinesis.KinesisFirehoseAccumulatingPublisher.KinesisFirehoseRecordAccumulatorFactory;
import com.dici.commons.Validate;
import com.dici.service.metrics.MetricPublisher;
import com.dici.service.metrics.Metrics;
import com.dici.testing.logging.RecordingAppender;
import com.dici.testing.logging.SimpleLogEvent;
import com.dici.testing.time.FakeTicker;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse;
import software.amazon.awssdk.services.firehose.model.Record;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.dici.testing.assertj.BetterAssertions.assertThatThrowable;
import static com.dici.testing.logging.RecordingAppender.ROOT_LOGGER;
import static com.google.common.collect.Iterables.partition;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings
class KinesisFirehoseAccumulatingPublisherTest {
    private static final Duration TARGET_FLUSH_DELAY = Duration.ofSeconds(2);

    private static final String HELLO = "hello";
    private static final String STREAM_NAME = "hello-stream";
    private static final Function<String, byte[]> SERIALIZER = String::getBytes;
    private static final KinesisFirehoseLimits LIMITS = KinesisFirehoseLimits.defaults();
    private static final PutRecordBatchRequest PUT_RECORD_BATCH_REQUEST = PutRecordBatchRequest.builder()
            .deliveryStreamName(STREAM_NAME)
            .records(Record.builder().data(SdkBytes.fromUtf8String(HELLO)).build())
            .build();
    private static final PutRecordBatchResponse PUT_RECORD_BATCH_RESPONSE = PutRecordBatchResponse.builder().build();

    @Mock private ExecutorService putRecordWorkers;
    @Mock private FirehoseAsyncClient firehose;
    @Mock private MetricPublisher metricPublisher;
    @Mock private Metrics metrics;
    @Mock private ScheduledExecutorService flushWorker;
    @Mock private KinesisFirehoseRecordAccumulatorFactory<String> accumulatorFactory;
    @Mock private KinesisFirehoseRecordAccumulator<String> accumulator;

    @Captor private ArgumentCaptor<Consumer<PutRecordBatchRequest>> onBufferFlushCaptor;
    @Captor private ArgumentCaptor<PutRecordBatchRequest> putRecordBatchRequestCaptor;
    @Captor private ArgumentCaptor<Runnable> flushTaskCaptor;

    private FakeTicker ticker;
    private RecordingAppender recordingAppender;
    private KinesisFirehoseAccumulatingPublisher<String> publisher;

    @BeforeEach
    void setUp() {
        when(accumulatorFactory.create(eq(STREAM_NAME), same(SERIALIZER), any(), eq(LIMITS))).thenReturn(accumulator);

        ticker = new FakeTicker();
        recordingAppender = RecordingAppender.attachedToLogger(ROOT_LOGGER);
        publisher = new KinesisFirehoseAccumulatingPublisher<>(
                STREAM_NAME,
                SERIALIZER,
                LIMITS,
                TARGET_FLUSH_DELAY,
                firehose,
                metricPublisher,
                flushWorker,
                accumulatorFactory,
                ticker.asClock()
        );
    }

    @AfterEach
    void tearDown() {
        recordingAppender.detachFromRootLogger(ROOT_LOGGER);
    }

    @Test
    void testPeriodicFlush_nothingToFlush() {
        Runnable flushTask = assertScheduledFlushTask();
        flushTask.run();

        verifyNoInteractions(firehose, metricPublisher);
    }

    @Test
    void testPeriodicFlush_flushesOnlyAfterTargetDelayExceeded() {
        Runnable flushTask = assertScheduledFlushTask();

        publisher.put(List.of(HELLO));
        ticker.advance(TARGET_FLUSH_DELAY.minusMillis(1));
        flushTask.run();

        verifyNoInteractions(firehose, metricPublisher);

        ticker.advance(Duration.ofMillis(1));
        flushTask.run();

        verify(accumulator).flush();
    }

    @Test
    void testPeriodicFlush_putRecordBatchResetNextFlushTime() {
        when(firehose.putRecordBatch(PUT_RECORD_BATCH_REQUEST)).thenReturn(completedFuture(PUT_RECORD_BATCH_RESPONSE));

        Runnable flushTask = assertScheduledFlushTask();
        Consumer<PutRecordBatchRequest> putRecordBatch = assertCreatedAccumulator();

        publisher.put(List.of(HELLO));
        putRecordBatch.accept(PUT_RECORD_BATCH_REQUEST);

        // We run the flush task more than the configured delay after the last write, so we should normally flush but since we recently put records
        // in the stream (e.g. if the accumulator overflowed before the scheduled flush), which resets the wait time for a forced flush
        ticker.advance(TARGET_FLUSH_DELAY.plusMillis(1));
        flushTask.run();

        verify(accumulator, never()).flush();
    }

    @Test
    void testPutRecordBatch_publishesToFirehose() {
        when(firehose.putRecordBatch(PUT_RECORD_BATCH_REQUEST)).thenReturn(completedFuture(PUT_RECORD_BATCH_RESPONSE));
        mockPutRecordBatchMetrics();

        Consumer<PutRecordBatchRequest> putRecordBatch = assertCreatedAccumulator();
        putRecordBatch.accept(PUT_RECORD_BATCH_REQUEST);

        verify(firehose).putRecordBatch(PUT_RECORD_BATCH_REQUEST);
        verifyNoMoreInteractions(firehose);

        assertPublishedPutMetrics(true);
    }

    @Test
    void testPutRecordBatch_kinesisFailure() {
        SdkException e = SdkException.builder().message("You.shall.not.PAAAAAAASS!").build();
        when(firehose.putRecordBatch(PUT_RECORD_BATCH_REQUEST)).thenReturn(failedFuture(e));

        mockPutRecordBatchMetrics();

        Consumer<PutRecordBatchRequest> putRecordBatch = assertCreatedAccumulator();
        assertThatCode(() -> putRecordBatch.accept(PUT_RECORD_BATCH_REQUEST)).doesNotThrowAnyException();

        assertThat(recordingAppender.getEvents()).first().satisfies(event -> {
            String errorMsg = "Failed to publish batch to hello-stream";
            Exception expectedException = new CompletionException(new AwsDependencyException(errorMsg, e));

            // our equals exclude the throwable so we compare it separately
            assertThat(event).isEqualTo(SimpleLogEvent.forClass(KinesisFirehoseAccumulatingPublisher.class, Level.ERROR, errorMsg));
            assertThatThrowable(event.getThrown()).isLike(expectedException);
        });

        assertPublishedPutMetrics(false);
    }

    // Note that this class only mocks classes that have side-effects on the external world (e.g. Firehose, metrics), everything else happens as it would
    // in production despite this being a "unit" test.
    @Test
    @Timeout(value = 1, unit = MINUTES)
    void testParallelism_allItemsEventuallyPublished() throws InterruptedException {
        // ===== Test configuration =====
        // all the test's configuration is at the top to have all the knobs in one place
        ExecutorService clientThreads = Executors.newFixedThreadPool(5);
        ExecutorService putRecordBatchWorkers = Executors.newFixedThreadPool(10);

        int itemsBatchSize = 5;
        int itemsCount = 10_000;
        int itemByteSize = 800; // something on purpose so that there is not an even number of items in a record's max size
        int bytesToWrite = itemsCount * itemByteSize;
        int optimalNumberOfBatches = bytesToWrite / LIMITS.getMaxBatchByteSize();

        Duration maxPutRecordBatchLatency = Duration.ofMillis(200); // simulates Firehose latency from 0 to 200ms
        Duration putRecordBatchJitter = Duration.ofMillis(10); // spreads the writes to KinesisFirehoseAccumulatingPublisher
        // ===== End of test configuration =====

        AtomicLong bytesWritten = new AtomicLong();
        CountDownLatch remainingBytesToWrite = new CountDownLatch(bytesToWrite);

        Random rd = new Random(123456);
        mockPutRecordBatchRealistically(putRecordBatchWorkers, maxPutRecordBatchLatency, bytesWritten, remainingBytesToWrite, rd);

        publisher = new KinesisFirehoseAccumulatingPublisher<>(STREAM_NAME, SERIALIZER, LIMITS, TARGET_FLUSH_DELAY, firehose, MetricPublisher.NOOP);

        List<String> items = IntStream.range(0, itemsCount).mapToObj(i -> "a".repeat(itemByteSize)).toList();
        partition(items, itemsBatchSize).forEach(batch -> clientThreads.submit(() -> {
            randomSleep(rd, putRecordBatchJitter);
            publisher.put(batch);
        }));

        assertThat(remainingBytesToWrite.await(1, MINUTES))
                .as("All bytes have been written")
                .isTrue();

        Duration timeout = Duration.ofMinutes(1);
        shutdownAndAwaitTermination(clientThreads, timeout);
        shutdownAndAwaitTermination(putRecordBatchWorkers, timeout);

        // We can potentially make more requests than the optimal number if the flush task has caused some records to be flushed before they grow to their
        // maximum size (shouldn't happen in this test since we put items often). We also voluntarily made it so each record is a few hundreds of bytes short
        // of the maximum size. Finally, we could exceed the record count limit before hitting the max byte limit for a batch. Thus, we just check that we do
        // not get too far from the optimal number.
        verify(firehose, atLeast(optimalNumberOfBatches)).putRecordBatch(any(PutRecordBatchRequest.class));
        verify(firehose, atMost(2 * optimalNumberOfBatches)).putRecordBatch(any(PutRecordBatchRequest.class));

        // The countdown latch ensures we have written at least the right amount of bytes, but here we double-check we wrote exactly the right amount
        assertThat(bytesWritten.get()).isEqualTo(bytesToWrite);
    }

    private void mockPutRecordBatchRealistically(
            ExecutorService workers, Duration maxPutRecordBatchLatency, AtomicLong bytesWritten, CountDownLatch remainingBytesToWrite, Random rd) {

        when(firehose.putRecordBatch(any(PutRecordBatchRequest.class))).thenAnswer(invocation -> CompletableFuture.supplyAsync(() -> {
            randomSleep(rd, maxPutRecordBatchLatency);

            long recordSize = invocation.<PutRecordBatchRequest>getArgument(0).records().stream()
                    .mapToLong(record -> record.data().asByteArrayUnsafe().length)
                    .sum();

            bytesWritten.addAndGet(recordSize);
            for (int i = 0; i < recordSize; i++) remainingBytesToWrite.countDown();

            return PUT_RECORD_BATCH_RESPONSE;
        }, workers));
    }

    @Test
    @Timeout(value = 5, unit = SECONDS)
    void testParallelism_publishingToFirehoseIsNonBlocking() throws InterruptedException {
        ExecutorService putWorkers = Executors.newFixedThreadPool(10);
        Duration insaneLatency = Duration.ofHours(10);
        Duration regularLatency = Duration.ofMillis(15);

        KinesisFirehoseLimits limits = LIMITS.toBuilder()
                .maxRecordsPerBatch(1) // force an early flush to make the test easier to write
                .build();
        publisher = new KinesisFirehoseAccumulatingPublisher<>(STREAM_NAME, SERIALIZER, limits, TARGET_FLUSH_DELAY, firehose, MetricPublisher.NOOP);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicInteger completedPutsCount = new AtomicInteger();

        when(firehose.putRecordBatch(any(PutRecordBatchRequest.class)))
                .thenAnswer(__ -> mockPutRecordBatch(putWorkers, insaneLatency, completedPutsCount))
                .thenAnswer(__ -> mockPutRecordBatch(putWorkers, regularLatency, completedPutsCount).thenRun(countDownLatch::countDown));

        int maxRecordByteSize = limits.getMaxRecordByteSize();
        List<String> items = List.of(
                "a".repeat(maxRecordByteSize), // exactly the first record
                "b", // prefix of the second record
                "c".repeat(maxRecordByteSize - 1), // suffix of the second record
                "d" // left voer data
        );

        publisher.put(items.subList(0, 2)); // triggers a putRecordBatch which won't terminate within the timeout
        publisher.put(items.subList(2, 4)); // triggers another putRecordBatch which will terminate quickly

        assertThat(countDownLatch.await(5, SECONDS))
                .as("Second putRecordBatch call has ended")
                .isTrue();

        assertThat(completedPutsCount.get()).isEqualTo(1); // only completed the one with regular latency
        verify(firehose, times(2)).putRecordBatch(putRecordBatchRequestCaptor.capture());

        assertThat(putRecordBatchRequestCaptor.getAllValues())
                .extracting(request -> Validate.singleton(request.records()).data().asUtf8String())
                .containsExactlyInAnyOrder(items.get(0), items.get(1) + items.get(2));
    }

    private CompletableFuture<PutRecordBatchResponse> mockPutRecordBatch(ExecutorService putRecordWorkers, Duration latency, AtomicInteger completedPutsCount) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(latency);
            completedPutsCount.incrementAndGet();
            return PUT_RECORD_BATCH_RESPONSE;
        }, putRecordWorkers);
    }

    private Runnable assertScheduledFlushTask() {
        long period = TARGET_FLUSH_DELAY.toMillis();
        verify(flushWorker).scheduleAtFixedRate(flushTaskCaptor.capture(), eq(period), eq(period), eq(MILLISECONDS));
        return flushTaskCaptor.getValue();
    }

    private Consumer<PutRecordBatchRequest> assertCreatedAccumulator() {
        verify(accumulatorFactory).create(eq(STREAM_NAME), same(SERIALIZER), onBufferFlushCaptor.capture(), eq(LIMITS));
        return onBufferFlushCaptor.getValue();
    }

    private void mockPutRecordBatchMetrics() {
        doAnswer(invocation -> {
            invocation.<Consumer<Metrics>> getArgument(1).accept(metrics);
            return null;
        }).when(metricPublisher).publishMetrics(eq("KinesisFirehose.%s.PutRecordBatch".formatted(STREAM_NAME)), any());
    }

    @SneakyThrows
    private void assertPublishedPutMetrics(boolean success) {
        if (success) verify(metrics).addMetric("BatchByteSize", HELLO.length());
        verify(metrics).addCount("Failure", success ? 0 : 1);
    }

    @SneakyThrows
    private static void randomSleep(Random rd, Duration maximumSleep) {
        Thread.sleep(rd.nextInt((int) maximumSleep.toMillis()));
    }

    @SneakyThrows
    private static void sleep(Duration duration) {
        Thread.sleep(duration.toMillis());
    }
}
