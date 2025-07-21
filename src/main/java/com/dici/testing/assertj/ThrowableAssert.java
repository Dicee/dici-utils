package com.dici.testing.assertj;

import org.assertj.core.api.AbstractThrowableAssert;

public class ThrowableAssert extends AbstractThrowableAssert<ThrowableAssert, Throwable> {
    public ThrowableAssert(Throwable actual) {
        super(actual, ThrowableAssert.class);
    }

    /// Allows deep assertions of exceptions in a recursive manner for perfect test accuracy despite exceptions
    /// not defining equality properly
    public ThrowableAssert isLike(Throwable expected) {
        hasSameClassAs(expected);
        hasMessage(expected.getMessage());
        if (expected.getCause() != null) {
            new ThrowableAssert(actual.getCause()).isLike(expected.getCause());
        }
        return this;
    }
}
