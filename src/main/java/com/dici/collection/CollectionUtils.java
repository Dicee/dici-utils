package com.dici.collection;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CollectionUtils {
	public static <T, R> List<R> map(Collection<T> collection, Function<T, R> function) {
		return collection.stream().map(function).toList();
	}

	@SafeVarargs
	public static <T> ImmutableSet<T> concatToSet(Iterable<T>... iterables) {
		return Stream.of(iterables).flatMap(Streams::stream).collect(toImmutableSet());
	}

	@SafeVarargs
	public static <T> ImmutableList<T> concat(Iterable<? extends T>... iterables) {
		return Stream.of(iterables).flatMap(Streams::stream).collect(toImmutableList());
	}
}
