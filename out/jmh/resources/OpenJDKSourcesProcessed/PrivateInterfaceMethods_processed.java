/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8081800 8165827
 * @summary Add JNI invocation tests for private interface methods
 * @run main/native PrivateInterfaceMethods
 */

public class PrivateInterfaceMethods {

    static {
        System.loadLibrary("PrivateInterfaceMethods");
    }

    static native int callIntVoid(Object target, String definingClassName, String methodName, boolean virtual);
    static native void lookupIntVoid(String definingClassName, String methodName);

    static interface A {
        static final int AmResult = 1;
        private int m() { return AmResult; }
        private int onlyA() { return 0; }
    }

    static interface B extends A {
        private int onlyB() { return 0; }
    }

    static interface C extends B {
        static final int CmResult = 2;
        private int m() { return CmResult; }  
        private int onlyC() { return 0; }
    }

    public static class Impl implements C {
        static final int ImplmResult = 3;
        private int m() { return ImplmResult; } 
    }


    public static class Impl2 implements C {
    }

    public static void main(String[] args) {


        lookup(A.class.getName(), "onlyA", null); 
        lookup(B.class.getName(), "onlyA", NoSuchMethodError.class); 
        lookup(C.class.getName(), "onlyA", NoSuchMethodError.class); 
        lookup(Impl.class.getName(), "onlyA", NoSuchMethodError.class); 
        lookup(Impl2.class.getName(), "onlyA", NoSuchMethodError.class); 

        lookup(B.class.getName(), "onlyB", null); 
        lookup(A.class.getName(), "onlyB", NoSuchMethodError.class); 
        lookup(C.class.getName(), "onlyB", NoSuchMethodError.class); 
        lookup(Impl.class.getName(), "onlyB", NoSuchMethodError.class); 
        lookup(Impl2.class.getName(), "onlyB", NoSuchMethodError.class); 

        lookup(C.class.getName(), "onlyC", null); 
        lookup(A.class.getName(), "onlyC", NoSuchMethodError.class); 
        lookup(B.class.getName(), "onlyC", NoSuchMethodError.class); 
        lookup(Impl.class.getName(), "onlyC", NoSuchMethodError.class); 
        lookup(Impl2.class.getName(), "onlyC", NoSuchMethodError.class); 

        Impl impl = new Impl();


        test(impl, A.class.getName(), "m", A.AmResult, true, null);
        test(impl, A.class.getName(), "m", A.AmResult, false, null);

        test(impl, B.class.getName(), "m", -1, true, NoSuchMethodError.class);
        test(impl, B.class.getName(), "m", -1, false, NoSuchMethodError.class);

        test(impl, C.class.getName(), "m", C.CmResult, true, null);
        test(impl, C.class.getName(), "m", C.CmResult, false, null);

        test(impl, Impl.class.getName(), "m", Impl.ImplmResult, true, null);
        test(impl, Impl.class.getName(), "m", Impl.ImplmResult, false, null);


        Impl2 impl2 = new Impl2();

        test(impl2, A.class.getName(), "m", A.AmResult, true, null);
        test(impl2, A.class.getName(), "m", A.AmResult, false, null);

        test(impl2, B.class.getName(), "m", -1, true, NoSuchMethodError.class);
        test(impl2, B.class.getName(), "m", -1, false, NoSuchMethodError.class);

        test(impl2, C.class.getName(), "m", C.CmResult, true, null);
        test(impl2, C.class.getName(), "m", C.CmResult, false, null);

        test(impl2, Impl2.class.getName(), "m", -1, true, NoSuchMethodError.class);
        test(impl2, Impl2.class.getName(), "m", -1, false, NoSuchMethodError.class);
    }

    static void lookup(String definingClass, String method, Class<?> expectedException) {

        String desc = "Lookup of " + definingClass + "." + method;
        try {
            lookupIntVoid(definingClass, method);
            if (expectedException != null)
                throw new Error(desc + " succeeded - but expected exception "
                                + expectedException.getSimpleName());
            System.out.println(desc + " - passed");
        }
        catch (Throwable t) {
           if (t.getClass() != expectedException)
               throw new Error(desc + " failed: got exception " + t + " but expected exception "
                               + expectedException.getSimpleName());
           else
              System.out.println(desc + " threw " + expectedException.getSimpleName() + " as expected");
        }
    }

    static void test(Object target, String definingClass, String method,
                     int expected, boolean virtual, Class<?> expectedException) {

        String desc = (virtual ? "Virtual" : "Nonvirtual") + " Invocation of " +
                       definingClass + "." + method + " on instance of class " +
                       target.getClass().getName();
        try {
            int res = callIntVoid(target, definingClass, method, virtual);
            if (expectedException != null)
                throw new Error(desc + " succeeded - but expected exception "
                                + expectedException.getSimpleName());
            if (res != expected)
                throw new Error(desc + " got wrong result: " + res + " instead of " + expected);
            System.out.println(desc + " - passed");
        }
        catch (Throwable t) {
           if (t.getClass() != expectedException)
               throw new Error(desc + " failed: got exception " + t + " but expected exception "
                               + expectedException.getSimpleName());
           else
              System.out.println(desc + " threw " + expectedException.getSimpleName() + " as expected");
        }
    }

}
