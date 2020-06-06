package com.dici.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class IOUtils {
	public static void closeQuietly(Closeable... resources) {
		for (Closeable resource : resources) closeQuietly(resource);
	}
	
	public static void closeQuietly(Closeable resource) {
		if (resource != null)
			try { resource.close(); } catch (IOException e) { /* do nothing */ }
	}
	
	public static void closeQuietly(AutoCloseable resource) {
		if (resource != null)
			try { resource.close(); } catch (Exception e) { /* do nothing */ }
	}

	public static void deleteQuietly(File file) {
		try {
			Files.delete(file.toPath());
		} catch (Exception e) {
			log.warn("Could not delete file: " + file.getAbsolutePath(), e);
		}
	}
	
	public static void closeIfCloseable(Object o) {
		if      (o instanceof     Closeable) closeQuietly((    Closeable) o);
		else if (o instanceof AutoCloseable) closeQuietly((AutoCloseable) o);
	}
}
