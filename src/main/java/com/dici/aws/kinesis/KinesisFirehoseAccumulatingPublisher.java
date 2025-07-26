package com.dici.aws.kinesis;

import com.dici.aws.exception.AwsErrorHandling;
import com.dici.service.metrics.MetricPublisher;
import com.dici.time.TimeUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.resilience4j.core.lang.Nullable;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.dici.time.TimeUtils.humanReadableDuration;

/// This class accumulates records for a certain period of time or up to a certain byte size, in order to optimize the TPS sent to Firehose while not delaying
/// the data too much when the debit is slow.
@Log4j2
public class KinesisFirehoseAccumulatingPublisher<T> {
    private static final String PUT_RECORD_BATCH = "PutRecordBatch";
    private static final String PERIODIC_FLUSH = "PeriodicFlush";

    private static final String FAILURE = "Failure";
    private static final String BATCH_BYTE_SIZE = "BatchByteSize";

    private final String deliveryStreamName;
    private final Duration targetFlushDelay;
    private final FirehoseAsyncClient firehose;
    private final MetricPublisher metricPublisher;
    private final Clock clock;

    private final KinesisFirehoseRecordAccumulator<T> recordAccumulator;
    @Nullable private Instant lastUnpublishedTime;

    public KinesisFirehoseAccumulatingPublisher(
            @NonNull String deliveryStreamName,
            @NonNull Function<T, byte[]> serializer,
            @NonNull KinesisFirehoseLimits limits,
            @NonNull Duration targetFlushDelay,
            @NonNull FirehoseAsyncClient firehose,
            @NonNull MetricPublisher metricPublisher
    ) {
        this(deliveryStreamName, serializer, limits, targetFlushDelay, firehose, metricPublisher,
                createFlushWorker(deliveryStreamName), KinesisFirehoseRecordAccumulator::new, Clock.systemUTC());
    }

    @VisibleForTesting
    KinesisFirehoseAccumulatingPublisher(
            @NonNull String deliveryStreamName,
            @NonNull Function<T, byte[]> serializer,
            @NonNull KinesisFirehoseLimits limits,
            @NonNull Duration targetFlushDelay,
            @NonNull FirehoseAsyncClient firehose,
            @NonNull MetricPublisher metricPublisher,
            @NonNull ScheduledExecutorService flushWorker,
            @NonNull KinesisFirehoseRecordAccumulatorFactory<T> accumulatorFactory,
            @NonNull Clock clock
    ) {
        this.deliveryStreamName = deliveryStreamName;
        this.targetFlushDelay = targetFlushDelay;
        this.firehose = firehose;
        this.metricPublisher = metricPublisher;
        this.recordAccumulator = accumulatorFactory.create(deliveryStreamName, serializer, this::putRecordBatch, limits);
        this.clock = clock;

        long period = targetFlushDelay.toMillis();
        flushWorker.scheduleAtFixedRate(this::flushIfNecessary, period, period, TimeUnit.MILLISECONDS);
    }

    /// Puts the given items in a record accumulator. This method may pr may not result in an immediate call to Firehose, depending
    public synchronized void put(Collection<T> items) {
        for (T item : items) recordAccumulator.put(item);

        // the time to put all the records on the accumulator should be negligible so we can update the state only once at the end
        if (lastUnpublishedTime == null) lastUnpublishedTime = clock.instant();
    }

    private synchronized void flushIfNecessary() {
        try {
            if (lastUnpublishedTime == null) return;

            Duration flushDelay = Duration.between(lastUnpublishedTime, clock.instant());
            if (flushDelay.compareTo(targetFlushDelay) >= 0) {
                log.info("Record accumulator for stream {} didn't fill up after {}. We will flush any buffered data so far to Firehose.",
                        deliveryStreamName, humanReadableDuration(targetFlushDelay));

                recordAccumulator.flush();
            }
        } catch (Throwable t) {
            // noop, because we have to catch everything if we don't want to interrupt the scheduled thread
            log.warn("Failed running periodic flush to Firehose for stream " + deliveryStreamName, t);
            metricPublisher.publishCount(getOperationName(deliveryStreamName, PERIODIC_FLUSH), FAILURE, 1);
        }
    }

    private void putRecordBatch(PutRecordBatchRequest request) {
        AwsErrorHandling.wrap(() -> "Failed to publish batch to " + request.deliveryStreamName(), firehose.putRecordBatch(request))
                .handle((response, t) -> {
                    publishPutRecordBatchMetrics(request, t);
                    return null;
                });

        lastUnpublishedTime = null;
    }

    private void publishPutRecordBatchMetrics(PutRecordBatchRequest request, Throwable t) {
        metricPublisher.publishMetrics(getOperationName(deliveryStreamName, PUT_RECORD_BATCH), metrics -> {
            // publishing a 0 allows getting a success percentage metric by using sample count vs sum
            boolean isFailure = t != null;
            metrics.addCount(FAILURE, isFailure ? 1 : 0);

            // we log the message of the cause because it's wrapped in a completion exception
            if (isFailure) log.error(t.getCause().getMessage(), t);
            else {
                metrics.addMetric(BATCH_BYTE_SIZE, request.records().stream()
                        .mapToLong(record -> record.data().asByteArrayUnsafe().length)
                        .sum()
                );
            }
        });
    }

    private static String getOperationName(String deliveryStreamName, String action) {
        return "KinesisFirehose.%s.%s".formatted(deliveryStreamName, action);
    }

    private static ScheduledExecutorService createFlushWorker(String deliveryStreamName) {
        return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
                .setNameFormat("KinesisFirehoseAccumulatingPublisher-" + deliveryStreamName + "-%d")
                .setDaemon(true)
                .build()
        );
    }

    @VisibleForTesting
    interface KinesisFirehoseRecordAccumulatorFactory<T> {
        KinesisFirehoseRecordAccumulator<T> create(
                String deliveryStreamName,
                Function<T, byte[]> serializer,
                Consumer<PutRecordBatchRequest> onFlushBuffer,
                KinesisFirehoseLimits limits
        );
    }
}
