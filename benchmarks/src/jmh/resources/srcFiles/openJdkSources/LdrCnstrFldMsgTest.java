/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @compile pkg/Grand.java pkg/Parent.java pkg/ClassLoaderForParentFoo.java
 * @compile pkg/ClassLoaderForChildGrandFoo.java pkg/Child.jasm
 * @run main/othervm LdrCnstrFldMsgTest
 */

import java.lang.reflect.Method;

public class LdrCnstrFldMsgTest {
    public static void main(String... args) throws Exception {
        ClassLoader l = new pkg.ClassLoaderForChildGrandFoo("pkg.Foo", "pkg.Child", "pkg.Grand");
        l.loadClass("pkg.Foo");

        Runnable r = (Runnable) l.loadClass("pkg.Child").newInstance();
        try {
            r.run();
            throw new RuntimeException("Expected LinkageError exception not thrown");
        } catch (java.lang.LinkageError e) {
            if (!e.getMessage().contains("for the field's defining class, pkg.Parent,") ||
                !e.getMessage().contains("have different Class objects for type pkg.Foo")) {
                throw new RuntimeException("Wrong LinkageError exception thrown: " + e.toString());
            }
        }
    }
}
