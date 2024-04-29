/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4414990 4415041
 * @summary Checks that the output is flushed properly when using various
 *          ImageOutputStreams and writers
 */

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

public class StreamFlush {

    public static void main(String[] args) throws IOException {
        ImageIO.setUseCache(true);

        File temp1 = File.createTempFile("StreamFlush_fis_", ".tmp");
        File temp2 = File.createTempFile("StreamFlush_bos_", ".tmp");
        try (ImageOutputStream fios = ImageIO.createImageOutputStream(temp1);
             FileOutputStream fos2 = new FileOutputStream(temp2)) {
            test(temp1, fios, temp2, fos2);
        } finally {
            Files.delete(Paths.get(temp1.getAbsolutePath()));
            Files.delete(Paths.get(temp2.getAbsolutePath()));
        }
    }

    private static void test(File temp1, ImageOutputStream fios, File temp2,
                             FileOutputStream fos2) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(fos2);
        ImageOutputStream fcios1 = ImageIO.createImageOutputStream(bos);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream fcios2 = ImageIO.createImageOutputStream(baos);

        BufferedImage bi =
            new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);

        ImageIO.write(bi, "jpg", fios); 
        ImageIO.write(bi, "png", fcios1); 
        ImageIO.write(bi, "jpg", fcios2); 


        long file1NoFlushLength = temp1.length();
        fios.flush();
        long file1FlushLength = temp1.length();

        long file2NoFlushLength = temp2.length();
        fcios1.flush();
        bos.flush();
        long file2FlushLength = temp2.length();

        byte[] b0 = baos.toByteArray();
        int cacheNoFlushLength = b0.length;
        fcios2.flush();
        byte[] b1 = baos.toByteArray();
        int cacheFlushLength = b1.length;

        if (file1NoFlushLength != file1FlushLength) {
            System.out.println
                ("FileImageOutputStream not flushed!");
        }

        if (file2NoFlushLength != file2FlushLength) {
            System.out.println
             ("FileCacheImageOutputStream/BufferedOutputStream not flushed!");
        }

        if (cacheNoFlushLength != cacheFlushLength) {
            System.out.println
            ("FileCacheImageOutputStream/ByteArrayOutputStream not flushed!");
        }
    }
}
