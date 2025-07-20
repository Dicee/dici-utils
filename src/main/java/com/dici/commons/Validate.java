package com.dici.commons;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/// Helpers for argument validation
public class Validate {
    public static int isPositive(int n) {
        Validate.that(n > 0, "Expected a positive value but was: %s", n);
        return n;
    }

    public static String notBlank(String s) {
        Validate.that(s != null && !s.isBlank(), "Expected a non-blank string but was: %s", s);
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
