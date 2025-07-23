package com.dici.service.exception;

import lombok.Getter;
import org.apache.commons.lang3.Validate;

/// Exception thrown when there is a failure while calling a dependency of our service.
/// Wrapping dependency exceptions in such a manner helps standardizing logging, metric
/// publishing and error handling.
@Getter
public class DependencyException extends RuntimeException {
    private final String service;
    private final String operation;

    public static DependencyException withAdditionalDetails(String service, String operation, String additionalDetails, Throwable cause) {
        return new DependencyException(service, operation, formatErrorMessage(service, operation, additionalDetails), cause);
    }

    public DependencyException(String service, String operation, Throwable cause) {
        this(service, operation, formatErrorMessage(service, operation, cause), cause);
    }

    private DependencyException(String service, String operation, String message, Throwable cause) {
        super(message, cause);
        this.service = Validate.notBlank(service);
        this.operation = Validate.notBlank(operation);
    }

    private static String formatErrorMessage(String service, String operation, Throwable cause) {
        // Throwable#toString displays nicely enough by default, e.g. "java.lang.RuntimeException: custom message"
        return formatErrorMessage(service, operation, cause.toString());
    }

    private static String formatErrorMessage(String service, String operation, String errorDetails) {
        return "Error while calling dependency %s.%s due to: %s".formatted(service, operation, errorDetails);
    }
}
