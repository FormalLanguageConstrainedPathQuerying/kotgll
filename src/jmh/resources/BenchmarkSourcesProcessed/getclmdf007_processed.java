/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetClassModifiers;

import java.io.PrintStream;

public class getclmdf007 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("getclmdf007");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getclmdf007 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void check(int i, Class arr, Class comp);
    native static int getRes();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        check(0, new Test0[1].getClass(), Test0.class);
        check(1, new Test1[1].getClass(), Test1.class);
        check(2, new Test2[1].getClass(), Test2.class);
        check(3, new Test3[1].getClass(), Test3.class);
        check(4, new Test4[1].getClass(), Test4.class);
        check(5, new Test5[1].getClass(), Test5.class);
        check(6, new Test6[1].getClass(), Test6.class);
        check(7, new Test7[1].getClass(), Test7.class);
        return getRes();
    }

    class Test0 {}
    public class Test1 {}
    private class Test2 {}
    protected class Test3 {}
    interface Test4 {}
    public interface Test5 {}
    private interface Test6 {}
    protected interface Test7 {}
}
