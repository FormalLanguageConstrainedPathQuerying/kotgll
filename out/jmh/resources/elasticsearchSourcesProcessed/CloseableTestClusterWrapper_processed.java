/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.test;

import org.elasticsearch.common.collect.Iterators;

import java.io.Closeable;
import java.io.IOException;

/**
 * Adapter to make one or more {@link TestCluster} instances compatible with things like try-with-resources blocks and IOUtils.
 */
public record CloseableTestClusterWrapper(TestCluster testCluster) implements Closeable {
    @Override
    public void close() throws IOException {
        testCluster().close();
    }

    public static Iterable<Closeable> wrap(Iterable<? extends TestCluster> clusters) {
        return () -> Iterators.map(clusters.iterator(), CloseableTestClusterWrapper::new);
    }

    public static Iterable<Closeable> wrap(TestCluster... clusters) {
        return wrap(() -> Iterators.forArray(clusters));
    }
}
