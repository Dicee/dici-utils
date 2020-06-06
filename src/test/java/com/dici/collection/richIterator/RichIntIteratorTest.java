package com.dici.collection.richIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class RichIntIteratorTest {
    @Test
    void testRange() {
        assertThat(RichIntIterator.range(0, 3).toList(), equalTo(List.of(0, 1, 2)));
    }

    @Test
    void testRangeFailsIfEmptyRange() {
        assertThrows(IllegalArgumentException.class, () -> RichIntIterator.range(0, 0).toList());
    }

    @Test
    void testClosedRange() {
        assertThat(RichIntIterator.closedRange(0, 3).toList(), equalTo(List.of(0, 1, 2, 3)));
    }
}
