/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @test id=Xcomp
 * @bug 8328702
 * @summary Check that SubTypeCheckNode is properly folded when having an array with bottom type elements checked
 *          against an interface.
 *
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.types.TestSubTypeCheckWithBottomArray::test*
 *                   -XX:CompileCommand=inline,compiler.types.TestSubTypeCheckWithBottomArray::check*
 *                   compiler.types.TestSubTypeCheckWithBottomArray
 */

/*
 * @test id=Xbatch
 * @bug 8328702
 * @summary Check that SubTypeCheckNode is properly folded when having an array with bottom type elements checked
 *          against an interface.
 *
 * @run main/othervm -Xbatch -XX:CompileCommand=compileonly,compiler.types.TestSubTypeCheckWithBottomArray::test*
 *                   -XX:CompileCommand=inline,compiler.types.TestSubTypeCheckWithBottomArray::check*
 *                   compiler.types.TestSubTypeCheckWithBottomArray
 */

/*
 * @test id=stress
 * @bug 8328702
 * @summary Check that PartialSubtypeCheckNode is properly folded when having an array with bottom type elements checked
 *          either against an interface or an unrelated non-sub-class.
 *
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.types.TestSubTypeCheckWithBottomArray::test*
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+ExpandSubTypeCheckAtParseTime
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressReflectiveCode
 *                   -XX:CompileCommand=inline,compiler.types.TestSubTypeCheckWithBottomArray::check*
 *                   compiler.types.TestSubTypeCheckWithBottomArray
 * @run main/othervm -Xbatch -XX:CompileCommand=compileonly,compiler.types.TestSubTypeCheckWithBottomArray::test*
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+ExpandSubTypeCheckAtParseTime
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressReflectiveCode
 *                   -XX:CompileCommand=inline,compiler.types.TestSubTypeCheckWithBottomArray::check*
 *                   compiler.types.TestSubTypeCheckWithBottomArray
 */

package compiler.types;

public class TestSubTypeCheckWithBottomArray {
    static byte[] bArr = new byte[10];
    static Object[] oArr = new Object[10];
    static boolean flag;

    public static void main(String[] args) {
        A a = new A();
        B b = new B();
        Y y = new Y();
        Z z = new Z();
        for (int i = 0; i < 10000; i++) {
            checkInterface(a); 
            checkInterface(b); 
            testInterface();

            checkClass(y); 
            checkClass(z); 
            testClass();
            flag = !flag;
        }
    }

    static void testInterface() {
        checkInterface(flag ? bArr : oArr); 
    }

    static void checkInterface(Object o) {
        if (o instanceof I i) {
            i.getClass();
        }
    }

    static void testClass() {
        checkClass(flag ? bArr : oArr); 
    }

    static void checkClass(Object o) {
        if (o instanceof X x) {
            x.getClass();
        }
    }

}

interface I {}
class A implements I {}
class B implements I {}

class X {}
class Y extends X {}
class Z extends X {}
