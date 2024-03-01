/*
 * Copyright (c) 2021 SAP SE. All rights reserved.
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.CyclicBarrier;

import static java.lang.System.currentTimeMillis;

public class MetaspaceTestManyArenasManyThreads extends MetaspaceTestWithThreads {


    public MetaspaceTestManyArenasManyThreads(MetaspaceTestContext context, long testAllocationCeiling, int numThreads, int seconds) {
        super(context, testAllocationCeiling, numThreads, seconds);
    }

    public void runTest() throws Exception {

        long t_start = currentTimeMillis();
        long t_stop = t_start + (seconds * 1000);

        CyclicBarrier gate = new CyclicBarrier(numThreads + 1);

        final long ceilingPerThread = testAllocationCeiling / numThreads;

        for (int i = 0; i < numThreads; i ++) {
            MetaspaceTestArena arena = context.createArena(RandomHelper.fiftyfifty(), ceilingPerThread);
            RandomAllocator allocator = new RandomAllocator(arena);
            RandomAllocatorThread thread = new RandomAllocatorThread(gate, allocator, i);
            threads[i] = thread;
            thread.start();
        }

        gate.await();

        while (System.currentTimeMillis() < t_stop) {

            Thread.sleep(200);

            for (RandomAllocatorThread t: threads) {
                if (t.allocator.arena.numAllocationFailures > 1000) {
                    t.interrupt();
                    t.join();
                    context.destroyArena(t.allocator.arena);

                    MetaspaceTestArena arena = context.createArena(RandomHelper.fiftyfifty(), ceilingPerThread);
                    RandomAllocator allocator = new RandomAllocator(arena);
                    RandomAllocatorThread t2 = new RandomAllocatorThread(null, allocator, t.id);
                    threads[t.id] = t2;
                    t2.start();
                }
            }

        }

        stopAllThreads();

        context.purge();

        context.updateTotals();
        System.out.println("  ## Finished: " + context);

        context.checkStatistics();

        destroyArenasAndPurgeSpace();

        context.destroy();

        context.updateTotals();

        System.out.println("This took " + (System.currentTimeMillis() - t_start) + "ms");

    }

}

