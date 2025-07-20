package com.dici.insights.auditing;

import com.dici.commons.Validate;

import java.util.List;
import java.util.Optional;

/// Interface allowing access to an [AuditData] instance without being able to mutate it.
public interface ReadOnlyAuditData<KEY extends AuditKey> {
    /// @param key key to query
    /// @param clazz expected supertype for the value to retrieve
    /// @throws ClassCastException if the value doesn't match the given type
    /// @throws IllegalArgumentException if the key had multiple values
    /// @return the unique value associated with a given key, or an empty [Optional] if there isn't any.
    <V> Optional<V> getUniqueOptional(KEY key, Class<V> clazz);

    /// @param key key to query
    /// @param clazz expected supertype for all values in the list
    /// @throws ClassCastException if the value doesn't match the given type
    /// @return the list of all values associated to the given key
    <V> List<V> get(KEY key, Class<V> clazz);

    /// @param key key to query
    /// @param clazz expected supertype for the value to retrieve
    /// @throws ClassCastException if the value doesn't match the given type
    /// @throws IllegalArgumentException if the key had no or multiple values
    /// @return the unique value associated with a given key
    default <V> V getUnique(KEY key, Class<V> clazz) {
        return Validate.isPresent(getUniqueOptional(key, clazz), "Could not find any value for key: %s", key);
    }
}
