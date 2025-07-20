package com.dici.insights.auditing;

import com.dici.insights.reporting.InsightsDataReporter;

import java.util.Map;

/// Implement this interface to pass extra data to the [InsightsDataReporter] that is present for all requests of
/// a certain type. The main upside is to be sure that these values will be filled, as opposed to [AuditData] which
/// may or may not contain the fields it is expected to contain.
public interface AuditMetadata {
    /// The client for the current request
    String getClient();

    /// Returns a human-friendly description of the request's metadata. Mainly to avoid direct calls to [Object#toString()]
    /// as they are untraceable in a codebase.
    String describe();

    /// Returns a map containing the key-value pairs of all metadata not included in this interface (i.e. this excludes
    /// the client).
    ///
    /// Note: we strongly recommend the use of lowercase snake-case keys to ,ake the, Glue-friendly (all field
    /// names are converted to lowercase, which means CamelCase names become difficult to read).
    Map<String, String> asMap();
}
