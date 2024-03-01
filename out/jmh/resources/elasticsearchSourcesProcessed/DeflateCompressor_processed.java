/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.compress;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.core.Assertions;
import org.elasticsearch.core.Releasable;
import org.elasticsearch.core.Streams;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.InflaterOutputStream;

/**
 * {@link Compressor} implementation based on the DEFLATE compression algorithm.
 */
public class DeflateCompressor implements Compressor {

    private static final byte[] HEADER = new byte[] { 'D', 'F', 'L', '\0' };

    public static final int HEADER_SIZE = HEADER.length;

    private static final int LEVEL = 3;
    public static final int BUFFER_SIZE = 4096;

    @Override
    public boolean isCompressed(BytesReference bytes) {
        if (bytes.length() < HEADER.length) {
            return false;
        }
        for (int i = 0; i < HEADER.length; ++i) {
            if (bytes.get(i) != HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    private static final ThreadLocal<ReleasableReference<Inflater>> inflaterForStreamRef = ThreadLocal.withInitial(() -> {
        final Inflater inflater = new Inflater(true);
        return new ReleasableReference<>(inflater, inflater::reset);
    });

    private static final ThreadLocal<ReleasableReference<Deflater>> deflaterForStreamRef = ThreadLocal.withInitial(() -> {
        final Deflater deflater = new Deflater(LEVEL, true);
        return new ReleasableReference<>(deflater, deflater::reset);
    });

    private static final class ReleasableReference<T> implements Releasable {

        protected final T resource;

        private final Releasable releasable;

        private Thread thread = null;

        boolean inUse;

        protected ReleasableReference(T resource, Releasable releasable) {
            this.resource = resource;
            this.releasable = releasable;
        }

        T get() {
            if (Assertions.ENABLED) {
                assert thread == null;
                thread = Thread.currentThread();
            }
            assert inUse == false;
            inUse = true;
            return resource;
        }

        @Override
        public void close() {
            if (Assertions.ENABLED) {
                assert thread == Thread.currentThread()
                    : "Opened on [" + thread.getName() + "] but closed on [" + Thread.currentThread().getName() + "]";
                thread = null;
            }
            assert inUse;
            inUse = false;
            releasable.close();
        }
    }

    @Override
    public InputStream threadLocalInputStream(InputStream in) throws IOException {
        return inputStream(in, true);
    }

    /**
     * Creates a new input stream that decompresses the contents read from the provided input stream.
     * Closing the returned stream will close the provided input stream.
     * Optionally uses thread-local, pooled resources to save off-heap allocations if the stream is guaranteed to not escape the current
     * thread.
     *
     * @param in           input stream to wrap
     * @param threadLocal  whether this stream will only be used on the current thread or not
     * @return             decompressing stream
     */
    public static InputStream inputStream(InputStream in, boolean threadLocal) throws IOException {
        final byte[] headerBytes = new byte[HEADER.length];
        final int len = Streams.readFully(in, headerBytes);
        if (len != HEADER.length || Arrays.equals(headerBytes, HEADER) == false) {
            throw new IllegalArgumentException("Input stream is not compressed with DEFLATE!");
        }

        final Releasable releasable;
        final Inflater inflater;
        if (threadLocal) {
            final ReleasableReference<Inflater> current = inflaterForStreamRef.get();
            if (current.inUse) {
                inflater = new Inflater(true);
                releasable = inflater::end;
            } else {
                inflater = current.get();
                releasable = current;
            }
        } else {
            inflater = new Inflater(true);
            releasable = inflater::end;
        }
        return new InflaterInputStream(in, inflater, BUFFER_SIZE) {

            private Releasable release = releasable;

            @Override
            public void close() throws IOException {
                if (release == null) {
                    return;
                }
                try {
                    super.close();
                } finally {
                    release.close();
                    release = null;
                }
            }
        };
    }

    @Override
    public OutputStream threadLocalOutputStream(OutputStream out) throws IOException {
        out.write(HEADER);
        final ReleasableReference<Deflater> current = deflaterForStreamRef.get();
        final Releasable releasable;
        final Deflater deflater;
        if (current.inUse) {
            deflater = new Deflater(LEVEL, true);
            releasable = deflater::end;
        } else {
            deflater = current.get();
            releasable = current;
        }
        final boolean syncFlush = true;
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(out, deflater, BUFFER_SIZE, syncFlush) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    releasable.close();
                }
            }
        };
        return new BufferedOutputStream(deflaterOutputStream, BUFFER_SIZE);
    }

    private static final ThreadLocal<BytesStreamOutput> baos = ThreadLocal.withInitial(BytesStreamOutput::new);

    private static final ThreadLocal<Inflater> inflaterRef = ThreadLocal.withInitial(() -> new Inflater(true));

    @Override
    public BytesReference uncompress(BytesReference bytesReference) throws IOException {
        if (bytesReference.length() < HEADER.length) {
            throw new IOException(
                String.format(
                    Locale.ROOT,
                    "Input bytes length %d is less than DEFLATE header size %d",
                    bytesReference.length(),
                    HEADER.length
                )
            );
        }
        final BytesStreamOutput buffer = baos.get();
        try {
            final Inflater inflater = inflaterRef.get();
            try (InflaterOutputStream ios = new InflaterOutputStream(buffer, inflater)) {
                bytesReference.slice(HEADER.length, bytesReference.length() - HEADER.length).writeTo(ios);
            } finally {
                inflater.reset();
            }
            return buffer.copyBytes();
        } finally {
            buffer.reset();
        }
    }

    private static final ThreadLocal<Deflater> deflaterRef = ThreadLocal.withInitial(() -> new Deflater(LEVEL, true));

    @Override
    public BytesReference compress(BytesReference bytesReference) throws IOException {
        final BytesStreamOutput buffer = baos.get();
        try {
            buffer.write(HEADER);
            final Deflater deflater = deflaterRef.get();
            try (DeflaterOutputStream dos = new DeflaterOutputStream(buffer, deflater, true)) {
                bytesReference.writeTo(dos);
            } finally {
                deflater.reset();
            }
            return buffer.copyBytes();
        } finally {
            buffer.reset();
        }
    }
}
