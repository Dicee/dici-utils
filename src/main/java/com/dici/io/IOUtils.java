package com.dici.io;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {
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
	
	public static void closeIfCloseable(Object o) {
		if      (o instanceof     Closeable) closeQuietly((    Closeable) o);
		else if (o instanceof AutoCloseable) closeQuietly((AutoCloseable) o);
	}
}
