package com.dici.service.exception;

import com.dici.exceptions.ExceptionUtils;
import com.google.common.base.Predicates;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Level;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.dici.exceptions.ExceptionUtils.toRuntimeException;
import static java.util.stream.Collectors.joining;

/// Defines the translation from one exception type to another, typically a customer-facing one to respect a contract.
@RequiredArgsConstructor
public class ExceptionTranslation {
    public static final ExceptionTranslation PROPAGATE_AS_RUNTIME_EXCEPTION =
            rethrowAs(RuntimeException.class, (__, t) -> toRuntimeException(t), Level.ERROR);

    public static ExceptionTranslation rethrowAs(
            Class<? extends Throwable> matchedSuperclass, BiFunction<String, Throwable, RuntimeException> translate, Level logLevel) {

        return rethrowAs(subTypeOf(matchedSuperclass), translate, logLevel);
    }

    public static ExceptionTranslation rethrowAs(Predicate<Throwable> isApplicable, BiFunction<String, Throwable, RuntimeException> translate, Level logLevel) {
        return new ExceptionTranslation(
                isApplicable,
                // Throwable#toString displays nicely enough by default, e.g. "java.lang.RuntimeException: custom message"
                t -> translate.apply(t.toString(), t),
                ExceptionTranslation::getDefaultFailureMetricName,
                logLevel
        );
    }

    public static ExceptionTranslation handleDependencyFailure(
            Class<? extends Throwable> matchedSuperclass, BiFunction<String, Throwable, RuntimeException> translate, Level logLevel) {

        return handleDependencyFailure(subTypeOf(matchedSuperclass), translate, logLevel);
    }

    public static ExceptionTranslation handleDependencyFailure(Predicate<Throwable> isApplicable, BiFunction<String, Throwable, RuntimeException> translate, Level logLevel) {
        return new ExceptionTranslation(
                isApplicable,
                t -> translate.apply(t.getMessage(), t.getCause()),
                ExceptionTranslation::getDefaultFailureMetricName,
                logLevel
        );
    }

    /// Returns a predicate that determines whether a given type is a subtype of any of the provided super types. Similar to [Predicates#subtypeOf(Class)]
    /// but with better generics that meet our needs, a toString implementation and the ability to pass several types.
    public static <T> Predicate<T> subTypeOf(Class<? extends T>... superTypes) {
        Validate.notEmpty(superTypes);
        return new Predicate<>() {
            @Override
            public boolean test(T t) {
                return Stream.of(superTypes).anyMatch(superType -> superType.isInstance(t));
            }

            @Override
            public String toString() {
                return "subTypeOf" + Stream.of(superTypes).map(Class::getName).collect(joining(", ", "[", "]"));
            }
        };
    }

    @NonNull private final Predicate<Throwable> isApplicable;
    @NonNull private final Function<Throwable, RuntimeException> translateException;
    @NonNull private final Function<Throwable, String> getFailureMetricName;

    @Getter
    @NonNull private final Level logLevel;

    public Result translate(Throwable t) {
        return tryTranslate(t).orElseThrow(() -> new IllegalArgumentException(
                "%s does not match %s".formatted(t.getClass().getName(), isApplicable)));
    }

    public Optional<Result> tryTranslate(Throwable t) {
        if (!isApplicable.test(t)) return Optional.empty();

        RuntimeException translated = translateException.apply(t);
        String failureMetricName = getFailureMetricName.apply(t);
        return Optional.of(new Result(translated, logLevel, failureMetricName));
    }

    private static String getDefaultFailureMetricName(Throwable t) {
        String name = t.getClass().getSimpleName();
        if (t instanceof DependencyException e) return String.join(".", name, e.getService(), e.getOperation());
        return name;
    }

    public record Result(
            @NonNull RuntimeException exception,
            @NonNull Level logLevel,
            @NonNull String failureMetricName
    ){}
}
