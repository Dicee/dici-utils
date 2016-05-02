package com.dici.io;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class IOUtils {
	private static final Log LOG = LogFactory.getLog(IOUtils.class);

	private IOUtils() { }
	
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
			if (!file.delete()) LOG.warn("Could not delete file: " + file.getAbsolutePath());
		} catch (Exception e) {
			LOG.warn("Could not delete file: " + file.getAbsolutePath(), e);
		}
	}
	
	public static void closeIfCloseable(Object o) {
		if      (o instanceof     Closeable) closeQuietly((    Closeable) o);
		else if (o instanceof AutoCloseable) closeQuietly((AutoCloseable) o);
	}
}
