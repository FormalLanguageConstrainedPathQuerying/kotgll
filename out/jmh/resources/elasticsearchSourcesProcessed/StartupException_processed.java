/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.bootstrap;

import org.elasticsearch.common.inject.CreationException;
import org.elasticsearch.common.inject.spi.Message;

import java.io.PrintStream;
import java.util.Objects;

/**
 * A wrapper for exceptions occurring during startup.
 *
 * <p> The stacktrack of a startup exception may be truncated if it is from Guice,
 * which can have a large number of stack frames.
 */
public final class StartupException extends Exception {

    /** maximum length of a stacktrace, before we truncate it */
    static final int STACKTRACE_LIMIT = 30;
    /** all lines from this package are RLE-compressed */
    static final String GUICE_PACKAGE = "org.elasticsearch.common.inject";

    public StartupException(Throwable cause) {
        super(Objects.requireNonNull(cause));
    }

    /**
     * Prints a stacktrace for an exception to a print stream, possibly truncating.
     *
     * @param err The error stream to print the stacktrace to
     */
    @Override
    public void printStackTrace(PrintStream err) {
        Throwable originalCause = getCause();
        Throwable cause = originalCause;
        if (cause instanceof CreationException) {
            cause = getFirstGuiceCause((CreationException) cause);
        }

        String message = cause.toString();
        err.println(message);

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause != originalCause && (message.equals(cause.toString()) == false)) {
            err.println("Likely root cause: " + cause);
        }

        StackTraceElement[] stack = cause.getStackTrace();
        int linesWritten = 0;
        for (int i = 0; i < stack.length; i++) {
            if (linesWritten == STACKTRACE_LIMIT) {
                err.println("\t<<<truncated>>>");
                break;
            }
            String line = stack[i].toString();

            if (line.startsWith(GUICE_PACKAGE)) {
                while (i + 1 < stack.length && stack[i + 1].toString().startsWith(GUICE_PACKAGE)) {
                    i++;
                }
                err.println("\tat <<<guice>>>");
                linesWritten++;
                continue;
            }

            err.println("\tat " + line);
            linesWritten++;
        }
        if (originalCause instanceof CreationException == false) {
            final String basePath = System.getProperty("es.logs.base_path");
            if (basePath != null) {
                final String logPath = System.getProperty("es.logs.base_path")
                    + System.getProperty("file.separator")
                    + System.getProperty("es.logs.cluster_name")
                    + ".log";

                err.println("For complete error details, refer to the log at " + logPath);
            }
        }
    }

    /**
     * Returns first cause from a guice error (it can have multiple).
     */
    private static Throwable getFirstGuiceCause(CreationException guice) {
        for (Message message : guice.getErrorMessages()) {
            Throwable cause = message.getCause();
            if (cause != null) {
                return cause;
            }
        }
        return guice; 
    }

}
