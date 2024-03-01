/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/gctests/StringInternSync.
 * VM Testbase keywords: [gc, stress, stressopt, feature_perm_removal_jdk7, nonconcurrent]
 * VM Testbase readme:
 * The test verifies that String.intern is correctly synchronized.
 * Test interns same strings in different threads and verifies that all interned equal
 * strings are same objects.
 * This test interns a few large strings.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      -XX:+UnlockDiagnosticVMOptions
 *      -XX:+VerifyStringTableAtExit
 *      gc.gctests.StringInternSync.StringInternSync
 *      -ms low
 */

package gc.gctests.StringInternSync;

import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import nsk.share.TestBug;
import nsk.share.TestFailure;
import nsk.share.gc.*;
import nsk.share.gc.gp.MemoryStrategy;
import nsk.share.gc.gp.MemoryStrategyAware;
import nsk.share.gc.gp.string.RandomStringProducer;

public class StringInternSync extends ThreadedGCTest implements MemoryStrategyAware {

    static final List<String> stringsToIntern = new ArrayList();
    static final List<List<String>> internedStrings = new ArrayList<List<String>>();
    long sizeOfAllInteredStrings = 0;
    int maxStringSize;
    RandomStringProducer gp = new RandomStringProducer();
    MemoryStrategy memoryStrategy;
    ReadWriteLock rwlock = new ReentrantReadWriteLock();

    @Override
    public void setMemoryStrategy(MemoryStrategy memoryStrategy) {
        this.memoryStrategy = memoryStrategy;
    }

    private class StringGenerator implements Runnable {

        List<String> internedLocal;

        public StringGenerator(List<String> internedLocal) {
            this.internedLocal = internedLocal;
        }

        public void run() {
            try {
                rwlock.readLock().lock();
                internedLocal.clear();
                for (String str : stringsToIntern) {
                    internedLocal.add(new String(str).intern());
                }
            } finally {
                rwlock.readLock().unlock();
            }

            if (internedLocal == internedStrings.get(0)) {
                try {
                    rwlock.writeLock().lock();
                    List<String> interned = internedStrings.get(0);

                    for (List<String> list : internedStrings) {
                        if (list == interned) {
                            continue;
                        }
                        if (list.size() == 0) {
                            continue; 
                        }

                        if (list.size() != interned.size()) {
                            throw new TestFailure("Size of interned string list differ from origial."
                                    + " interned " + list.size() + " original " + interned.size());
                        }
                        for (int i = 0; i < interned.size(); i++) {
                            String str = interned.get(i);
                            if (!str.equals(list.get(i))) {
                                throw new TestFailure("The interned strings are not the equals.");
                            }
                            if (str != list.get(i)) {
                                throw new TestFailure("The equal interned strings are not the same.");
                            }
                        }
                        list.clear();

                    }
                    interned.clear();
                    stringsToIntern.clear();
                    for (long currentSize = 0; currentSize <= sizeOfAllInteredStrings; currentSize++) {
                        stringsToIntern.add(gp.create(maxStringSize));
                        currentSize += maxStringSize;
                    }
                } finally {
                    rwlock.writeLock().unlock();
                }
            }
        }
    }

    @Override
    public void run() {
        sizeOfAllInteredStrings = 10 * 1024 * 1024; 
        maxStringSize = (int) (sizeOfAllInteredStrings / memoryStrategy.getSize(sizeOfAllInteredStrings));
        log.debug("The overall size of interned strings  : " + sizeOfAllInteredStrings / (1024 * 1024) + "M");
        log.debug("The count of interned strings : " + sizeOfAllInteredStrings / maxStringSize);
        for (long currentSize = 0; currentSize <= sizeOfAllInteredStrings; currentSize++) {
            stringsToIntern.add(gp.create(maxStringSize));
            currentSize += maxStringSize;
        }
        super.run();
    }

    @Override
    protected Runnable createRunnable(int i) {
        ArrayList list = new ArrayList();
        internedStrings.add(list);
        return new StringGenerator(list);
    }

    public static void main(String[] args) {
        GC.runTest(new StringInternSync(), args);
    }
}
