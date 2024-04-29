/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
   @bug 8313739
   @summary Verify that ZipOutputStream closes the wrapped stream even after failed writes
   @run junit CloseWrappedStream
   */

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class CloseWrappedStream {

    /**
     * Verify that closing a ZipOutputStream closes the wrapped output stream,
     * also when the wrapped stream throws while remaining data is flushed
     */
    @Test
    public void exceptionDuringFinish() {
        WrappedOutputStream wrappedStream = new WrappedOutputStream();

        IOException exception = assertThrows(IOException.class, () -> {
            try (ZipOutputStream zo = new ZipOutputStream(wrappedStream)) {
                zo.putNextEntry(new ZipEntry("file.txt"));
                zo.write("hello".getBytes(StandardCharsets.UTF_8));
                wrappedStream.failOnWrite = true;
            } 
        });

        assertEquals(WrappedOutputStream.WRITE_MSG, exception.getMessage());

        assertEquals(1, wrappedStream.timesClosed,
                "Expected wrapped output stream to be closed once");
    }

    /**
     * Sanity check that the wrapped stream is closed also for the
     * normal case where the wrapped stream does not throw
     *
     * @throws IOException if an unexpected IOException occurs
     */
    @Test
    public void noExceptions() throws IOException {

        WrappedOutputStream wrappedStream = new WrappedOutputStream();

        try (ZipOutputStream zo = new ZipOutputStream(wrappedStream)) {
            zo.putNextEntry(new ZipEntry("file.txt"));
            zo.write("hello".getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(1, wrappedStream.timesClosed,
                "Expected wrapped output stream to be closed once");
    }

    /**
     * Check that an exception thrown while closing the wrapped
     * stream is propagated to the caller without any suppressed exceptions
     *
     * @throws IOException if an unexpected IOException occurs
     */
    @Test
    public void exceptionDuringClose() throws IOException {

        WrappedOutputStream wrappedStream = new WrappedOutputStream();

        IOException exception = assertThrows(IOException.class, () -> {
            try (ZipOutputStream zo = new ZipOutputStream(wrappedStream)) {
                zo.putNextEntry(new ZipEntry("file.txt"));
                zo.write("hello".getBytes(StandardCharsets.UTF_8));
                wrappedStream.failOnClose = true;
            }
        });

        assertEquals(WrappedOutputStream.CLOSE_MSG, exception.getMessage());

        assertEquals(0, exception.getSuppressed().length);

        assertEquals(1, wrappedStream.timesClosed,
                "Expected wrapped output stream to be closed once");
    }

    /**
     * Check that if an exception is thrown while closing the wrapped stream,
     * then later close attempts will not close the wrapped stream again
     *
     * @throws IOException if an unexpected IOException occurs
     */
    @Test
    public void doubleCloseShouldCloseWrappedStreamOnce() throws IOException {

        WrappedOutputStream wrappedStream = new WrappedOutputStream();

        AtomicReference<ZipOutputStream> ref = new AtomicReference<>();

        IOException exception = assertThrows(IOException.class, () -> {
            try (ZipOutputStream zo = new ZipOutputStream(wrappedStream)) {
                ref.set(zo);
                zo.putNextEntry(new ZipEntry("file.txt"));
                zo.write("hello".getBytes(StandardCharsets.UTF_8));
                wrappedStream.failOnClose = true;
            }
        });

        ref.get().close();

        assertEquals(WrappedOutputStream.CLOSE_MSG, exception.getMessage());

        assertEquals(0, exception.getSuppressed().length);

        assertEquals(1, wrappedStream.timesClosed,
                "Expected wrapped output stream to be closed once");
    }

    /**
     * Check that when the wrapped stream throws while calling finish
     * AND while being closed, then the propagated exception is the one
     * from the close operation, with the exception thrown during finish
     * added as a suppressed exception.
     *
     * @throws IOException if an unexpected IOException occurs
     */
    @Test
    public void exceptionDuringFinishAndClose() throws IOException {

        WrappedOutputStream wrappedStream = new WrappedOutputStream();

        IOException exception = assertThrows(IOException.class, () -> {
            try (ZipOutputStream zo = new ZipOutputStream(wrappedStream)) {

                zo.putNextEntry(new ZipEntry("file.txt"));
                zo.write("hello".getBytes(StandardCharsets.UTF_8));
                wrappedStream.failOnWrite = true;
                wrappedStream.failOnClose = true;
            }
        });

        assertEquals(WrappedOutputStream.CLOSE_MSG, exception.getMessage());

        assertEquals(1, exception.getSuppressed().length, "Expected suppressed exception");
        assertEquals(WrappedOutputStream.WRITE_MSG, exception.getSuppressed()[0].getMessage());

        assertEquals(1, wrappedStream.timesClosed,
                "Expected wrapped output stream to be closed once");
    }

    /**
     * Check that when the wrapped stream throws the same IOException
     * (identical instance) for write and close operations, then no attempt
     * is made to add the exception instance to itself as a suppressed exception.
     *
     * @throws IOException if an unexpected IOException occurs
     */
    @Test
    public void sameExceptionDuringFinishAndClose() throws IOException {

        WrappedOutputStream wrappedStream = new WrappedOutputStream();

        IOException exception = assertThrows(IOException.class, () -> {
            try (ZipOutputStream zo = new ZipOutputStream(wrappedStream)) {
                zo.putNextEntry(new ZipEntry("file.txt"));
                zo.write("hello".getBytes(StandardCharsets.UTF_8));
                wrappedStream.failException = new IOException("same fail");
                wrappedStream.failOnWrite = true;
                wrappedStream.failOnClose = true;
            }
        });
        assertSame(wrappedStream.failException, exception);

        assertEquals(0, exception.getSuppressed().length, "Expected no suppressed exception");

        assertEquals(1, wrappedStream.timesClosed,
                "Expected wrapped output stream to be closed once");
    }

    /**
     * Output stream which conditionally throws IOException on writes
     * and tracks its close status.
     */
    static class WrappedOutputStream extends FilterOutputStream {
        static final String WRITE_MSG = "fail during write";
        static final String CLOSE_MSG = "fail during close";
        boolean failOnWrite = false;
        boolean failOnClose = false;
        IOException failException = null;
        int timesClosed = 0;

        public WrappedOutputStream() {
            super(new ByteArrayOutputStream());
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException{
            if (failOnWrite) {
                throw failException != null ? failException : new IOException(WRITE_MSG);
            } else {
                super.write(b, off, len);
            }
        }

        @Override
        public void close() throws IOException {
            timesClosed++;
            if (failOnClose) {
                throw failException != null ? failException : new IOException(CLOSE_MSG);
            } else {
                super.close();
            }
        }
    }
}
