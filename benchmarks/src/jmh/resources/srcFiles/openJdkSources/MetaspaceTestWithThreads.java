/*
 * Copyright (c) 2021, 2023 SAP SE. All rights reserved.
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

public class MetaspaceTestWithThreads {

    final MetaspaceTestContext context;

    final long testAllocationCeiling;

    final int numThreads;

    final int seconds;

    RandomAllocatorThread threads[];

    public MetaspaceTestWithThreads(MetaspaceTestContext context, long testAllocationCeiling, int numThreads, int seconds) {
        this.context = context;
        this.testAllocationCeiling = testAllocationCeiling;
        this.numThreads = numThreads;
        this.seconds = seconds;
        this.threads = new RandomAllocatorThread[numThreads];
    }

    protected void stopAllThreads() throws InterruptedException {
        for (Thread t: threads) {
            t.interrupt();
        }
        for (Thread t: threads) {
            t.join();
        }
    }

    void destroyArenasAndPurgeSpace() {


        for (RandomAllocatorThread t: threads) {
            if (t.allocator.arena.isLive()) {
                context.destroyArena(t.allocator.arena);
            }
        }
        context.purge();

        context.checkStatistics();

        if (context.committedWords() > 0) {
            throw new RuntimeException("Expected no committed words after purging empty metaspace context (was: " + context.committedWords() + ")");
        }
    }

    @Override
    public String toString() {
        return "commitLimit=" + context.commitLimit +
                ", reserveLimit=" + context.reserveLimit +
                ", testAllocationCeiling=" + testAllocationCeiling +
                ", num_allocators=" + numThreads +
                ", seconds=" + seconds;
    }

}
