package com.dici.aws.kinesis;

import com.dici.commons.Validate;
import io.github.resilience4j.core.lang.Nullable;
import lombok.Builder;
import lombok.Value;

@Value
public final class KinesisFirehoseLimits {
    /// According to [the AWS documentation](http://docs.aws.amazon.com/firehose/latest/APIReference/API_Record.html), each record
    /// can hold up to 1000 kiB.
    private static final int MAX_RECORD_BYTE_SIZE = 1000 * 1024;
    private static final int MAX_RECORDS_PER_BATCH = 500;

    /// The [documentation](https://docs.aws.amazon.com/firehose/latest/APIReference/API_PutRecordBatch.html) is not very clear on whether
    /// the limit is in MiB or MB, but conservatively we will pick the smaller of the two
    private static final int MAX_BATCH_BYTE_SIZE = 4 * 1000 * 1000;

    public static KinesisFirehoseLimits defaults() {
        return KinesisFirehoseLimits.builder().build();
    }

    // for multi-records deaggregation there's a limit of 500 items per record, so we need to be able to enforce a limit
    private final int maxItemsPerRecord;
    private final int maxRecordByteSize;
    private final int maxRecordsPerBatch;
    private final int maxBatchByteSize;

    @Builder(toBuilder = true)
    private KinesisFirehoseLimits(
            @Nullable Integer maxItemsPerRecord,
            @Nullable Integer maxRecordByteSize,
            @Nullable Integer maxRecordsPerBatch,
            @Nullable Integer maxBatchByteSize
    ) {
        this.maxItemsPerRecord = notNullOrDefault(maxItemsPerRecord, Integer.MAX_VALUE, "The number of items per record");
        this.maxRecordByteSize = notNullOrDefault(maxRecordByteSize, MAX_RECORD_BYTE_SIZE, "The maximum byte size of a record");
        this.maxRecordsPerBatch = notNullOrDefault(maxRecordsPerBatch, MAX_RECORDS_PER_BATCH, "The maximum number of records per batch");
        this.maxBatchByteSize = notNullOrDefault(maxBatchByteSize, MAX_BATCH_BYTE_SIZE, "The byte size of a record batch");
    }

    private static int notNullOrDefault(Integer nullableInt, int defaultAndMaxValue, String description) {
        if (nullableInt != null) {
            Validate.isPositive(nullableInt);
            Validate.that(nullableInt <= defaultAndMaxValue, "%s must be less than or equal to %d but was: %d",
                    description, defaultAndMaxValue, nullableInt);
            return nullableInt;
        }
        return defaultAndMaxValue;
    }
}
