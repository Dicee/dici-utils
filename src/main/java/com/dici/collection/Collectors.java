package com.dici.collection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class Collectors {
    /// Collects a [Stream] to a collection of elements computed from two consecutive elements in the [Stream]. Example:
    /// {@code Stream.of(1, 2, 5).collect(scan((prev, next) -> String.valueOf(next - prev)), toUnmodifiableLis())} will
    /// yield a list with value {@code ["1", "3"]}
    public static <T, O, R> Collector<T, List<T>, R> scan(BiFunction<T, T, O> op, Collector<O, ?, R> collector) {
        return new Collector<>() {
            @Override
            public Supplier<List<T>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<T>, T> accumulator() {
                return List::add;
            }

            @Override
            public BinaryOperator<List<T>> combiner() {
                return (left, right) -> {
                    left.addAll(right);
                    return left;
                };
            }

            @Override
            public Function<List<T>, R> finisher() {
                return list -> {
                    var output = Stream.<O>builder();
                    for (int i = 0; i < list.size() - 1; i++) {
                        output.add(op.apply(list.get(i), list.get(i + 1)));
                    }
                    return output.build().collect(collector);
                };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return EnumSet.noneOf(Collector.Characteristics.class);
            }
        };
    }
}
