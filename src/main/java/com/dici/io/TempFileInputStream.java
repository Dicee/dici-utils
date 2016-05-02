package com.dici.io;

import java.io.*;

import static com.dici.check.Check.notNull;

public class TempFileInputStream extends FileInputStream {
    private final File tmp;

    public TempFileInputStream(String name) throws FileNotFoundException {
        this(new File(name));
    }

    public TempFileInputStream(File tmp) throws FileNotFoundException {
        super(tmp);
        this.tmp = notNull(tmp);
    }

    @Override
    public void close() throws IOException {
        super.close();
        IOUtils.deleteQuietly(tmp);
    }
}
