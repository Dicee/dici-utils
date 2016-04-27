package com.dici.files;

import com.dici.check.Check;
import com.dici.collection.richIterator.RichIterators;
import com.dici.system.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public final class FileUtils {
	private FileUtils() { }
	
	public static String getResourceAbsolutePath(String localPath, Class<?> clazz) {
		String path = clazz.getResource(localPath).getPath();
		return SystemProperties.isWindows() ? path.substring(1) : path;
	}
	
	@Deprecated
	public static Path getPathRelativeToClass(Class<?> clazz, String path) {
		return Paths.get(currentDirectory()).resolve(Paths.get(getPathToPackage(clazz) + path));
	}

	public static String getPathToPackage(Class<?> clazz) {
		String name = clazz.getCanonicalName();
		return name.substring(0,name.lastIndexOf('.') + 1).replace('.','/');
	}
	
	public static String currentDirectory() {
		String path = new File(".").getAbsolutePath();
		return path.substring(0,path.length() - 1);
	}
	
	public static String canonicalCurrentDirectory() { return toCanonicalPath(currentDirectory()); }
	
	public static void ensureExists(File directory) {
		if (!directory.exists()) {
			Check.isTrue(directory.mkdirs(), "Unable to create directory: " + directory);
		}
		Check.isTrue(directory.canWrite(), "Directory " + directory + " is not writable");
	}

	/**
	 * Transforms a potentially system-dependent path into its canonical representation
	 * @param path potentially system-dependent path
	 * @return canonical path corresponding to $code{path}
	 */
	public static String toCanonicalPath(String path) { return path.replace(File.separatorChar,'/'); }
	
	public static boolean hasExtension(String path, List<String> extensions) {
		return hasExtension(new File(path),extensions);
	}
	
	public static boolean hasExtension(File file, List<String> extensions) {
	    return RichIterators.fromCollection(extensions).map(FileUtils::formatExtension).exists(file.getName()::endsWith);
	}
	
	public static File toExtension(String path, String extension) {
		return toExtension(new File(path), extension);
	}
	
	public static File toExtension(File file, String extension) {
	    extension = formatExtension(extension);
		if (hasExtension(file,Arrays.asList(extension))) return file;
		
		String path = file.getAbsolutePath();
		int lastDot = path.lastIndexOf('.');
		return new File((lastDot < 0 ? path : path.substring(0,lastDot)) + extension);
	}
		
	private static String formatExtension(String extension) {
	    return !extension.startsWith(".") ? "." + extension : extension;
	}
	
	public static String toString(File f) throws IOException {
		return RichIterators.fromLines(f).mkString("\n");
    }
}
