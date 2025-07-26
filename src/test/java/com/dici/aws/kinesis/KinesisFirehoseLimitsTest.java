package com.dici.aws.kinesis;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.dici.testing.assertj.BetterAssertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class KinesisFirehoseLimitsTest {
    private static final int MAX_ITEMS_PER_RECORD = Integer.MAX_VALUE;
    private static final int MAX_RECORD_BYTE_SIZE = 1000 * 1024;
    private static final int MAX_RECORDS_PER_BATCH = 500;
    private static final int MAX_BATCH_BYTE_SIZE = 4 * 1000 * 1000;

    @Test
    void testDefaults() {
        KinesisFirehoseLimits limits = KinesisFirehoseLimits.defaults();
        assertThat(limits.getMaxItemsPerRecord()).isEqualTo(MAX_ITEMS_PER_RECORD);
        assertThat(limits.getMaxRecordByteSize()).isEqualTo(MAX_RECORD_BYTE_SIZE);
        assertThat(limits.getMaxRecordsPerBatch()).isEqualTo(MAX_RECORDS_PER_BATCH);
        assertThat(limits.getMaxBatchByteSize()).isEqualTo(MAX_BATCH_BYTE_SIZE);
    }

    @Test
    void testToBuilder() {
        KinesisFirehoseLimits original = KinesisFirehoseLimits.builder()
                .maxItemsPerRecord(10)
                .maxRecordByteSize(100)
                .maxRecordsPerBatch(5)
                .maxBatchByteSize(MAX_RECORDS_PER_BATCH)
                .build();
        KinesisFirehoseLimits modified = original.toBuilder()
                .maxItemsPerRecord(20)
                .build();

        assertThat(modified.getMaxItemsPerRecord()).isEqualTo(20);
        assertThat(modified.getMaxRecordByteSize()).isEqualTo(100);
        assertThat(modified.getMaxRecordsPerBatch()).isEqualTo(5);
        assertThat(modified.getMaxBatchByteSize()).isEqualTo(MAX_RECORDS_PER_BATCH);
    }

    @Nested
    class MaxItemsPerRecord {
        @Test
        void testBuilder_customValue_WithinLimit() {
            KinesisFirehoseLimits limits = KinesisFirehoseLimits.builder().maxItemsPerRecord(100).build();
            assertThat(limits.getMaxItemsPerRecord()).isEqualTo(100);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void testBuilder_notPositive(int invalidValue) {
            assertThatThrownBy(() -> KinesisFirehoseLimits.builder().maxItemsPerRecord(invalidValue).build())
                    .isLike(new IllegalArgumentException("Expected a positive value but was: " + invalidValue));
        }
    }

    @Nested
    class MaxRecordByteSize {
        @Test
        void testBuilder_belowLimit() {
            KinesisFirehoseLimits limits = KinesisFirehoseLimits.builder().maxRecordByteSize(1024).build();
            assertThat(limits.getMaxRecordByteSize()).isEqualTo(1024);
        }

        @Test
        void testBuilder_atLimit() {
            int limit = MAX_RECORD_BYTE_SIZE;
            KinesisFirehoseLimits limits = KinesisFirehoseLimits.builder().maxRecordByteSize(limit).build();
            assertThat(limits.getMaxRecordByteSize()).isEqualTo(limit);
        }

            @ParameterizedTest
            @ValueSource(ints = {0, -1, -100})
            void testBuilder_notPositive(int invalidValue) {
                assertThatThrownBy(() -> KinesisFirehoseLimits.builder().maxRecordByteSize(invalidValue).build())
                        .isLike(new IllegalArgumentException("Expected a positive value but was: " + invalidValue));
            }

        @Test
        void testBuilder_aboveLimit() {
            assertThatThrownBy(() -> KinesisFirehoseLimits.builder().maxRecordByteSize(MAX_RECORD_BYTE_SIZE + 1).build())
                    .isLike(new IllegalArgumentException("The maximum byte size of a record must be less than or equal to 1024000 but was: 1024001"));
        }
    }

    @Nested
    class MaxRecordsPerBatch {
        @Test
        void testBuilder_belowLimit() {
            KinesisFirehoseLimits limits = KinesisFirehoseLimits.builder().maxRecordsPerBatch(100).build();
            assertThat(limits.getMaxRecordsPerBatch()).isEqualTo(100);
        }

        @Test
        void testBuilder_atLimit() {
            int limit = MAX_RECORDS_PER_BATCH;
            KinesisFirehoseLimits limits = KinesisFirehoseLimits.builder().maxRecordsPerBatch(limit).build();
            assertThat(limits.getMaxRecordsPerBatch()).isEqualTo(limit);
        }

        @Test
        void testBuilder_aboveLimit() {
            assertThatThrownBy(() -> KinesisFirehoseLimits.builder().maxRecordsPerBatch(501).build())
                    .isLike(new IllegalArgumentException("The maximum number of records per batch must be less than or equal to 500 but was: 501"));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void testBuilder_notPositive(int invalidValue) {
            assertThatThrownBy(() -> KinesisFirehoseLimits.builder().maxRecordsPerBatch(invalidValue).build())
                    .isLike(new IllegalArgumentException("Expected a positive value but was: " + invalidValue));
        }
    }

    @Nested
    class MaxBatchByteSize {
        @Test
        void testBuilder_belowLimit() {
            KinesisFirehoseLimits limits = KinesisFirehoseLimits.builder().maxBatchByteSize(1024).build();
            assertThat(limits.getMaxBatchByteSize()).isEqualTo(1024);
        }

        @Test
        void testBuilder_atLimit() {
            int limit = MAX_BATCH_BYTE_SIZE;
            KinesisFirehoseLimits limits = KinesisFirehoseLimits.builder().maxBatchByteSize(limit).build();
            assertThat(limits.getMaxBatchByteSize()).isEqualTo(limit);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void testBuilder_notPositive(int invalidValue) {
            assertThatThrownBy(() -> KinesisFirehoseLimits.builder().maxBatchByteSize(invalidValue).build())
                    .isLike(new IllegalArgumentException("Expected a positive value but was: " + invalidValue));
        }

        @Test
        void testBuilder_aboveLimit() {
            assertThatThrownBy(() -> KinesisFirehoseLimits.builder().maxBatchByteSize(MAX_BATCH_BYTE_SIZE + 1).build())
                    .isLike(new IllegalArgumentException("The byte size of a record batch must be less than or equal to 4000000 but was: 4000001"));
        }
    }
}