package com.dici.aws.exception;

import com.dici.exceptions.ExceptionUtils.ThrowingRunnable;
import com.dici.exceptions.ExceptionUtils.ThrowingSupplier;
import software.amazon.awssdk.core.exception.SdkException;

import java.util.function.Supplier;

/// This class contains utilities to handle AWS exceptions in a consistent way
public class AwsErrorHandling {
    public static <E extends Exception> void decorate(Supplier<String> messageSupplier, ThrowingRunnable<E> runnable) throws E {
        decorate(messageSupplier, () -> {
            runnable.run();
            return null;
        });
    }

    /// AWS exceptions aren't always very informative, maybe on purpose for security reasons (for example, 404s almost never include the name of the resource
    /// that didn't exist) and it's often beneficial to rethrow their exceptions with a custom message.
    /// @param messageSupplier custom message for rethrown exceptions, passed as a supplier to only pay the cost of string construction if we need to
    public static <T, E extends Exception> T decorate(Supplier<String> messageSupplier, ThrowingSupplier<T, E> supplier) throws E {
        try {
            return supplier.get();
        } catch (SdkException e) {
            throw new AwsDependencyException(messageSupplier.get(), e);
        }
    }
}
