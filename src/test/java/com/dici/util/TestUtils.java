package com.dici.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestUtils {
    public static File tempFileWithContent(String content) throws IOException {
        File f = File.createTempFile("test-file", null);
        try (BufferedWriter bw = Files.newBufferedWriter(f.toPath())) { bw.write(content); }
        return f;
    }
}
