/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8143012
 * @summary CRC32 Intrinsics support on SPARC
 *
 * @run main/othervm/timeout=720 -Xbatch compiler.intrinsics.zip.TestCRC32 -m
 */

package compiler.intrinsics.zip;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class TestCRC32 {
    final static long polyBits = (1L<<(32-32)) + (1L<<(32-26)) + (1L<<(32-23)) + (1L<<(32-22))
                               + (1L<<(32-16)) + (1L<<(32-12)) + (1L<<(32-11)) + (1L<<(32-10))
                               + (1L<<(32-8))  + (1L<<(32-7))  + (1L<<(32-5))  + (1L<<(32-4))
                               + (1L<<(32-2))  + (1L<<(32-1))  + (1L<<(32-0));
    final static long polyBitsShifted = polyBits>>1;

    public static void main(String[] args) throws Exception {
        int offset = Integer.getInteger("offset", 0);
        int msgSize = Integer.getInteger("msgSize", 512);
        boolean multi = false;
        int iters = 20000;
        int warmupIters = 20000;

        if (args.length > 0) {
            if (args[0].equals("-m")) {
                multi = true;
            } else {
                iters = Integer.valueOf(args[0]);
            }
            if (args.length > 1) {
                warmupIters = Integer.valueOf(args[1]);
            }
        }

        if (multi) {
            test_multi(warmupIters);
            return;
        }

        System.out.println(" offset = " + offset);
        System.out.println("msgSize = " + msgSize + " bytes");
        System.out.println("  iters = " + iters);

        byte[] b = initializedBytes(msgSize, offset);

        final long crcReference = update_byteLoop(0, b, offset);

        CRC32 crc0 = new CRC32();
        CRC32 crc1 = new CRC32();
        CRC32 crc2 = new CRC32();

        crc0.update(b, offset, msgSize);
        check(crc0, crcReference);

        System.out.println("-------------------------------------------------------");

        /* warm up */
        for (int i = 0; i < warmupIters; i++) {
            crc1.reset();
            crc1.update(b, offset, msgSize);
            check(crc1, crcReference);
        }

        /* check correctness
         * Do that before measuring performance
         * to even better heat up involved methods.
         */
        for (int i = 0; i < iters; i++) {
            crc1.reset();
            crc1.update(b, offset, msgSize);
            check(crc1, crcReference);
        }
        report("CRCs", crc1, crcReference);

        /* measure performance
         * Don't spoil times with error checking.
         */
        long start = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            crc1.reset();
            crc1.update(b, offset, msgSize);
        }
        long end = System.nanoTime();

        double total = (double)(end - start)/1e9;         
        double thruput = (double)msgSize*iters/1e6/total; 
        System.out.println("CRC32.update(byte[]) runtime = " + total + " seconds");
        System.out.println("CRC32.update(byte[]) throughput = " + thruput + " MB/s");
        report("CRCs", crc1, crcReference);

        System.out.println("-------------------------------------------------------");

        ByteBuffer buf = ByteBuffer.allocateDirect(msgSize);
        buf.put(b, offset, msgSize);
        buf.flip();

        /* warm up */
        for (int i = 0; i < warmupIters; i++) {
            crc2.reset();
            crc2.update(buf);
            buf.rewind();
            check(crc2, crcReference);
        }

        /* check correctness
         * Do that before measuring performance
         * to even better heat up involved methods.
         */
        for (int i = 0; i < iters; i++) {
            crc2.reset();
            crc2.update(buf);
            buf.rewind();
            check(crc2, crcReference);
        }
        report("CRCs", crc2, crcReference);

        /* measure performance
         * Don't spoil times with error checking.
         */
        start = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            crc2.reset();
            crc2.update(buf);
            buf.rewind();
        }
        end = System.nanoTime();
        total = (double)(end - start)/1e9;         
        thruput = (double)msgSize*iters/1e6/total; 
        System.out.println("CRC32.update(ByteBuffer) runtime = " + total + " seconds");
        System.out.println("CRC32.update(ByteBuffer) throughput = " + thruput + " MB/s");
        report("CRCs", crc2, crcReference);

        System.out.println("-------------------------------------------------------");
    }

    public static long update_byteLoop(long crc, byte[] buf, int offset) {
        return update_byteLoop(crc, buf, offset, buf.length-offset);
    }

    public static long update_byteLoop(long crc, byte[] buf, int offset, int length) {
        int end = length+offset;
        for (int i = offset; i < end; i++) {
            crc = update_singlebyte(crc, polyBitsShifted, buf[i]);
        }
        return crc;
    }

    public static long update_singlebyte(long crc, long polynomial, int val) {
        crc = (crc ^ -1L) & 0x00000000ffffffffL;  
        crc =  crc ^ (val&0xff);                  
        for (int i = 0; i <  8; i++) {
            boolean bitset = (crc & 0x01L) != 0;

            crc = crc>>1;
            if (bitset) {
                crc = crc ^ polynomial;
                crc = crc & 0x00000000ffffffffL;
            }
        }
        crc = (crc ^ -1L) & 0x00000000ffffffffL;  
        return crc;
    }

    private static void report(String s, Checksum crc, long crcReference) {
        System.out.printf("%s: crc = %08x, crcReference = %08x\n",
                          s, crc.getValue(), crcReference);
    }

    private static void check(Checksum crc, long crcReference) throws Exception {
        if (crc.getValue() != crcReference) {
            System.err.printf("ERROR: crc = %08x, crcReference = %08x\n",
                              crc.getValue(), crcReference);
            throw new Exception("TestCRC32 Error");
        }
    }

    private static byte[] initializedBytes(int M, int offset) {
        byte[] bytes = new byte[M + offset];
        for (int i = 0; i < offset; i++) {
            bytes[i] = (byte) i;
        }
        for (int i = offset; i < bytes.length; i++) {
            bytes[i] = (byte) (i - offset);
        }
        return bytes;
    }

    private static void test_multi(int iters) throws Exception {
        int len1 = 8;    
        int len2 = 32;   
        int len3 = 4096; 

        byte[] b = initializedBytes(len3*16, 0);
        int[] offsets = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 16, 32, 64, 128, 256, 512 };
        int[] sizes = { 0, 1, 2, 3, 4, 5, 6, 7,
                        len1, len1+1, len1+2, len1+3, len1+4, len1+5, len1+6, len1+7,
                        len1*2, len1*2+1, len1*2+3, len1*2+5, len1*2+7,
                        len2, len2+1, len2+3, len2+5, len2+7,
                        len2*2, len2*4, len2*8, len2*16, len2*32, len2*64,
                        len3, len3+1, len3+3, len3+5, len3+7,
                        len3*2, len3*4, len3*8,
                        len1+len2, len1+len2+1, len1+len2+3, len1+len2+5, len1+len2+7,
                        len1+len3, len1+len3+1, len1+len3+3, len1+len3+5, len1+len3+7,
                        len2+len3, len2+len3+1, len2+len3+3, len2+len3+5, len2+len3+7,
                        len1+len2+len3, len1+len2+len3+1, len1+len2+len3+3,
                        len1+len2+len3+5, len1+len2+len3+7,
                        (len1+len2+len3)*2, (len1+len2+len3)*2+1, (len1+len2+len3)*2+3,
                        (len1+len2+len3)*2+5, (len1+len2+len3)*2+7,
                        (len1+len2+len3)*3, (len1+len2+len3)*3-1, (len1+len2+len3)*3-3,
                        (len1+len2+len3)*3-5, (len1+len2+len3)*3-7 };
        CRC32[] crc1 = new CRC32[offsets.length*sizes.length];
        long[] crcReference = new long[offsets.length*sizes.length];
        int i, j, k;

        System.out.printf("testing %d cases ...\n", offsets.length*sizes.length);

        for (i = 0; i < offsets.length; i++) {
            for (j = 0; j < sizes.length; j++) {
                crc1[i*sizes.length + j] = new CRC32();
                crcReference[i*sizes.length + j] = update_byteLoop(0, b, offsets[i], sizes[j]);
            }
        }

        for (k = 0; k < iters; k++) {
            for (i = 0; i < offsets.length; i++) {
                for (j = 0; j < sizes.length; j++) {
                    crc1[i*sizes.length + j].reset();
                    crc1[i*sizes.length + j].update(b, offsets[i], sizes[j]);
                    check(crc1[i*sizes.length + j], crcReference[i*sizes.length + j]);
                }
            }
        }
    }
}
