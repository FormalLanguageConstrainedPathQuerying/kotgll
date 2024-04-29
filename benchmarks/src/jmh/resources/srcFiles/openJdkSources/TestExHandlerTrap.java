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
package compiler.c2;

import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

/*
 * @test id=default_config
 * @bug 8267532
 * @summary Test whether trap in place of pruned exception handler block works
 *
 * @run main/othervm
 *   -Xbatch
 *   -Xlog:deoptimization=trace
 *   -XX:CompileCommand=PrintCompilation,compiler.c2.TestExHandlerTrap::payload
 *   -XX:CompileCommand=dontinline,compiler.c2.TestExHandlerTrap::payload
 *   -XX:CompileCommand=dontinline,compiler.c2.TestExHandlerTrap::maybeThrow
 *   compiler.c2.TestExHandlerTrap
 */

/*
 * @test id=no_profiling
 * @bug 8267532
 * @summary basic smoke test for disabled ex. handler profiling
 *
 * @run main/othervm
 *   -Xbatch
 *   -Xlog:deoptimization=trace
 *   -XX:CompileCommand=PrintCompilation,compiler.c2.TestExHandlerTrap::payload
 *   -XX:CompileCommand=dontinline,compiler.c2.TestExHandlerTrap::payload
 *   -XX:CompileCommand=dontinline,compiler.c2.TestExHandlerTrap::maybeThrow
 *   -XX:-ProfileExceptionHandlers
 *   compiler.c2.TestExHandlerTrap
 */

/*
 * @test id=stress
 * @bug 8267532
 * @summary basic smoke test for stressing ex. handler pruning
 * @requires vm.debug
 *
 * @run main/othervm
 *   -Xbatch
 *   -Xlog:deoptimization=trace
 *   -XX:CompileCommand=PrintCompilation,compiler.c2.TestExHandlerTrap::payload
 *   -XX:CompileCommand=dontinline,compiler.c2.TestExHandlerTrap::payload
 *   -XX:CompileCommand=dontinline,compiler.c2.TestExHandlerTrap::maybeThrow
 *   -XX:+StressPrunedExceptionHandlers
 *   compiler.c2.TestExHandlerTrap
 */

public class TestExHandlerTrap {

    private static final String EX_MESSAGE = "Testing trap";

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 20_000; i++) {
            payload(false);
        }

        try {
            payload(true);
        } catch (IllegalStateException e) {
            if (!e.getMessage().equals(EX_MESSAGE)) {
                throw e;
            }
        }

        for (int i = 0; i < 1_000; i++) {
            payload(false);
        }
    }

    public static void payload(boolean shouldThrow) {
        doIt(shouldThrow); 
    }

    public static void doIt(boolean shouldThrow) {
        PrintStream err = System.err;
        try (ConfinedScope r = new ConfinedScope()) {
            r.addCloseAction(dummy);
            maybeThrow(shouldThrow); 
        } catch (IllegalArgumentException e) {
            err.println("Exception message: " + e.getMessage());
            err.println("shouldThrow: " + shouldThrow);
        }
    }

    private static void maybeThrow(boolean shouldThrow) {
        if (shouldThrow) {
            throw new IllegalStateException(EX_MESSAGE);
        }
    }

    static final Runnable dummy = () -> {};

    static class ConfinedScope implements AutoCloseable {
        final Thread owner;
        boolean closed;
        final List<Runnable> resources = new ArrayList<>();

        private void checkState() {
            if (closed) {
                throw new AssertionError("Closed");
            } else if (owner != Thread.currentThread()) {
                throw new AssertionError("Wrong thread");
            }
        }

        ConfinedScope() {
            this.owner = Thread.currentThread();
        }

        void addCloseAction(Runnable runnable) {
            checkState();
            resources.add(runnable);
        }

        public void close() {
            checkState();
            closed = true;
            for (Runnable r : resources) {
                r.run();
            }
        }
    }
}
