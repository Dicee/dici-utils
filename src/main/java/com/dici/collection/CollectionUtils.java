package com.dici.collection;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CollectionUtils {
	private static final int UNKNOWN_CHARACTERISTICS = 0;

	@SafeVarargs
	public static <T> ImmutableSet<T> concatToSet(Iterable<T>... iterables) {
		return Stream.of(iterables).flatMap(Streams::stream).collect(toImmutableSet());
	}

	@SafeVarargs
	public static <T> ImmutableList<T> concat(Iterable<? extends T>... iterables) {
		return Stream.of(iterables).flatMap(Streams::stream).collect(toImmutableList());
	}
}
