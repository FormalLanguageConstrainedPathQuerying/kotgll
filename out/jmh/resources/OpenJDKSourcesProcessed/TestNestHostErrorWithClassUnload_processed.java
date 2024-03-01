/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test the ability to safely unload a class that has an error
 *          with its designated nest host. The nest host class must resolve
 *          successfully but fail validation. This tests a specific, otherwise
 *          untested, code path in ResolutionErrorTable::free_entry.
 *
 * @library /test/lib
 * @compile TestNestHostErrorWithClassUnload.java
 *          Helper.java
 *          PackagedNestHost.java
 *          PackagedNestHost2.java
 * @compile PackagedNestHost2Member.jcod

 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xlog:class+unload=trace TestNestHostErrorWithClassUnload
 */


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import jdk.test.lib.classloader.ClassUnloadCommon;

public class TestNestHostErrorWithClassUnload {

    static final MethodType INVOKE_T = MethodType.methodType(void.class);

    public static void main(String[] args) throws Throwable {
        ClassLoader cl = ClassUnloadCommon.newClassLoader();
        Class<?> c = cl.loadClass("Helper");
        MethodHandle mh = MethodHandles.lookup().findStatic(c, "test", INVOKE_T);
        mh.invoke();
        mh = null;
        c = null;
        cl = null;
        ClassUnloadCommon.triggerUnloading();
    }
}
