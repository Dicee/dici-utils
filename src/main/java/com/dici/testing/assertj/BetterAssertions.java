package com.dici.testing.assertj;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.catchThrowable;

public class BetterAssertions {
    public static ThrowableAssert assertThatThrownBy(ThrowingCallable callable) {
        return assertThatThrowable(catchThrowable(callable));
    }

    public static ThrowableAssert assertThatThrowable(Throwable t) {
        return new ThrowableAssert(t);
    }
}
