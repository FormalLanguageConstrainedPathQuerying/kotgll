/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;

import com.sun.management.UnixOperatingSystemMXBean;

import jdk.test.lib.util.FileUtils;

/**
 * @test
 * @bug 8080225
 * @library /test/lib
 * @build jdk.test.lib.util.FileUtils UnreferencedRAFClosesFd
 * @modules java.base/java.io:open
 * @summary Test to ensure that an unclosed and unreferenced RandomAccessFile closes the fd
 * @run main/othervm UnreferencedRAFClosesFd
 */
public class UnreferencedRAFClosesFd {

    static final String FILE_NAME = "empty.txt";

    /* standalone interface */
    public static void main(String argv[]) throws Exception {

        File inFile= new File(System.getProperty("test.dir", "."), FILE_NAME);
        inFile.createNewFile();
        inFile.deleteOnExit();

        FileUtils.listFileDescriptors(System.out);
        long fdCount0 = getFdCount();

        String name = inFile.getPath();
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(name, "rw");
        } catch (FileNotFoundException e) {
            System.out.println("Unexpected exception " + e);
            throw(e);
        }
        FileDescriptor fd = raf.getFD();

        Field fdField = FileDescriptor.class.getDeclaredField("cleanup");
        fdField.setAccessible(true);
        Cleaner.Cleanable cleanup = (Cleaner.Cleanable)fdField.get(fd);

        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        HashSet<Reference<?>> pending = HashSet.newHashSet(3);
        pending.add(new WeakReference<>(cleanup, queue));
        pending.add(new WeakReference<>(raf, queue));
        pending.add(new WeakReference<>(fd, queue));

        Reference<?> r;
        while (((r = queue.remove(10L)) != null)
                || !pending.isEmpty()) {
            System.out.printf("r: %s, pending: %d%n", r, pending.size());
            if (r != null) {
                pending.remove(r);
            } else {
                cleanup = null;
                raf = null;
                fd = null;
                System.gc();  
            }
        }

        Reference.reachabilityFence(cleanup);
        Reference.reachabilityFence(fd);
        Reference.reachabilityFence(raf);
        Reference.reachabilityFence(pending);

        long fdCount = getFdCount();
        if (fdCount != fdCount0) {
            System.out.printf("initial count of open file descriptors: %d%n", fdCount0);
            System.out.printf("final count of open file descriptors: %d%n", fdCount);
            FileUtils.listFileDescriptors(System.out);
        }
    }


    private static long getFdCount() {
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
        return  (mxBean instanceof UnixOperatingSystemMXBean)
                ? ((UnixOperatingSystemMXBean) mxBean).getOpenFileDescriptorCount()
                : -1L;
    }
}
