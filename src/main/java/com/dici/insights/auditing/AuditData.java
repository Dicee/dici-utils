package com.dici.insights.auditing;

import com.dici.commons.Validate;

import java.util.*;

import static java.util.Collections.synchronizedMap;

/// This class is a wrapper around a heterogeneous map. This should be used for storing metadata about a given workflow.
/// [AuditData] confers a little bit a type safety on top of the raw map, which is its only interest. Note that it is also
/// an append-only data structure, which helps to mitigate its mutable nature. There are many blind writers and a single reader.
///
/// @param <KEY> type of the keys. Typically should be an enumeration or at least a closed type with a definite number of subclasses
public class AuditData<KEY extends AuditKey> implements ReadOnlyAuditData<KEY> {
    public static <KEY extends AuditKey> AuditData<KEY> empty() {
        return new AuditData<>(Map.of());
    }

    private final Map<KEY, List<Object>> store;

    public AuditData(Map<KEY, List<Object>> store) {
        store.forEach((key, values) -> values.forEach(value -> checkType(key, value)));
        this.store = synchronizedMap(Map.copyOf(store));
    }

    @Override
    public <V> Optional<V> getUniqueOptional(KEY key, Class<V> clazz) {
        List<V> values = get(key, clazz);
        Validate.that(values.size() <= 1, "Expected a unique value for key %s but was %s", key, values);
        return values.isEmpty() ? Optional.empty() : Optional.of(values.getFirst());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> List<V> get(KEY key, Class<V> clazz) {
        List<Object> values = store.get(key);
        values.forEach(clazz::cast);
        return (List<V>) values;
    }

    public void putAll(KEY key, Collection<?> values) {
        values.forEach(value -> put(key, value));
    }

    public void put(KEY key, Object value) {
        checkType(key, value);
        store.computeIfAbsent(key, __ -> new ArrayList<>()).add(value);
    }

    private void checkType(KEY key, Object value) {
        Class<?> validSuperclass = key.getValidSuperclass();
        Validate.that(validSuperclass.isInstance(value), "Invalid value type %s for key %s. Was expecting %s instead.",
                value.getClass().getName(), key, validSuperclass.getName());
    }
}
