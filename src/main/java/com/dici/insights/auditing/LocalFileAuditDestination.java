package com.dici.insights.auditing;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import static com.dici.files.DirectoryUtils.createIfNeeded;
import static java.util.Objects.requireNonNull;

/// Writes an audit file to the local filesystem.
class LocalFileAuditDestination implements AuditDestination {
    private final File directory;
    private final boolean openOutputFile;

    public LocalFileAuditDestination(File directory, boolean openOutputFile) {
        this.directory = requireNonNull(directory);
        this.openOutputFile = openOutputFile;
    }

    @Override
    public void write(String fileName, File file) {
        try {
            File output = new File(createIfNeeded(directory), fileName);
            Files.copy(file.toPath(), output.toPath());
            if (openOutputFile) Desktop.getDesktop().browse(output.toURI());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
