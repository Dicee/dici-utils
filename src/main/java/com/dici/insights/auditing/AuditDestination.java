package com.dici.insights.auditing;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/// An interface that defines what to do with the temporary file output of the audit process.
public interface AuditDestination {
    /// Writes an output fileto the desired destination
    void write(String fileName, File file);

    /// Writes an audit artifact to a local file with the option of opening it afterwards.
    static AuditDestination local(File directory, boolean openOutputFile) {
        return new LocalFileAuditDestination(directory, openOutputFile);
    }

    /// Returns a partially random file name to prevent collisions, with the aim of producing
    /// unique but understandable file names.
    static String getCollisionSafeName(String prefix, String extension, Instant now) {
        int hashCode = (prefix + Math.random()).hashCode();
        String suffix = UUID.nameUUIDFromBytes(String.valueOf(hashCode).getBytes(UTF_8))
                .toString().substring(0, 5);
        return "%s-%s-%s-%s".formatted(prefix, now, suffix, extension);
    }
}
