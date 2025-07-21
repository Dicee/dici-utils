package com.dici.testing.time;

import com.google.common.base.Ticker;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/// A controllable [Ticker] for testing which can also be converted to a clock
@RequiredArgsConstructor
public class FakeTicker extends Ticker {
    @NonNull private final Instant initialTime;
    private final AtomicLong nanos = new AtomicLong();
    private final Clock clock = new Clock() {
        @Override
        public Instant instant() {
            return getInstant(FakeTicker.this.nanos.get());
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException("Cannot change zone with this fake clock");
        }
    };

    public FakeTicker() {
        this(Instant.EPOCH);
    }

    public Instant advance(Duration duration) {
        return advance(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    public Instant advance(long quantity, TimeUnit unit) {
        long nanosToAdd = nanos.addAndGet(unit.toNanos(quantity));
        return getInstant(nanosToAdd);
    }

    private Instant getInstant(long nanosToAdd) {
        return initialTime.plusNanos(nanosToAdd);
    }

    public Clock asClock() {
        return clock;
    }

    @Override
    public long read() {
        return nanos.get();
    }
}
