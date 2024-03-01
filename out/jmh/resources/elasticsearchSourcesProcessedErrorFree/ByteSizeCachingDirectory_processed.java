/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.store;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.elasticsearch.common.lucene.store.FilterIndexOutput;
import org.elasticsearch.common.util.SingleObjectCache;
import org.elasticsearch.core.TimeValue;

import java.io.IOException;
import java.io.UncheckedIOException;

final class ByteSizeCachingDirectory extends ByteSizeDirectory {

    private static class SizeAndModCount {
        final long size;
        final long modCount;
        final boolean pendingWrite;

        SizeAndModCount(long length, long modCount, boolean pendingWrite) {
            this.size = length;
            this.modCount = modCount;
            this.pendingWrite = pendingWrite;
        }
    }

    private final SingleObjectCache<SizeAndModCount> size;
    private long modCount = 0;
    private long numOpenOutputs = 0;

    ByteSizeCachingDirectory(Directory in, TimeValue refreshInterval) {
        super(in);
        size = new SingleObjectCache<>(refreshInterval, new SizeAndModCount(0L, -1L, true)) {
            @Override
            protected SizeAndModCount refresh() {
                final long modCount;
                final boolean pendingWrite;
                synchronized (ByteSizeCachingDirectory.this) {
                    modCount = ByteSizeCachingDirectory.this.modCount;
                    pendingWrite = ByteSizeCachingDirectory.this.numOpenOutputs != 0;
                }
                final long size;
                try {
                    size = estimateSizeInBytes(getDelegate());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return new SizeAndModCount(size, modCount, pendingWrite);
            }

            @Override
            protected boolean needsRefresh() {
                if (super.needsRefresh() == false) {
                    return false;
                }
                SizeAndModCount cached = getNoRefresh();
                if (cached.pendingWrite) {
                    return true;
                }
                synchronized (ByteSizeCachingDirectory.this) {
                    return numOpenOutputs != 0 || cached.modCount != modCount;
                }
            }
        };
    }

    @Override
    public long estimateSizeInBytes() throws IOException {
        try {
            return size.getOrRefresh().size;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    public long estimateDataSetSizeInBytes() throws IOException {
        return estimateSizeInBytes(); 
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        return wrapIndexOutput(super.createOutput(name, context));
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
        return wrapIndexOutput(super.createTempOutput(prefix, suffix, context));
    }

    private IndexOutput wrapIndexOutput(IndexOutput out) {
        synchronized (this) {
            numOpenOutputs++;
        }
        return new FilterIndexOutput(out.toString(), out) {
            private boolean closed;

            @Override
            public void writeBytes(byte[] b, int length) throws IOException {
                super.writeBytes(b, length);
            }

            @Override
            public void writeByte(byte b) throws IOException {
                super.writeByte(b);
            }

            @Override
            public void writeInt(int i) throws IOException {
                out.writeInt(i);
            }

            @Override
            public void writeShort(short s) throws IOException {
                out.writeShort(s);
            }

            @Override
            public void writeLong(long l) throws IOException {
                out.writeLong(l);
            }

            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    synchronized (ByteSizeCachingDirectory.this) {
                        if (closed == false) {
                            closed = true;
                            numOpenOutputs--;
                            modCount++;
                        }
                    }
                }
            }
        };
    }

    @Override
    public void deleteFile(String name) throws IOException {
        try {
            super.deleteFile(name);
        } finally {
            synchronized (this) {
                modCount++;
            }
        }
    }
}
