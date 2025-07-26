package com.dici.aws.kinesis;

import com.dici.commons.Validate;
import lombok.Getter;
import lombok.NonNull;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.Record;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/// The role of this class is to accumulate as many data items as possible in memory before sending them to Firehose, to optimize throughput.
/// The first level of optimization is to write multiple items per Firehose record, and batch records in a single request. This class performs
/// both optimizations while abiding by the various limitations Firehose imposes. It is not thread-safe, and thus it is the responsibility of
/// the caller to correctly use this class in a thread-safe context. Finally, this class is not meant to be exposed outside of this package,
/// precisely because it is not thread-safe on its own.
@NotThreadSafe
class KinesisFirehoseRecordAccumulator<T> {
    private final String deliveryStreamName;
    private final Function<T, byte[]> serializer;
    private final Consumer<PutRecordBatchRequest> onFlushBuffer;
    private final KinesisFirehoseLimits limits;

    private final ByteBuffer buffer;
    private final List<Record> records;
    private int totalByteSize = 0;
    private int itemsCount = 0;

    public KinesisFirehoseRecordAccumulator(String deliveryStreamName, Function<T, byte[]> serializer, Consumer<PutRecordBatchRequest> onFlushBuffer) {
        this(deliveryStreamName, serializer, onFlushBuffer, KinesisFirehoseLimits.defaults());
    }

    public KinesisFirehoseRecordAccumulator(
            @NonNull String deliveryStreamName,
            @NonNull Function<T, byte[]> serializer,
            @NonNull Consumer<PutRecordBatchRequest> onFlushBuffer,
            @NonNull KinesisFirehoseLimits limits
    ) {
        this.deliveryStreamName = deliveryStreamName;
        this.serializer = serializer;
        this.onFlushBuffer = onFlushBuffer;
        this.limits = limits;
        this.buffer = ByteBuffer.allocate(limits.getMaxRecordByteSize());
        this.records = new ArrayList<>();
    }

    /// Puts the given item in the record accumulator. This method may or may not result in an immediate call to Firehose, depending on the remaining space of the buffer,
    /// the current size of the record batch and the limits configured on this accumulator.
    public void put(T item) {
        byte[] bytes = toByteArray(item);

        if (totalByteSize + bytes.length >= limits.getMaxBatchByteSize()) flush();
        if (buffer.remaining() < bytes.length) createRecordAndResetBuffer();

        buffer.put(bytes);
        itemsCount++;
        totalByteSize += bytes.length;

        if (itemsCount == limits.getMaxItemsPerRecord()) createRecordAndResetBuffer();
    }

    private byte[] toByteArray(T item) {
        byte[] bytes = serializer.apply(item);
        int maxByteSize = limits.getMaxRecordByteSize();
        Validate.that(bytes.length <= maxByteSize,
                "This item alone weighs %d bytes in serialized form, which exceeds the max record size of %d".formatted(bytes.length, maxByteSize));
        return bytes;
    }

    private void createRecordAndResetBuffer() {
        buffer.flip();

        records.add(Record.builder().data(SdkBytes.fromByteBuffer(buffer)).build());
        buffer.clear();
        itemsCount = 0;

        if (records.size() == limits.getMaxRecordsPerBatch()) flush();
    }

    public void flush() {
        // very important because it prevents infinite recursion between createRecordAndResetBuffer() and flush()
        if (buffer.position() > 0) createRecordAndResetBuffer();

        // this condition is just to avoid empty Firehose requests but plays no role in the recursion
        if (!records.isEmpty()) {
            onFlushBuffer.accept(PutRecordBatchRequest.builder()
                    .deliveryStreamName(deliveryStreamName)
                    .records(records)
                    .build()
            );

            records.clear();
        }

        totalByteSize = 0;
    }
}
