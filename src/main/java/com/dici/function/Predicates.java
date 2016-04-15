package com.dici.function;

import static com.dici.check.Check.isGreaterOrEqual;
import static com.dici.check.Check.notNegative;

import com.dici.check.Check;
import com.dici.exceptions.ExceptionUtils.ThrowingPredicate;

public class Predicates {
    public static <X> ThrowingPredicate<X> countdownPredicate(int n) {
        return new ThrowingPredicate<X>() {
            private int counter = notNegative(n);
            @Override public boolean test(X x) { return counter-- > 0; }
        };
    }
}
