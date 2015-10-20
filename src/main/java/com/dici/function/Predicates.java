package com.dici.function;

import static com.dici.check.Check.isPositive;

import com.dici.exceptions.ExceptionUtils.ThrowingPredicate;

public class Predicates {
    public static <X> ThrowingPredicate<X> countdownPredicate(int n) {
        isPositive(n);
        return new ThrowingPredicate<X>() {
            private int counter = n;
            @Override public boolean test(X x) { return counter-- > 0; }
        };
    }
}
