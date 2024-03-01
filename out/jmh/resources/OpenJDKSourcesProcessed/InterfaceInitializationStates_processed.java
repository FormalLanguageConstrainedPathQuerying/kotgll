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
 *
 */

/*
 * @test
 * @bug 8163969
 * @summary Test interface initialization states and when certain interfaces are initialized
 * in the presence of initialization errors.
 * @run main InterfaceInitializationStates
 */

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class InterfaceInitializationStates {

    static List<Class<?>> cInitOrder = new ArrayList<>();

    interface K {
        boolean v = InterfaceInitializationStates.out(K.class);
        static final Object CONST = InterfaceInitializationStates.someMethod();
        default int method() { return 2; }
    }

    interface I extends K {
        boolean v = InterfaceInitializationStates.out(I.class);
        static final Object CONST = InterfaceInitializationStates.someMethod();
    }

    interface L extends K {
        boolean v = InterfaceInitializationStates.out(L.class);
        default void lx() {}
        static void func() {
            System.out.println("Calling function on interface with bad super interface.");
        }
    }

    interface M {
        boolean v = InterfaceInitializationStates.out(M.class);
        default void mx() {}
    }

    static class ClassLIM implements L, I, M {
        boolean v = InterfaceInitializationStates.out(ClassLIM.class);
        int callMethodInK() { return method(); }
        static {
            System.out.println("Initializing C, but L is still good");
            L.func();
        }
    }

    static class ClassM implements M {
        boolean v = InterfaceInitializationStates.out(ClassM.class);
    }

    interface Iunlinked extends K {
        boolean v = InterfaceInitializationStates.out(Iunlinked.class);
    }

    interface Iparams {
        boolean v = InterfaceInitializationStates.out(Iparams.class);
        K the_k = null;
        K m(K k); 
        default K method() { return new K(){}; }
    }

    static class ClassIparams implements Iparams {
        boolean v = InterfaceInitializationStates.out(ClassIparams.class);
        public K m(K k) { return k; }
    }

    public static void main(java.lang.String[] unused) {


        boolean v = L.v;
        L.func();

        try {
            ClassLIM c  = new ClassLIM();  
            throw new RuntimeException("FAIL exception not thrown for class");
        } catch (ExceptionInInitializerError e) {
            System.out.println("ExceptionInInitializerError thrown as expected");
        }

        try {
            Class.forName("InterfaceInitializationStates$K", true, InterfaceInitializationStates.class.getClassLoader());
            throw new RuntimeException("FAIL exception not thrown for forName(K)");
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("ClassNotFoundException should not be thrown");
        } catch(NoClassDefFoundError e) {
            System.out.println("NoClassDefFoundError thrown as expected");
        }

        new ClassM();

        try {
            Object ii = I.CONST;
            throw new RuntimeException("FAIL exception not thrown for I's initialization");
        } catch (ExceptionInInitializerError e) {
            System.out.println("ExceptionInInitializerError as expected");
        }

        boolean bb = Iunlinked.v;

        boolean value = Iparams.v;
        System.out.println("value is " + value);

        ClassIparams p = new ClassIparams();
        try {
            K kk = p.method();
            throw new RuntimeException("FAIL exception not thrown for calling method for K");
        } catch(NoClassDefFoundError e) {
            System.out.println("NoClassDefFoundError thrown as expected");
        }

        List<Class<?>> expectedCInitOrder = Arrays.asList(L.class, K.class, M.class, ClassM.class,
                                                          I.class, Iunlinked.class, Iparams.class,
                                                          ClassIparams.class);
        if (!cInitOrder.equals(expectedCInitOrder)) {
            throw new RuntimeException(
                String.format("Class initialization array %s not equal to expected array %s",
                              cInitOrder, expectedCInitOrder));
        }
    }

    static boolean out(Class c) {
        System.out.println("#: initializing " + c.getName());
        cInitOrder.add(c);
        return true;
    }
    static Object someMethod() {
        throw new RuntimeException();
    }
}
