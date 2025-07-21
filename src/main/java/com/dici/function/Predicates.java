package com.dici.function;

import com.dici.exceptions.ExceptionUtils.ThrowingPredicate;

import static com.dici.check.Check.notNegative;

public class Predicates {
    public static <X> ThrowingPredicate<X> countdownPredicate(int n) {
        return new ThrowingPredicate<X>() {
            private int counter = notNegative(n);
            @Override public boolean test(X x) { return counter-- > 0; }
        };
    }
}
