/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.codec.tsdb;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class ES87TSDBDocValuesEncoderTests extends LuceneTestCase {

    private final ES87TSDBDocValuesEncoder encoder;
    private final int blockSize = ES87TSDBDocValuesFormat.NUMERIC_BLOCK_SIZE;

    public ES87TSDBDocValuesEncoderTests() {
        this.encoder = new ES87TSDBDocValuesEncoder();
    }

    public void testRandomValues() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            arr[i] = random().nextLong();
        }
        arr[4] = Long.MAX_VALUE / 2;
        arr[100] = Long.MIN_VALUE / 2 - 1;
        final long expectedNumBytes = 2 
            + (blockSize * 64) / Byte.SIZE; 
        doTest(arr, expectedNumBytes);
    }

    public void testAllEqual() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.fill(arr, 3);
        final long expectedNumBytes = 2; 
        doTest(arr, expectedNumBytes);
    }

    public void testSmallPositiveValues() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            arr[i] = (i + 2) & 0x03; 
        }
        final long expectedNumBytes = 1 
            + (blockSize * 2) / Byte.SIZE; 
        doTest(arr, expectedNumBytes);
    }

    public void testSmallPositiveValuesWithOffset() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            arr[i] = 1000 + ((i + 2) & 0x03); 
        }
        final long expectedNumBytes = 3 
            + (blockSize * 2) / Byte.SIZE; 
        doTest(arr, expectedNumBytes);
    }

    public void testSmallPositiveValuesWithNegativeOffset() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            arr[i] = -1000 + ((i + 2) & 0x03); 
        }
        final long expectedNumBytes = 3 
            + (blockSize * 2) / Byte.SIZE; 
        doTest(arr, expectedNumBytes);
    }

    /**
     * Integers as doubles, GCD compression should help here given high numbers of trailing zeroes.
     */
    public void testIntegersAsDoubles() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            arr[i] = NumericUtils.doubleToSortableLong((i + 2) & 0x03); 
        }
        final long expectedNumBytes = 9 
            + blockSize * 12 / Byte.SIZE; 
        doTest(arr, expectedNumBytes);
    }

    public void testDecreasingValues() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            arr[i] = 1000 - 30 * i;
        }
        final long expectedNumBytes = 4; 
        doTest(arr, expectedNumBytes);
    }

    public void testTwoValues() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            arr[i] = i % 3 == 1 ? 42 : 100;
        }
        final long expectedNumBytes = 3 
            + (blockSize * 1) / Byte.SIZE; 
        doTest(arr, expectedNumBytes);
    }

    /** Monotonic timestamps: delta coding + GCD compression. */
    public void testMonotonicTimestamps() throws IOException {
        long offset = 2 * 60 * 60 * 1000; 
        long first = (2021 - 1970) * 365L * 24 * 60 * 60 * 1000 + offset;
        long granularity = 24L * 60 * 60 * 1000; 

        long[] arr = new long[blockSize];
        arr[0] = first;
        for (int i = 1; i < blockSize; ++i) {
            arr[i] = arr[i - 1] + (5 + i % 4) * granularity; 
        }
        final long expectedNumBytes = 16 
            + (blockSize * 2) / Byte.SIZE; 
        doTest(arr, expectedNumBytes);
    }

    public void testZeroOrMinValue() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            arr[i] = i % 3 == 1 ? Long.MIN_VALUE : 0;
        }
        final long expectedNumBytes = 10 
            + (blockSize * 1) / Byte.SIZE; 
        doTest(arr, expectedNumBytes);
    }

    public void testFloatingPointValues() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            double value = (i % 2 == 1) ? (i * 1956.0) : (i * 356923.5);
            arr[i] = Double.doubleToLongBits(value);
        }
        final long expectedNumBytes = 6 
            + (blockSize * 40) / Byte.SIZE; 
        doTest(arr, expectedNumBytes);
    }

    public void testBitsPerValueFullRange() throws IOException {
        final Random random = new Random(17);
        long[] arr = new long[blockSize];
        long constant = 1;
        for (int bitsPerValue = 0; bitsPerValue <= 64; bitsPerValue++) {
            for (int i = 0; i < blockSize; ++i) {
                if (bitsPerValue == 0) {
                    arr[i] = constant;
                } else {
                    arr[i] = random.nextLong(0, bitsPerValue <= 62 ? 1L << bitsPerValue : Long.MAX_VALUE);
                }
            }
            long actualBitsPerValue = DocValuesForUtil.roundBits(bitsPerValue);
            int actualTokenBytes = bitsPerValue < 16 ? 1 : 2;
            final long expectedNumBytes = bitsPerValue == 0
                ? 2
                : actualTokenBytes 
                    + (blockSize * actualBitsPerValue) / Byte.SIZE; 
            doTest(arr, expectedNumBytes);
        }
    }

    private void doTest(long[] arr, long expectedNumBytes) throws IOException {
        final long[] expected = arr.clone();
        try (Directory dir = newDirectory()) {
            try (IndexOutput out = dir.createOutput("tests.bin", IOContext.DEFAULT)) {
                encoder.encode(arr, out);
                assertEquals(expectedNumBytes, out.getFilePointer());
            }
            try (IndexInput in = dir.openInput("tests.bin", IOContext.DEFAULT)) {
                long[] decoded = new long[blockSize];
                for (int i = 0; i < decoded.length; ++i) {
                    decoded[i] = random().nextLong();
                }
                encoder.decode(in, decoded);
                assertEquals(in.length(), in.getFilePointer());
                assertArrayEquals(expected, decoded);
            }
        }
    }

    public void testEncodeOrdinalsSingleValueSmall() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.fill(arr, 63);
        final long expectedNumBytes = 1;

        doTestOrdinals(arr, expectedNumBytes);
    }

    public void testEncodeOrdinalsSingleValueMedium() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.fill(arr, 64);
        final long expectedNumBytes = 2;

        doTestOrdinals(arr, expectedNumBytes);
    }

    public void testEncodeOrdinalsSingleValueLarge() throws IOException {
        long[] arr = new long[blockSize];
        final long expectedNumBytes = 3;
        Arrays.fill(arr, (1 << 6 + 7 + 7) - 1);

        doTestOrdinals(arr, expectedNumBytes);
    }

    public void testEncodeOrdinalsSingleValueGrande() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.fill(arr, Long.MAX_VALUE);
        final long expectedNumBytes = 1 + blockSize * Long.BYTES;

        doTestOrdinals(arr, expectedNumBytes);
    }

    public void testEncodeOrdinalsTwoValuesSmall() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.fill(arr, 63);
        arr[0] = 1;
        final long expectedNumBytes = 3;

        doTestOrdinals(arr, expectedNumBytes);
    }

    public void testEncodeOrdinalsTwoValuesLarge() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.fill(arr, Long.MAX_VALUE >> 2);
        arr[0] = (Long.MAX_VALUE >> 2) - 1;
        final long expectedNumBytes = 11;

        doTestOrdinals(arr, expectedNumBytes);
    }

    public void testEncodeOrdinalsTwoValuesGrande() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.fill(arr, Long.MAX_VALUE);
        arr[0] = Long.MAX_VALUE - 1;
        final long expectedNumBytes = 1 + blockSize * Long.BYTES;

        doTestOrdinals(arr, expectedNumBytes);
    }

    public void testEncodeOrdinalsNoRepetitions() throws IOException {
        long[] arr = new long[blockSize];
        for (int i = 0; i < blockSize; ++i) {
            arr[i] = i;
        }
        doTestOrdinals(arr, 113);
    }

    public void testEncodeOrdinalsBitPack3Bits() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.fill(arr, 4);
        for (int i = 0; i < 4; i++) {
            arr[i] = i;
        }
        doTestOrdinals(arr, 49);
    }

    public void testEncodeOrdinalsCycle2() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.setAll(arr, i -> i % 2);
        doTestOrdinals(arr, 3);
    }

    public void testEncodeOrdinalsCycle3() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.setAll(arr, i -> i % 3);
        doTestOrdinals(arr, 4);
    }

    public void testEncodeOrdinalsLongCycle() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.setAll(arr, i -> i % 32);
        doTestOrdinals(arr, 34);
    }

    public void testEncodeOrdinalsCycleTooLong() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.setAll(arr, i -> i % 33);
        doTestOrdinals(arr, 97);
    }

    public void testEncodeOrdinalsAlmostCycle() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.setAll(arr, i -> i % 3);
        arr[arr.length - 1] = 4;
        doTestOrdinals(arr, 49);
    }

    public void testEncodeOrdinalsDifferentCycles() throws IOException {
        long[] arr = new long[blockSize];
        Arrays.setAll(arr, i -> i > 64 ? i % 4 : i % 3);
        doTestOrdinals(arr, 33);
    }

    private void doTestOrdinals(long[] arr, long expectedNumBytes) throws IOException {
        long maxOrd = 0;
        for (long ord : arr) {
            maxOrd = Math.max(maxOrd, ord);
        }
        final int bitsPerOrd = PackedInts.bitsRequired(maxOrd);
        final long[] expected = arr.clone();
        try (Directory dir = newDirectory()) {
            try (IndexOutput out = dir.createOutput("tests.bin", IOContext.DEFAULT)) {
                encoder.encodeOrdinals(arr, out, bitsPerOrd);
                assertEquals(expectedNumBytes, out.getFilePointer());
            }
            try (IndexInput in = dir.openInput("tests.bin", IOContext.DEFAULT)) {
                long[] decoded = new long[blockSize];
                for (int i = 0; i < decoded.length; ++i) {
                    decoded[i] = random().nextLong();
                }
                encoder.decodeOrdinals(in, decoded, bitsPerOrd);
                assertEquals(in.length(), in.getFilePointer());
                assertArrayEquals(expected, decoded);
            }
        }
    }
}
