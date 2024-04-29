/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 * This Java port of CLD3 was derived from Google's CLD3 project at https:
 */
package org.elasticsearch.xpack.core.ml.inference.preprocessing.customwordembedding;

import java.nio.charset.StandardCharsets;

/**
 * Custom Hash class necessary for hashing nGrams
 */
final class Hash32 {

    private static final int DEFAULT_SEED = 0xBEEF;

    private final int seed;

    Hash32(int seed) {
        this.seed = seed;
    }

    Hash32() {
        this(DEFAULT_SEED);
    }

    public long hash(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        return Integer.toUnsignedLong(hash32(bytes));
    }

    /**
     * Derived from https:
     *
     * It is critical that we utilize this hash as it determines which weight and quantile column/row we choose
     * when building the feature array.
     */
    private int hash32(byte[] data) {
        int n = data.length;
        int m = 0x5bd1e995;
        int r = 24;

        int h = (seed ^ n);

        int i = 0;
        while (n >= 4) {
            int k = decodeFixed32(data, i);
            k *= m;
            k ^= k >>> r; 
            k *= m;
            h *= m;
            h ^= k;
            i += 4;
            n -= 4;
        }

        if (n == 3) {
            h ^= Byte.toUnsignedInt(data[i + 2]) << 16;
        }
        if (n >= 2) {
            h ^= Byte.toUnsignedInt(data[i + 1]) << 8;
        }
        if (n >= 1) {
            h ^= Byte.toUnsignedInt(data[i]);
            h *= m;
        }

        h ^= h >>> 13; 
        h *= m;
        h ^= h >>> 15; 
        return h;
    }

    private static int decodeFixed32(byte[] ptr, int offset) {
        return Byte.toUnsignedInt(ptr[offset]) | Byte.toUnsignedInt(ptr[offset + 1]) << 8 | Byte.toUnsignedInt(ptr[offset + 2]) << 16 | Byte
            .toUnsignedInt(ptr[offset + 3]) << 24;
    }

}
