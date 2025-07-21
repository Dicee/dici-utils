package com.dici.exceptions;

import com.dici.commons.Validate;

import javax.xml.crypto.dsig.spec.HMACParameterSpec;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// This class allows handling the typical case in which one iterates over a list of things to do
/// and wants to execute all of them before reporting individual errors. Essentially, it makes it
/// easier to collect the exceptions and throw them at the end (if any was caught), with neat formatting.
/// Finally, it allows setting a limit of exceptions to display. Subsequent exceptions will be ignored.
public class MultiException extends RuntimeException {
    private final int maxExceptions;
    private final Map<Exception, Instant> exceptions = new LinkedHashMap<>();

    private Exception lastException = null;
    private int count = 0;

    public static void main(String[] args) {
        MultiException multiException = new MultiException(2);
        test(multiException);
        multiException.add(new RuntimeException("oops"));
        multiException.throwIfNotEmpty();
    }

    public static void test(MultiException multiException) {
        multiException.add(new IllegalArgumentException("noooooooo"));
    }

    public MultiException(int maxExceptions) {
        this.maxExceptions = Validate.isPositive(maxExceptions);
    }

    public void addOrThrowIfFull(Exception e) {
        if (size() == maxExceptions) throw this;
        add(e);
    }

    public void add(Exception e) {
        if (exceptions.size() >= maxExceptions) return;
        count++;
        lastException = e;
        exceptions.put(e, Instant.now());
    }

    public void throwIfNotEmpty() {
        if (!isEmpty()) throw this;
    }

    @Override
    public void printStackTrace(PrintStream ps) {
        printStackTrace(new PrintWriter(ps));
        ps.flush();
    }

    /*
        Along with getMessage(), printStackTrace() will generate this type of stacktraces:

        Exception in thread "main"
        == Exception 1 of 2 ==
        java.lang.IllegalArgumentException: noooooooo
            at com.dici.exception.MultiException.test(MultiException.java:32)
            at com.dici.exception.MultiException.main(MultiException.java:26)

        == Exception 2 of 2 ==
        java.lang.RuntimeException: oops
            at com.dici.exception.MultiException.main(MultiException.java:27)
     */
    @Override
    public void printStackTrace(PrintWriter pw) {
        if (count > maxExceptions) {
            pw.println("\n== Warning: maximum number of exceptions (%d) exceeded ==".formatted(maxExceptions));
        }

        int i = 0;
        for (Map.Entry<Exception, Instant> entry : exceptions.entrySet()) {
            pw.println("\n== Exception %d of %d ==".formatted(++i, count));
            entry.getKey().printStackTrace(pw);
        }
        pw.flush();
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Exception e : exceptions.keySet()) {
            sb.append(++i).append('/').append(exceptions.size()).append(" - ");
            sb.append(e.getClass().getSimpleName());
            sb.append(": ").append(e.getMessage()).append('\n');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public synchronized Throwable getCause() {
        return count == 0 ? null : lastException;
    }

    public int size() {
        return exceptions.size();
    }

    public boolean isEmpty() {
        return exceptions.isEmpty();
    }

    public List<Exception> getExceptions() {
        return List.copyOf(exceptions.keySet());
    }
}
