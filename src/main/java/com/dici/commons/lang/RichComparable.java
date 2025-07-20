package com.dici.commons.lang;

/// Rich interface for [Comparable] implementations
public interface RichComparable<T> extends Comparable<T> {
    default boolean isGreaterThan(T that) {
        return compareTo(that) > 0;
    }

    default boolean isGreaterOrEqualTo(T that) {
        return compareTo(that) >= 0;
    }

    default boolean isLessThan(T that) {
        return compareTo(that) < 0;
    }

    default boolean isLessOrEqualTo(T that) {
        return compareTo(that) <= 0;
    }
}
