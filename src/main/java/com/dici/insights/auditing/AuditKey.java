package com.dici.insights.auditing;

/// Implement this interface with an enum type to define a set of valid audit keys for a given workflow. See it as a set of keys in
/// a JSON schema, along with some complex type information.
public interface AuditKey {
    /// @return a common supertype for all valid values for this key
    Class<?> getValidSuperclass();
}
