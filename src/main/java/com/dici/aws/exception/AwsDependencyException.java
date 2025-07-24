package com.dici.aws.exception;

import software.amazon.awssdk.core.exception.SdkException;

/// Exception class introduced to consistently wrap all AWS SDK exceptions. This allows adding more detailed messages than the default messages provided
/// by AWS, while retaining strong typing to be enable fine-grained error handling by callers.
public class AwsDependencyException extends RuntimeException {
    private final SdkException cause;

    public AwsDependencyException(String message, SdkException cause) {
        super(message, cause);
        this.cause = cause;
    }

    @Override
    public synchronized SdkException getCause() {
        return cause;
    }
}
