/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https:
 */
package jdk.internal.org.jline.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class NonBlockingPumpInputStream extends NonBlockingInputStream {

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;

    private final OutputStream output;

    private boolean closed;

    private IOException ioException;

    public NonBlockingPumpInputStream() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public NonBlockingPumpInputStream(int bufferSize) {
        byte[] buf = new byte[bufferSize];
        this.readBuffer = ByteBuffer.wrap(buf);
        this.writeBuffer = ByteBuffer.wrap(buf);
        this.output = new NbpOutputStream();
        readBuffer.limit(0);
    }

    public OutputStream getOutputStream() {
        return this.output;
    }

    private int wait(ByteBuffer buffer, long timeout) throws IOException {
        Timeout t = new Timeout(timeout);
        while (!closed && !buffer.hasRemaining() && !t.elapsed()) {
            notifyAll();
            try {
                wait(t.timeout());
                checkIoException();
            } catch (InterruptedException e) {
                checkIoException();
                throw new InterruptedIOException();
            }
        }
        return buffer.hasRemaining()
                ? 0
                : closed
                    ? EOF
                    : READ_EXPIRED;
    }

    private static boolean rewind(ByteBuffer buffer, ByteBuffer other) {
        if (buffer.position() > other.position()) {
            other.limit(buffer.position());
        }
        if (buffer.position() == buffer.capacity()) {
            buffer.rewind();
            buffer.limit(other.position());
            return true;
        } else {
            return false;
        }
    }

    public synchronized int available() {
        int count = readBuffer.remaining();
        if (writeBuffer.position() < readBuffer.position()) {
            count += writeBuffer.position();
        }
        return count;
    }

    @Override
    public synchronized int read(long timeout, boolean isPeek) throws IOException {
        checkIoException();
        int res = wait(readBuffer, timeout);
        if (res >= 0) {
            res = readBuffer.get() & 0x00FF;
        }
        rewind(readBuffer, writeBuffer);
        return res;
    }

    @Override
    public synchronized int readBuffered(byte[] b, int off, int len, long timeout) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || off + len < b.length) {
            throw new IllegalArgumentException();
        } else if (len == 0) {
            return 0;
        } else {
            checkIoException();
            int res = wait(readBuffer, timeout);
            if (res >= 0) {
                res = 0;
                while (res < len && readBuffer.hasRemaining()) {
                    b[off + res++] = (byte) (readBuffer.get() & 0x00FF);
                }
            }
            rewind(readBuffer, writeBuffer);
            return res;
        }
    }

    public synchronized void setIoException(IOException exception) {
        this.ioException = exception;
        notifyAll();
    }

    protected synchronized void checkIoException() throws IOException {
        if (ioException != null) {
            throw ioException;
        }
    }

    synchronized void write(byte[] cbuf, int off, int len) throws IOException {
        while (len > 0) {
            if (wait(writeBuffer, 0L) == EOF) {
                throw new ClosedException();
            }
            int count = Math.min(len, writeBuffer.remaining());
            writeBuffer.put(cbuf, off, count);
            off += count;
            len -= count;
            rewind(writeBuffer, readBuffer);
        }
    }

    synchronized void flush() {
        if (readBuffer.hasRemaining()) {
            notifyAll();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        this.closed = true;
        notifyAll();
    }

    private class NbpOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            NonBlockingPumpInputStream.this.write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] cbuf, int off, int len) throws IOException {
            NonBlockingPumpInputStream.this.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            NonBlockingPumpInputStream.this.flush();
        }

        @Override
        public void close() throws IOException {
            NonBlockingPumpInputStream.this.close();
        }

    }

}
