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

/**
 * @test
 * @bug 8309034
 * @summary Test that when saving a class initialization failure caused by
 *          a StackOverflowError, that we record the SOE as the underlying
 *          cause, even if we can't create the ExceptionInInitializerError
 * @requires os.simpleArch == "x64"
 * @comment The reproducer only fails in the desired way on x64.
 * @requires vm.flagless
 * @comment This test could easily be perturbed so don't allow flag settings.
 *
 * @run main/othervm -Xss160K -Xint TestStackOverflowDuringInit
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestStackOverflowDuringInit {


    static void recurse() {
        try {
            Long.valueOf(1024L);
            recurse();
        } finally {
            Long.valueOf(0);
        }
    }

    public static void main(String[] args) throws Exception {
        String expected = "java.lang.NoClassDefFoundError: Could not initialize class java.lang.Long$LongCache";
        String cause = "Caused by: java.lang.StackOverflowError";

        try {
            recurse();
        } catch (Throwable ex) {
            verify_stack(ex, expected, cause);
        }
    }

    private static void verify_stack(Throwable e, String expected, String cause) throws Exception {
        ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(byteOS)) {
            e.printStackTrace(printStream);
        }
        String stackTrace = byteOS.toString("ASCII");
        System.out.println(stackTrace);
        if (!stackTrace.contains(expected) ||
            (cause != null && !stackTrace.contains(cause))) {
            throw new RuntimeException(expected + " and/or " + cause + " missing from stacktrace");
        }
    }
}
