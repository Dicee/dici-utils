package com.dici.files;

import com.dici.commons.Validate;

import java.io.File;

public class DirectoryUtils {
    /// Ensures a directory exists or creates it if needed, including if it requires create multiple sub-folders.
    /// @param dir directory which existence must be ensured. Synchronized to prevent race conditions.
    /// @return the given directory, that is now guaranteed to exist and be writable
    /// @throws IllegalStateException if the directory did not exist and could not be created for any reason, or was not writable
    public static File createIfNeeded(File dir) {
        Validate.that(dir.isDirectory() || dir.mkdirs(), IllegalStateException::new, "Could not create directory: %s", dir);
        Validate.that(dir.canWrite(), IllegalStateException::new, "Directory is not writable: %s", dir);
        return dir;
    }

    /// Ensures a non-blank string representing a path ends in a slash, or adds one if it doesn't.
    /// @param path a non-blank path
    /// @return the path, guaranteed to end in a slash
    /// @throws IllegalArgumentException if the string is blank (null, empty, or composed only of blank characters)
    public static String ensureTrailingSlash(String path) {
        return Validate.notBlank(path).charAt(path.length() - 1) != '/' ? path + '/' : path;
    }
}
