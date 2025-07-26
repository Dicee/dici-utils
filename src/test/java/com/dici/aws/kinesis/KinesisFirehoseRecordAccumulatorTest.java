package com.dici.aws.kinesis;

import com.dici.json.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.Record;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.dici.testing.assertj.BetterAssertions.assertThatThrownBy;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@MockitoSettings
class KinesisFirehoseRecordAccumulatorTest {
    private static final String DELIVERY_STREAM_NAME = "test-stream";
    private static final Function<TestPojo, byte[]> SERIALIZER = KinesisFirehoseRecordAccumulatorTest::toJsonBytes;

    private static final TestPojo TEST_POJO_1 = new TestPojo("pojo1", 123);
    private static final TestPojo TEST_POJO_2 = new TestPojo("pojo2", 345);
    private static final String SERIALIZED_TEST_POJO_1 = new String(toJsonBytes(TEST_POJO_1));
    private static final String SERIALIZED_TEST_POJO_2 = new String(toJsonBytes(TEST_POJO_2));

    private static final KinesisFirehoseLimits DEFAULT_LIMITS;
    static {
        int testRecordByteSize = SERIALIZED_TEST_POJO_1.getBytes(UTF_8).length;
        DEFAULT_LIMITS = KinesisFirehoseLimits.builder()
                .maxItemsPerRecord(2)
                .maxRecordByteSize(testRecordByteSize * 3)
                .maxRecordsPerBatch(2)
                .maxBatchByteSize(testRecordByteSize * 5)
                .build();
    }

    @Mock private Consumer<PutRecordBatchRequest> onFlushBuffer;
    @Captor private ArgumentCaptor<PutRecordBatchRequest> batchRequestCaptor;

    private KinesisFirehoseRecordAccumulator<TestPojo> accumulator;

    @BeforeEach
    void setUp() {
        accumulator = newAccumulator(DEFAULT_LIMITS);
    }

    @Test
    void testPut_itemTooLarge() {
        KinesisFirehoseLimits limits = KinesisFirehoseLimits.builder().maxRecordByteSize(10).build();
        assertThatThrownBy(() -> newAccumulator(limits).put(TEST_POJO_1))
                .isLike(new IllegalArgumentException("This item alone weighs 29 bytes in serialized form, which exceeds the max record size of 10"));
    }

    @Test
    void testPut_overflow_noOverflow() {
        accumulator.put(TEST_POJO_1);
        verifyNoInteractions(onFlushBuffer);

        accumulator.flush();

        verify(onFlushBuffer).accept(newPutRecordBatchRequest(SERIALIZED_TEST_POJO_1));
        verifyNoMoreInteractions(onFlushBuffer);
    }

    @Test
    void testPut_overflow_itemCountOverflow() {
        accumulator.put(TEST_POJO_1);
        accumulator.put(TEST_POJO_2);
        accumulator.put(TEST_POJO_1); // This should trigger the creation of a new Kinesis record

        verifyNoInteractions(onFlushBuffer);

        accumulator.flush();

        verify(onFlushBuffer).accept(newPutRecordBatchRequest(
                SERIALIZED_TEST_POJO_1 + SERIALIZED_TEST_POJO_2,
                SERIALIZED_TEST_POJO_1
        ));
        verifyNoMoreInteractions(onFlushBuffer);
    }

    @Test
    void testPut_overflow_recordSizeOverflow() {
        accumulator = newAccumulator(DEFAULT_LIMITS.toBuilder()
                .maxItemsPerRecord(Integer.MAX_VALUE) // effectively disable this limit to hit the record byte size limit instead
                .build()
        );

        accumulator.put(TEST_POJO_1);
        accumulator.put(TEST_POJO_2);
        accumulator.put(TEST_POJO_2);
        accumulator.put(TEST_POJO_1); // This should trigger a new record due to size

        verifyNoInteractions(onFlushBuffer);

        accumulator.flush();

        verify(onFlushBuffer).accept(newPutRecordBatchRequest(
                SERIALIZED_TEST_POJO_1 + SERIALIZED_TEST_POJO_2 + SERIALIZED_TEST_POJO_2,
                        SERIALIZED_TEST_POJO_1
        ));
        verifyNoMoreInteractions(onFlushBuffer);
    }

    @Test
    void testPut_overflow_recordCountOverflow() {
        accumulator.put(TEST_POJO_1);
        accumulator.put(TEST_POJO_2);
        accumulator.put(TEST_POJO_1);
        verifyNoInteractions(onFlushBuffer);

        accumulator.put(TEST_POJO_1); // causes the creation of a second record, which should trigger a flush

        verify(onFlushBuffer).accept(newPutRecordBatchRequest(
                SERIALIZED_TEST_POJO_1 + SERIALIZED_TEST_POJO_2,
                SERIALIZED_TEST_POJO_1 + SERIALIZED_TEST_POJO_1
        ));

        accumulator.flush();
        verifyNoMoreInteractions(onFlushBuffer); // should have flushed automatically so not expecting anything more here
    }

    @Test
    void testPut_overflow_batchByteSizeOverflow() {
        accumulator = newAccumulator(DEFAULT_LIMITS.toBuilder()
                .maxRecordsPerBatch(500) // effectively disable this limit to hit the batch byte size limit first
                .build()
        );

        accumulator.put(TEST_POJO_1);
        accumulator.put(TEST_POJO_2);
        accumulator.put(TEST_POJO_1);
        accumulator.put(TEST_POJO_1);
        verifyNoInteractions(onFlushBuffer);

        // causes a batch size byte overflow, which should trigger a flush even if the last record is not maxed out
        accumulator.put(TEST_POJO_2);

        verify(onFlushBuffer).accept(newPutRecordBatchRequest(
                SERIALIZED_TEST_POJO_1 + SERIALIZED_TEST_POJO_2,
                SERIALIZED_TEST_POJO_1 + SERIALIZED_TEST_POJO_1
        ));

        accumulator.flush();

        verify(onFlushBuffer).accept(newPutRecordBatchRequest(SERIALIZED_TEST_POJO_2));
        verifyNoMoreInteractions(onFlushBuffer);
    }

    private KinesisFirehoseRecordAccumulator<TestPojo> newAccumulator(KinesisFirehoseLimits limits) {
        return new KinesisFirehoseRecordAccumulator<>(DELIVERY_STREAM_NAME, SERIALIZER, onFlushBuffer, limits);
    }

    private static PutRecordBatchRequest newPutRecordBatchRequest(String... recordPayloads) {
        return PutRecordBatchRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME)
                .records(Stream.of(recordPayloads).map(KinesisFirehoseRecordAccumulatorTest::newKinesisRecord).toList())
                .build();
    }

    private static Record newKinesisRecord(String data) {
        return Record.builder().data(SdkBytes.fromUtf8String(data)).build();
    }

    private static byte[] toJsonBytes(TestPojo record) {
        return (Json.DEFAULT.toJsonString(record) + "\n").getBytes(UTF_8);
    }

    private record TestPojo(String name, int value) {}
}