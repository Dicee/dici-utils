package com.dici.service.exception;

import lombok.*;

import java.util.List;
import java.util.function.Supplier;

import static com.dici.service.exception.ExceptionTranslation.PROPAGATE_MODELED_RUNTIME_EXCEPTION;

/// Custom configuration allowing to add custom behaviours around exception translation. Note that order matters,
/// translations should be added from the most to least specific as the first match will be used.
@Value
@RequiredArgsConstructor
@Builder(toBuilder = true)
public class ExceptionTranslationConfig {
    @NonNull private final ExceptionTranslation defaultTranslation;

    /// Exceptions that are part of the API's model, and thus should be propagated as is
    @Singular
    @NonNull private final List<Class<? extends RuntimeException>> modeledExceptions;

    @Singular
    @NonNull private final List<ExceptionTranslation> translations;

    /// Gets the result of a supplier, handling errors according to the configuration
    public <T> T getWithErrorHandling(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            throw findBestTranslation(t).exception();
        }
    }

    /// Traverses all custom translations and returns the first one that applies to the given [Throwable]. If none applies, the default translation
    /// (typically, an internal failure exception representing a 500 status code) is used.
    public ExceptionTranslation.Result findBestTranslation(Throwable t) {
        // If this is not a RuntimeException, it's either an Error (e.g. OOM) or a checked exception that violated compiler rules e.g. because of Lombok's
        // SneakyThrows. In both cases, it's definitely not an expected failure mode.
        if (!(t instanceof RuntimeException)) return defaultTranslation.translate(t);
        if (modeledExceptions.contains(t.getClass())) return PROPAGATE_MODELED_RUNTIME_EXCEPTION.translate(t);
        return translations.stream()
                .flatMap(translation -> translation.tryTranslate(t).stream())
                .findFirst()
                .orElse(defaultTranslation.translate(t));
    }
}
