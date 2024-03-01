/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.recycler;

import org.elasticsearch.common.recycler.Recycler.V;
import org.elasticsearch.test.ESTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractRecyclerTestCase extends ESTestCase {

    protected static final byte FRESH = 1;
    protected static final byte RECYCLED = 2;
    protected static final byte DEAD = 42;

    protected static final Recycler.C<byte[]> RECYCLER_C = new AbstractRecyclerC<byte[]>() {

        @Override
        public byte[] newInstance() {
            byte[] value = new byte[10];
            Arrays.fill(value, FRESH);
            return value;
        }

        @Override
        public void recycle(byte[] value) {
            Arrays.fill(value, RECYCLED);
        }

        @Override
        public void destroy(byte[] value) {
            Arrays.fill(value, DEAD);
        }

    };

    protected void assertFresh(byte[] data) {
        assertNotNull(data);
        for (int i = 0; i < data.length; ++i) {
            assertEquals(FRESH, data[i]);
        }
    }

    protected void assertRecycled(byte[] data) {
        assertNotNull(data);
        for (int i = 0; i < data.length; ++i) {
            assertEquals(RECYCLED, data[i]);
        }
    }

    protected void assertDead(byte[] data) {
        assertNotNull(data);
        for (int i = 0; i < data.length; ++i) {
            assertEquals(DEAD, data[i]);
        }
    }

    protected abstract Recycler<byte[]> newRecycler(int limit);

    protected int limit = randomIntBetween(5, 10);

    public void testReuse() {
        Recycler<byte[]> r = newRecycler(limit);
        Recycler.V<byte[]> o = r.obtain();
        assertFalse(o.isRecycled());
        final byte[] b1 = o.v();
        assertFresh(b1);
        o.close();
        assertRecycled(b1);
        o = r.obtain();
        final byte[] b2 = o.v();
        if (o.isRecycled()) {
            assertRecycled(b2);
            assertSame(b1, b2);
        } else {
            assertFresh(b2);
            assertNotSame(b1, b2);
        }
        o.close();
    }

    public void testRecycle() {
        Recycler<byte[]> r = newRecycler(limit);
        Recycler.V<byte[]> o = r.obtain();
        assertFresh(o.v());
        random().nextBytes(o.v());
        o.close();
        o = r.obtain();
        assertRecycled(o.v());
        o.close();
    }

    public void testDoubleRelease() {
        final Recycler<byte[]> r = newRecycler(limit);
        final Recycler.V<byte[]> v1 = r.obtain();
        v1.close();
        try {
            v1.close();
        } catch (IllegalStateException e) {
            return;
        }
        final Recycler.V<byte[]> v2 = r.obtain();
        final Recycler.V<byte[]> v3 = r.obtain();
        assertNotSame(v2.v(), v3.v());
    }

    public void testDestroyWhenOverCapacity() {
        Recycler<byte[]> r = newRecycler(limit);

        Recycler.V<byte[]> o = r.obtain();
        byte[] data = o.v();
        assertFresh(data);

        List<V<byte[]>> vals = new ArrayList<>(limit);
        for (int i = 0; i < limit; ++i) {
            vals.add(r.obtain());
        }
        for (V<byte[]> v : vals) {
            v.close();
        }

        o.close();
        assertDead(data);
    }

    public void testClose() {
        Recycler<byte[]> r = newRecycler(limit);

        Recycler.V<byte[]> o = r.obtain();
        byte[] data = o.v();
        assertFresh(data);

        random().nextBytes(data);
        o.close();

        assertRecycled(data);
    }

}
