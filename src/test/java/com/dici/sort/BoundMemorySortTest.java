package com.dici.sort;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.dici.sort.BoundMemorySort;

import org.junit.jupiter.api.Test;

public class BoundMemorySortTest {
	@Test
	public void test() {
		BoundMemorySort<Integer> sorter = new BoundMemorySort<>(Integer.class,3);
		
		List<Integer> list = Arrays.asList(3,69,6,4,7,88,5,1,4,6,9);
		List<Integer> res = sorter.sort(list.iterator()).stream().collect(Collectors.toList());
		Collections.sort(list);
		assertThat(res,equalTo(list));
	}
}
