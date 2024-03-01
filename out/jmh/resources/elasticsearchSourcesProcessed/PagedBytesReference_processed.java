/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.bytes;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.elasticsearch.common.util.ByteArray;
import org.elasticsearch.common.util.PageCacheRecycler;

import java.io.IOException;
import java.util.Objects;

/**
 * A page based bytes reference, internally holding the bytes in a paged
 * data structure.
 */
public class PagedBytesReference extends AbstractBytesReference {

    private static final int PAGE_SIZE = PageCacheRecycler.BYTE_PAGE_SIZE;

    private final ByteArray byteArray;
    private final int offset;

    PagedBytesReference(ByteArray byteArray, int from, int length) {
        super(length);
        assert byteArray.hasArray() == false : "use BytesReference#fromByteArray";
        this.byteArray = byteArray;
        this.offset = from;
    }

    @Override
    public byte get(int index) {
        return byteArray.get(offset + index);
    }

    @Override
    public BytesReference slice(int from, int length) {
        if (from == 0 && this.length == length) {
            return this;
        }
        Objects.checkFromIndexSize(from, length, this.length);
        return new PagedBytesReference(byteArray, offset + from, length);
    }

    @Override
    public BytesRef toBytesRef() {
        BytesRef bref = new BytesRef();
        byteArray.get(offset, length, bref);
        return bref;
    }

    @Override
    public final BytesRefIterator iterator() {
        final int offset = this.offset;
        final int length = this.length;
        final int initialFragmentSize = offset != 0 ? PAGE_SIZE - (offset % PAGE_SIZE) : PAGE_SIZE;
        return new BytesRefIterator() {
            int position = 0;
            int nextFragmentSize = Math.min(length, initialFragmentSize);
            final BytesRef slice = new BytesRef();

            @Override
            public BytesRef next() throws IOException {
                if (nextFragmentSize != 0) {
                    final boolean materialized = byteArray.get(offset + position, nextFragmentSize, slice);
                    assert materialized == false : "iteration should be page aligned but array got materialized";
                    position += nextFragmentSize;
                    final int remaining = length - position;
                    nextFragmentSize = Math.min(remaining, PAGE_SIZE);
                    return slice;
                } else {
                    assert nextFragmentSize == 0 : "fragmentSize expected [0] but was: [" + nextFragmentSize + "]";
                    return null; 
                }
            }
        };
    }

    @Override
    public long ramBytesUsed() {
        return byteArray.ramBytesUsed();
    }
}
