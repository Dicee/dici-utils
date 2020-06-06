package com.dici.collection.richIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class RichLongIteratorTest {
	@Test
	void testRange() {
		assertThat(RichLongIterator.range(0,3).toList(), Matchers.equalTo(Arrays.asList(0L,1L,2L)));
	}
	
	@Test
	void testRangeFailsIfEmptyRange() {
		assertThrows(IllegalArgumentException.class, () -> RichLongIterator.range(0, 0));
	}
	
	@Test
	void testClosedRange() {
		assertThat(RichLongIterator.closedRange(0,3).toList(), Matchers.equalTo(Arrays.asList(0L,1L,2L,3L)));
	}
}
