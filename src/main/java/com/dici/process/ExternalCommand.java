package com.dici.process;

import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.dici.exceptions.ExceptionUtils.uncheckedRunnable;
import static java.nio.charset.StandardCharsets.UTF_8;

/// Utility class to run external commands within the JVM with satisfactory error handling and visibility into
/// stdout and stderr.
public class ExternalCommand {
    /// Runs an external command in a given working directory and returns its output as a list of lines. The command's
    /// stderr will be routed to the Java process' stderr.
    /// @throws UncheckedExecutionException if an error occurred during execution or the return code was not 0
    public static List<String> getResult(File directory, String... command) {
        try {
            var result = Stream.<String>builder();
            executeCommandUnsafe(directory, result::add, command);
            return result.build().toList();
        } catch (UncheckedExecutionException e) {
            throw e;
        } catch (Exception e) {
            String message = "An error occurred while running command \"%s\". Check error logs.";
            throw new UncheckedExecutionException(message.formatted(getFullCommand(command)), e);
        }
    }

    private static void executeCommandUnsafe(File directory, Consumer<String> outputConsumer, String... command) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(directory.getAbsoluteFile())
                .redirectErrorStream(false);

        Process process = pb.start();
        sinkStreamTo(process.getInputStream(), outputConsumer);
        sinkStreamTo(process.getErrorStream(), System.err::println);

        process.waitFor();
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String message = "Non-zero exit code %d for command %s".formatted(exitCode, getFullCommand(command));
            throw new UncheckedExecutionException(message, null);
        }
    }

    private static void sinkStreamTo(InputStream inputStream, Consumer<String> consumer) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
        reader.lines().onClose(uncheckedRunnable(reader::close)).forEach(consumer);
    }

    private static String getFullCommand(String... command) {
        return String.join(" ", command);
    }
}
