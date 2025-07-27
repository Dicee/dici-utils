package com.dici.commons;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/// Helpers for argument validation
public class Validate {
    public static int isPositive(int n) {
        Validate.that(n > 0, "Expected a positive value but was: %s", n);
        return n;
    }

    public static <T> void isNull(T t) {
        isNull(t, "Expected value to be null but was %s", t);
    }

    public static <T> void isNull(T t, String errorMessageFormat, Object... errorMessageArgs) {
        Validate.that(t == null, IllegalArgumentException::new, errorMessageFormat, errorMessageArgs);
    }

    // only keeping this signature because simpler notBlank signatures are available in Apache Commons
    public static <E extends RuntimeException> String notBlank(
            String s,
            Function<String, E> exceptionFactory,
            String errorMessageTemplate,
            Object... errorMessageArgs
    ) {
        Validate.that(s != null && !s.isBlank(), exceptionFactory, errorMessageTemplate, errorMessageArgs);
        return s;
    }

    public static <V> V isPresent(
            Optional<V> optional,
            String errorMessageTemplate,
            Object... errorMessageArgs
    ) {
        Validate.that(optional.isPresent(), errorMessageTemplate, errorMessageArgs);
        return optional.get();
    }

    public static <T, E extends RuntimeException> T equalTo(
            T actual,
            T expected,
            Function<String, E> exceptionFactory,
            String errorMessageTemplate,
            Object... errorMessageArgs
    ) {
        Validate.that(Objects.equals(actual, expected), exceptionFactory, errorMessageTemplate, errorMessageArgs);
        return actual;
    }

    public static <T> T singleton(Collection<T> collection) {
        return singleton(collection, "Expected a singleton but collection has %s elements", collection.size());
    }

    public static <T> T singleton(Collection<T> collection, String errorMessageTemplate, Object... errorMessageArgs) {
        return singleton(collection, IllegalArgumentException::new, errorMessageTemplate, errorMessageArgs);
    }

    public static <T, E extends RuntimeException> T singleton(
            Collection<T> collection,
            Function<String, E> exceptionFactory,
            String errorMessageTemplate,
            Object... errorMessageArgs
    ) {
        Validate.that(collection.size() == 1, exceptionFactory, errorMessageTemplate, errorMessageArgs);
        return collection.iterator().next();
    }

    public static void that(
            boolean condition,
            String errorMessageTemplate,
            Object... errorMessageArgs
    ) {
        that(condition, IllegalArgumentException::new, errorMessageTemplate, errorMessageArgs);
    }

    public static <E extends RuntimeException> void that(
            boolean condition,
            Function<String, E> exceptionFactory,
            String errorMessageTemplate,
            Object... errorMessageArgs
    ) {
        if (!condition) throw exceptionFactory.apply(errorMessageTemplate.formatted(errorMessageArgs));
    }

    public static <E extends RuntimeException> void that(boolean condition, Supplier<E> exceptionSupplier) {
        if (!condition) throw exceptionSupplier.get();
    }
}
