package com.dici.lang.extensions;

import com.google.common.collect.Streams;

import java.util.Optional;
import java.util.stream.Stream;

public final class Optionals {
    @SafeVarargs
    public static <T> Optional<T> firstPresent(Optional<? extends T> first, Optional<? extends T>... others) {
        return Streams.concat(Stream.of(first), Stream.of(others))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(o -> (T) o)
                .findFirst();
    }
}