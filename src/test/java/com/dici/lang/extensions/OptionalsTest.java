package com.dici.lang.extensions;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OptionalsTest {
    @Test
    void testFirstPresent_single() {
        assertThat(Optionals.firstPresent(Optional.empty())).isEmpty();
        assertThat(Optionals.firstPresent(Optional.of(1))).hasValue(1);
    }

    @Test
    void testFirstPresent_allEmpty() {
        assertThat(Optionals.firstPresent(Optional.empty(), Optional.empty())).isEmpty();
    }

    @Test
    void testFirstPresent_firstIsPresent() {
        assertThat(Optionals.firstPresent(Optional.of(1), Optional.of(2))).hasValue(1);
    }

    @Test
    void testFirstPresent_secondIsPresent() {
        assertThat(Optionals.firstPresent(Optional.empty(), Optional.of(2))).hasValue(2);
    }

    @Test
    void testFirstPresent_lastIsPresent() {
        assertThat(Optionals.firstPresent(Optional.empty(), Optional.empty(), Optional.of(3))).hasValue(3);
    }

    @Test
    void testFirstPresent_withDifferentTypes() {
        Optional<Integer> intOpt = Optional.of(1);
        Optional<Double> doubleOpt = Optional.of(2.0);
        Optional<Number> result = Optionals.firstPresent(intOpt, doubleOpt);
        assertThat(result).hasValue(1);
    }

    @Test
    void testFirstPresent_withDifferentTypesAndEmpty() {
        Optional<Integer> intOpt = Optional.empty();
        Optional<Double> doubleOpt = Optional.of(2.0);
        Optional<Number> result = Optionals.firstPresent(intOpt, doubleOpt);
        assertThat(result).hasValue(2.0);
    }
}
