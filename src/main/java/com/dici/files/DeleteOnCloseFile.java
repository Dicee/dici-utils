package com.dici.files;

import com.dici.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import static com.dici.check.Check.notNull;

public class DeleteOnCloseFile implements Closeable {
    private final File file;

    public DeleteOnCloseFile(File file) {
        this.file = notNull(file);
    }

    @Override
    public void close() throws IOException {
        IOUtils.deleteQuietly(file);
    }

    public File getFile() {
        return file;
    }
}
