package com.dici.commons;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.dici.testing.assertj.BetterAssertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class ValidateTest {
    @Test
    void testSingleton_happyPath() {
        List<String> list = List.of("a");
        assertThat(Validate.singleton(list)).isEqualTo("a");
    }

    @Test
    void testSingleton_emptyCollection() {
        assertThatThrownBy(() -> Validate.singleton(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a singleton but collection has 0 elements");
    }

    @Test
    void testSingleton_multipleElements() {
        assertThatThrownBy(() -> Validate.singleton(List.of("a", "b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a singleton but collection has 2 elements");
    }

    @Test
    void testSingleton_customMessage() {
        assertThatThrownBy(() -> Validate.singleton(List.of(), "Custom message. Size was %s", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Custom message. Size was 0");
    }

    @Test
    void testSingleton_customException() {
        assertThatThrownBy(() -> Validate.singleton(List.of(), IllegalStateException::new, "Custom message. Size was %s", 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Custom message. Size was 0");
    }
}
