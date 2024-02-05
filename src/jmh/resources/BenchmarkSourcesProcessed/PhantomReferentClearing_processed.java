/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8071507
 * @summary Test that PhantomReferences are cleared when notified.
 * @run main/othervm PhantomReferentClearing
 */

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.List;

public class PhantomReferentClearing {

    private static final long ENQUEUE_TIMEOUT = 1000;   


    private static final ReferenceQueue<Object> Q1 = new ReferenceQueue<>();
    private static final ReferenceQueue<Object> Q2 = new ReferenceQueue<>();

    private static volatile Object O2 = new Object();
    private static volatile List<Object> O1 = new ArrayList<>();
    static {
        O1.add(O2);
    }

    private static final PhantomReference<Object> P1 = new PhantomReference<>(O1, Q1);
    private static final PhantomReference<Object> P2 = new PhantomReference<>(O2, Q2);

    public static void main(String[] args) throws InterruptedException {

        System.gc();
        if (Q1.remove(ENQUEUE_TIMEOUT) != null) {
            throw new RuntimeException("P1 already notified");
        } else if (Q2.poll() != null) {
            throw new RuntimeException("P2 already notified");
        }

        O1 = null;
        System.gc();
        if (Q1.remove(ENQUEUE_TIMEOUT) == null) {
            throw new RuntimeException("P1 not notified by O1 deletion");
        } else if (Q2.remove(ENQUEUE_TIMEOUT) != null) {
            throw new RuntimeException("P2 notified by O1 deletion.");
        }

        O2 = null;
        System.gc();
        if (Q2.remove(ENQUEUE_TIMEOUT) == null) {
            throw new RuntimeException("P2 not notified by O2 deletion");
        }
    }
}
