package com.dici.testing.hamcrest.matchers;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.function.Function;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MatchedProperty<T> {
    public static <T, S> MatchedProperty<T> compareWith(
            T expected,
            String propertyName,
            Function<T, S> extractor,
            Function<S, Matcher<? super S>> newMatcher
    ) {
        return new MatchedProperty<>(propertyName, new TypeSafeMatcher<T>() {
            private final Matcher<? super S> matcher = newMatcher.apply(extractor.apply(expected));

            @Override
            public void describeTo(Description description) {
                matcher.describeTo(description);
            }

            @Override
            protected void describeMismatchSafely(T actual, Description mismatchDescription) {
                matcher.describeMismatch(extractor.apply(actual), mismatchDescription);
            }

            @Override
            protected boolean matchesSafely(T actual) {
                return matcher.matches(extractor.apply(actual));
            }
        });
    }

    @NonNull private final String name;
    @NonNull private final Matcher<T> matcher;
}
