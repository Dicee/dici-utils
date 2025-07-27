package com.dici.security;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.joining;

/// Use this class to implement toString methods that exclude sensitive information. It's only relevant for internal models,
/// for API models you have to use whatever the framework you're using allows, or implementing a custom display function. Note
/// that [org.apache.commons.lang3.builder.ReflectionToStringBuilder#toStringExclude(Object, String...)] can help as well,
/// but our version supports adding a few characters of the sensitive data. It can be useful for ensuring two ids are the same
/// without logging the full id.
public class SafeToStringBuilder {
    private static final String NULL_STR = "null";
    // voluntarily show a fixed number of starts to avoid revealing the actual length of the string
    private static final String STARS = "****";
    // sad, but apparently Jacoco modifies the bytecode to add this field in order to track coverage data per clss
    private static final Set<String> EXCLUDED_FIELDS = Set.of("$jacocoData");

    /// Serializes an object to a string using reflection, obfuscating the fields marked with [Obfuscated]. The method is not recursive,
    /// and relies on the correct implementation of [Object#toString()] on any sub-structure. Thus, nested structures containing sensitive
    /// data should each override [Object#toString()] using this methode.
    ///
    /// Finally, the method filters out the fields marked as excluded, and this only applies to the top-level object, not to any sub-structure.
    public static String toString(Object o) {
        if (o == null) return NULL_STR;

        Class<?> clazz = o.getClass();

        return Stream.of(clazz.getDeclaredFields())
                .filter(field -> !EXCLUDED_FIELDS.contains(field.getName()))
                .map(field -> getFieldNameAndValue(field, o))
                .collect(joining(", ", clazz.getSimpleName() + "(", ")"));
    }

    private static String getFieldNameAndValue(Field field, Object o) {
        // in case multiple annotations are present (which should be impossible as it's not repeatable), we select the one that shows the
        // least amount of data
        Optional<Integer> plainPrefixLength = Stream.of(field.getAnnotations())
                .filter(annotation -> annotation.annotationType().equals(Obfuscated.class))
                .map(annotation -> ((Obfuscated) annotation).plainPrefixLength())
                // negative is invalid, default to 0, which is the safest choice
                .map(length -> Math.max(0, length))
                .min(naturalOrder());

        return "%s=%s".formatted(field.getName(), safelyGetStringValue(field, o, plainPrefixLength));
    }

    private static Object safelyGetStringValue(Field field, Object o, Optional<Integer> plainPrefixLength) {
        try {
            field.setAccessible(true);

            Object value = field.get(o);
            if (value == null) return NULL_STR;

            // when the sensitive field is shorter than the plain prefix, we hide everything out of safety, otherwise the whole value would
            // be shown in plain
            String s = value.toString();
            return plainPrefixLength
                    .map(length -> length >= s.length() ? STARS : s.substring(0, length) + STARS)
                    .orElse(s);
        } catch (IllegalAccessException e) {
            return e.toString(); // safe fallback, we don't want to crash for a toString call
        }
    }
}
