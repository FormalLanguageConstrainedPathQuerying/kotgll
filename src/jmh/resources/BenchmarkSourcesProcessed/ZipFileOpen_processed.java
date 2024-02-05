/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.bench.java.util.zip;

import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Simple benchmark measuring cost of opening zip files, parsing CEN
 * entries.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class ZipFileOpen {

    @Param({"512", "1024"})
    private int size;

    public File zipFile;
    public File relativePathFile;

    @Setup(Level.Trial)
    public void beforeRun() throws IOException {
        File tempFile = Files.createTempFile("zip-micro", ".zip").toFile();
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            String[] dirPrefixes = new String[] { "dir1", "dir2", "dir3",
                    "longer-directory-name-", "ridiculously-long-pathname-to-help-exercize-vectorized-subroutines-"};
            String[] entryPrefixes = new String[] { "e", "long-entry-name-",
                    "ridiculously-long-entry-name-to-help-exercize-vectorized-subroutines-"};

            for (int i = 0; i < size; i++) {
                String ename = dirPrefixes[i % dirPrefixes.length] + i + "/";
                zos.putNextEntry(new ZipEntry(ename));

                ename += entryPrefixes[i % entryPrefixes.length] + "-" + i;
                zos.putNextEntry(new ZipEntry(ename));
            }
        }
        zipFile = tempFile;
        relativePathFile = Path.of(System.getProperty("user.dir"))
                                .relativize(zipFile.toPath()).toFile();
    }

    @Benchmark
    public ZipFile openCloseZipFile() throws Exception {
        ZipFile zf = new ZipFile(zipFile);
        zf.close();
        return zf;
    }

    @Benchmark
    public void openCloseZipFilex2() throws Exception {
        ZipFile zf = new ZipFile(zipFile);
        ZipFile zf2 = new ZipFile(relativePathFile);
        zf.close();
        zf2.close();
    }
}
