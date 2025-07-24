package com.dici.time;

import org.junit.jupiter.api.Test;
import java.time.Duration;

import static com.dici.time.TimeUtils.humanReadableDuration;
import static com.dici.time.TimeUtils.logFriendlyDuration;
import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilsTest {
    @Test
    void testHumanReadableDuration_zeroDuration() {
        assertThat(humanReadableDuration(Duration.ZERO)).isEqualTo("0 milliseconds");
    }

    @Test
    void testHumanReadableDuration_milliseconds() {
        assertThat(humanReadableDuration(Duration.ofMillis(1))).isEqualTo("1 millisecond");
        assertThat(humanReadableDuration(Duration.ofMillis(500))).isEqualTo("500 milliseconds");
    }

    @Test
    void testHumanReadableDuration_seconds() {
        assertThat(humanReadableDuration(Duration.ofSeconds(1))).isEqualTo("1 second");
        assertThat(humanReadableDuration(Duration.ofSeconds(30))).isEqualTo("30 seconds");
    }

    @Test
    void testHumanReadableDuration_minutes() {
        assertThat(humanReadableDuration(Duration.ofMinutes(1))).isEqualTo("1 minute");
        assertThat(humanReadableDuration(Duration.ofMinutes(45))).isEqualTo("45 minutes");
    }

    @Test
    void testHumanReadableDuration_hours() {
        assertThat(humanReadableDuration(Duration.ofHours(1))).isEqualTo("1 hour");
        assertThat(humanReadableDuration(Duration.ofHours(12))).isEqualTo("12 hours");
    }

    @Test
    void testHumanReadableDuration_days() {
        assertThat(humanReadableDuration(Duration.ofDays(1))).isEqualTo("1 day");
        assertThat(humanReadableDuration(Duration.ofDays(25))).isEqualTo("25 days");
    }

    @Test
    void testHumanReadableDuration_months() {
        assertThat(humanReadableDuration(Duration.ofDays(30))).isEqualTo("1 month");
        assertThat(humanReadableDuration(Duration.ofDays(70))).isEqualTo("2 months and 10 days");
    }

    @Test
    void testHumanReadableDuration_years() {
        assertThat(humanReadableDuration(Duration.ofDays(365))).isEqualTo("1 year");
        assertThat(humanReadableDuration(Duration.ofDays(730))).isEqualTo("2 years");
    }

    @Test
    void testHumanReadableDuration_complex() {
        Duration complexDuration = Duration.ofDays(398) // 1 year, 1 month, 3 days
                .plusHours(5)
                .plusMinutes(20)
                .plusSeconds(55)
                .plusMillis(123);
        assertThat(humanReadableDuration(complexDuration))
                .isEqualTo("1 year, 1 month, 3 days, 5 hours, 20 minutes, 55 seconds and 123 milliseconds");
    }

    @Test
    void testLogFriendlyDuration() {
        Duration duration = Duration.ofMinutes(15).plusSeconds(30).plusMillis(123);
        assertThat(logFriendlyDuration(duration)).isEqualTo("930123 ms (or 15 minutes, 30 seconds and 123 milliseconds)");
    }
}