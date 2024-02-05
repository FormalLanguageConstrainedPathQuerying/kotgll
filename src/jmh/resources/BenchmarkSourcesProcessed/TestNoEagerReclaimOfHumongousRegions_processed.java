/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * @test TestNoEagerReclaimOfHumongousRegions
 * @bug 8139424
 * @summary Test to check that a live humongous object is not eagerly reclaimed. This is a regression test for
 *          8139424 and the test will crash if an eager reclaim occur. The test is not 100% deterministic and
 *          might pass even if there are problems in the code, but it will never crash unless there is a problem.
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xlog:gc,gc+humongous=debug -XX:+UseG1GC -XX:MaxTenuringThreshold=0 -XX:+UnlockExperimentalVMOptions -XX:G1RemSetArrayOfCardsEntries=32 -XX:G1HeapRegionSize=1m -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI gc.g1.TestNoEagerReclaimOfHumongousRegions
 */

import java.util.LinkedList;

import jdk.test.whitebox.WhiteBox;

public class TestNoEagerReclaimOfHumongousRegions {
    static class LargeRef {
        private byte[] _ref;

        LargeRef(byte[] ref) {
            _ref = ref;
        }

        byte[] ref() { return _ref; }
    }

    static LargeRef humongous_reference_holder;

    public static void main(String[] args) throws InterruptedException{
        WhiteBox wb = WhiteBox.getWhiteBox();
        LinkedList<Object> garbageAndRefList = new LinkedList<Object>();
        humongous_reference_holder = new LargeRef(new byte[1 * 1024 * 1024]);

        for (int i = 0; i < 32; i++) {
            garbageAndRefList.add(new byte[400*1000]);
            garbageAndRefList.add(new LargeRef(humongous_reference_holder.ref()));

            wb.youngGC();
        }
        garbageAndRefList.clear();

        wb.g1RunConcurrentGC();

        wb.youngGC();
        wb.fullGC();
    }
}
