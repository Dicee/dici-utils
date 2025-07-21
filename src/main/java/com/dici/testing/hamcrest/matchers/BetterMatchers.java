package com.dici.testing.hamcrest.matchers;

import org.hamcrest.Matcher;

import java.time.Duration;
import java.time.Instant;

import static com.dici.testing.hamcrest.matchers.MatchedProperty.compareWith;
import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class BetterMatchers {
    public static Matcher<Instant> closeTo(Instant expected, Duration epsilon) {
        return CompositeMatcher.<Instant> builder()
                .expected(expected)
                .matchedProperty(compareWith(expected, "min", identity(), instant -> greaterThanOrEqualTo(expected.minus(epsilon))))
                .matchedProperty(compareWith(expected, "max", identity(), instant -> lessThanOrEqualTo(expected.plus(epsilon))))
                .build();
    }

    public static Matcher<Duration> closeTo(Duration expected, Duration epsilon) {
        return CompositeMatcher.<Duration> builder()
                .expected(expected)
                .matchedProperty(compareWith(expected, "min", identity(), instant -> greaterThanOrEqualTo(expected.minus(epsilon))))
                .matchedProperty(compareWith(expected, "max", identity(), instant -> lessThanOrEqualTo(expected.plus(epsilon))))
                .build();
    }
}
