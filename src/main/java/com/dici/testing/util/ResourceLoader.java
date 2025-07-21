package com.dici.testing.util;

import java.io.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class ResourceLoader {
    /// Loads a resources as a string for a given relative path from the given class
    public static String loadResource(Class<?> clazz, String path) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(loadResourceAsStream(clazz, path)))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + getResourceDescription(clazz, path), e);
        }
    }

    /// Loads a resources as an input stream for a given relative path from the given class
    public static InputStream loadResourceAsStream(Class<?> clazz, String path) {
        String resourceDescription = getResourceDescription(clazz, path);
        return requireNonNull(clazz.getResourceAsStream(path), "Could not find resource " + resourceDescription);
    }

    private static String getResourceDescription(Class<?> clazz, String path) {
        return "resource with path \"%s\" from class %s".formatted(path, clazz.getName());
    }
}
