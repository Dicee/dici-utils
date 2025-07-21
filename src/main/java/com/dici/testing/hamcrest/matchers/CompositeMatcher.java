package com.dici.testing.hamcrest.matchers;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.List;

/// A matcher that allows nicely composing a collection of matchers, each of which tests a specific property of a given object.
/// It allows creating custom matchers rather easily, to maintain deep assertions without paying a high cost for it in cases when
/// perfect equality is inconvenient to assert in tests.
@Builder
public class CompositeMatcher<T> extends TypeSafeMatcher<T> {
    @Singular
    @NonNull private final List<MatchedProperty<T>> matchedProperties;
    @NonNull private final T expected;

    @Override
    protected boolean matchesSafely(T actual) {
        return matchedProperties.stream().allMatch(property -> property.getMatcher().matches(actual));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("An instance of type %s with the following properties:".formatted(expected.getClass().getName()));
        for (MatchedProperty<T> property : matchedProperties) {
            description.appendText(getPropertyListHeader(property));
            description.appendDescriptionOf(property.getMatcher());
        }
    }

    @Override
    protected void describeMismatchSafely(T actual, Description mismatchDescription) {
        mismatchDescription.appendText("The following properties did not match:");
        matchedProperties.stream()
                .filter(property -> !property.getMatcher().matches(actual))
                .forEach(property -> {
                    mismatchDescription.appendText(getPropertyListHeader(property));
                    property.getMatcher().describeMismatch(actual, mismatchDescription);
                });
    }

    private static <T> String getPropertyListHeader(MatchedProperty<T> property) {
        return "\n\t\t- %s: ".formatted(property.getName());
    }
}
