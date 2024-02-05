/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8204310
 * @summary Check how FileChannel behaves if the file size/offset change via
 *          RAF.setLength() and other methods.
 * @run main TruncateRAF
 */

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class TruncateRAF {

    static void checkState(RandomAccessFile raf, FileChannel fch,
            long expectedOffset, long expectedLength)
        throws IOException
    {
        long rafLength = raf.length();
        long rafOffset = raf.getFilePointer();
        long fchLength = fch.size();
        long fchOffset = fch.position();

        if (rafLength != expectedLength)
            throw new RuntimeException("rafLength (" + rafLength + ") != " +
                    "expectedLength (" + expectedLength + ")");
        if (rafOffset != expectedOffset)
            throw new RuntimeException("rafOffset (" + rafOffset + ") != " +
                    "expectedOffset (" + expectedOffset + ")");
        if (fchLength != expectedLength)
            throw new RuntimeException("fchLength (" + fchLength + ") != " +
                    "expectedLength (" + expectedLength + ")");
        if (fchOffset != expectedOffset)
            throw new RuntimeException("fchOffset (" + fchOffset + ") != " +
                    "expectedOffset (" + expectedOffset + ")");
    }

    public static void main(String[] args) throws Throwable {
        File file = new File("tmp");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel fch = raf.getChannel()) {

            checkState(raf, fch, 0, 0);

            raf.seek(42);
            checkState(raf, fch, 42, 0);

            fch.position(84);
            checkState(raf, fch, 84, 0);

            raf.write(1);
            checkState(raf, fch, 85, 85);

            raf.setLength(63);
            checkState(raf, fch, 63, 63);

            fch.write(ByteBuffer.wrap(new byte[1]));
            checkState(raf, fch, 64, 64);

            fch.position(32);
            checkState(raf, fch, 32, 64);

            fch.truncate(42);
            checkState(raf, fch, 32, 42);

            fch.truncate(16);
            checkState(raf, fch, 16, 16);

            fch.write(ByteBuffer.wrap(new byte[1]), 127);
            checkState(raf, fch, 16, 128);

            fch.write(ByteBuffer.wrap(new byte[1]), 42);
            checkState(raf, fch, 16, 128);

            raf.setLength(64);
            checkState(raf, fch, 16, 64);

            raf.seek(21);
            checkState(raf, fch, 21, 64);

            raf.skipBytes(4);
            checkState(raf, fch, 25, 64);

            raf.read();
            checkState(raf, fch, 26, 64);

            raf.setLength(0);
            checkState(raf, fch, 0, 0);

            fch.truncate(42);
            checkState(raf, fch, 0, 0);

            raf.setLength(42);
            checkState(raf, fch, 0, 42);

            raf.seek(512);
            checkState(raf, fch, 512, 42);

            fch.truncate(256);
            checkState(raf, fch, 256, 42);

            fch.truncate(42);
            checkState(raf, fch, 42, 42);

            fch.truncate(0);
            checkState(raf, fch, 0, 0);
        }
    }
}
