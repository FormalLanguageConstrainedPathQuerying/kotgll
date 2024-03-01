/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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
 * @summary Check that the verbose message of ICCE is printed correctly.
 *          The test forces errors in vtable stubs and interpreter.
 * @requires !(os.arch=="arm") & vm.flavor == "server" & !vm.emulatedClient & vm.compMode=="Xmixed" & (!vm.graal.enabled | vm.opt.TieredCompilation == true) & (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel==4)
 * @library /test/lib /
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @compile IncompatibleClassChangeErrorTest.java
 * @compile ImplementsSomeInterfaces.jasm ICC2_B.jasm ICC3_B.jasm ICC4_B.jasm
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileThreshold=1000 -XX:-BackgroundCompilation -XX:-Inline
 *                   -XX:CompileCommand=exclude,test.IncompatibleClassChangeErrorTest::test_iccInt
 *                   test.IncompatibleClassChangeErrorTest
 */

package test;

import jdk.test.whitebox.WhiteBox;
import compiler.whitebox.CompilerWhiteBoxTest;
import java.lang.reflect.Method;

public class IncompatibleClassChangeErrorTest {

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    private static boolean enableChecks = true;

    private static String expectedErrorMessageInterpreted =
        "Class test.ImplementsSomeInterfaces " +
        "does not implement the requested interface test.InterfaceICCE1";
    private static String expectedErrorMessageCompiled =
        "Class test.ICC2_B does not implement the requested interface test.ICC2_iB";


    private static boolean compile(Class<?> clazz, String name) {
        try {
            Method method = clazz.getMethod(name);
            boolean enqueued = WHITE_BOX.enqueueMethodForCompilation(method, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
            if (!enqueued) {
                System.out.println("Warning: Blocking compilation failed for " + clazz.getName() + "." + name + " (timeout?)");
                return false;
            } else if (!WHITE_BOX.isMethodCompiled(method)) {
                throw new RuntimeException(clazz.getName() + "." + name + " is not compiled");
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(clazz.getName() + "." + name + " not found", e);
        }
        return true;
    }

    public static boolean setup_test() {
        new AbstractMethodError();
        new IncompatibleClassChangeError();

        enableChecks = false;
        System.out.println("warmup:");
        test_iccInt();
        test_icc_compiled_itable_stub();
        enableChecks = true;

        if (!compile(IncompatibleClassChangeErrorTest.class, "test_icc_compiled_itable_stub") ||
            !compile(ICC2_C.class, "b") ||
            !compile(ICC2_D.class, "b") ||
            !compile(ICC2_E.class, "b")) {
          return false;
        }

        System.out.println("warmup done.");
        return true;
    }

    public static void test_iccInt() {
        boolean caught_icc = false;
        try {
            InterfaceICCE1 objectInterface = new ImplementsSomeInterfaces();
            objectInterface.aFunctionOfMyInterface();
        } catch (IncompatibleClassChangeError e) {
            String errorMsg = e.getMessage();
            if (enableChecks && !errorMsg.equals(expectedErrorMessageInterpreted)) {
                System.out.println("Expected: " + expectedErrorMessageInterpreted + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of IncompatibleClassChangeError.");
            }
            if (enableChecks) {
                System.out.println("Test 1 passed with message: " + errorMsg);
            }
            caught_icc = true;
        } catch (Throwable e) {
            throw new RuntimeException("Caught unexpected exception: " + e);
        }

        if (!caught_icc) {
            throw new RuntimeException("Expected IncompatibleClassChangeError was not thrown.");
        }
    }

    public static void test_icc_compiled_itable_stub() {
        boolean caught_icc = false;
        ICC2_B b = new ICC2_B();
        ICC2_C c = new ICC2_C();
        ICC2_D d = new ICC2_D();
        ICC2_E e = new ICC2_E();
        b.a();
        c.a();
        d.a();
        e.a();

        try {
            final int iterations = 10;
            for (int i = 0; i < iterations; i++) {
                ICC2_iB a = b;
                if (i % 3 == 0 && i < iterations - 1) {
                    a = c;
                }
                if (i % 3 == 1 && i < iterations - 1) {
                    a = d;
                }
                if (i % 3 == 2 && i < iterations - 1) {
                    a = e;
                }
                a.b();
            }
        } catch (AbstractMethodError exc) {
            System.out.println();
            System.out.println(exc);
            if (enableChecks) {
                String errorMsg = exc.getMessage();
                if (errorMsg == null) {
                    throw new RuntimeException("Caught unexpected AbstractMethodError with empty message.");
                }
                throw new RuntimeException("Caught unexpected AbstractMethodError.");
            }
        } catch (IncompatibleClassChangeError exc) {
            caught_icc = true;
            System.out.println();
            String errorMsg = exc.getMessage();
            if (enableChecks && errorMsg == null) {
                System.out.println(exc);
                throw new RuntimeException("Empty error message of IncompatibleClassChangeError.");
            }
            if (enableChecks &&
                !errorMsg.equals(expectedErrorMessageCompiled)) {
                System.out.println("Expected: " + expectedErrorMessageCompiled + "\n" +
                                   "but got:  " + errorMsg);
                System.out.println(exc);
                throw new RuntimeException("Wrong error message of IncompatibleClassChangeError.");
            }
            if (enableChecks) {
                System.out.println("Test 2 passed with message: " + errorMsg);
            }
        } catch (Throwable exc) {
            throw exc; 
        }

        if (enableChecks && !caught_icc) {
            throw new RuntimeException("Expected IncompatibleClassChangeError was not thrown.");
        }
    }

    private static String expectedErrorMessage3 =
        "class test.ICC3_B can not implement test.ICC3_A, because it is not an interface (test.ICC3_A is in unnamed module of loader 'app')";

    public static void test3_implementsClass() throws Exception {
        try {
            new ICC3_B();
            throw new RuntimeException("Expected IncompatibleClassChangeError was not thrown.");
        } catch (IncompatibleClassChangeError e) {
            String errorMsg = e.getMessage();
            if (!errorMsg.equals(expectedErrorMessage3)) {
                System.out.println("Expected: " + expectedErrorMessage3 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of IncompatibleClassChangeError.");
            }
            System.out.println("Test 3 passed with message: " + errorMsg);
        } catch (Throwable e) {
            throw new RuntimeException("Caught unexpected exception: " + e);
        }
    }

    private static String expectedErrorMessage4 =
        "class test.ICC4_B has interface test.ICC4_iA as super class";

    public static void test4_extendsInterface() throws Exception {
        try {
            new ICC4_B();
            throw new RuntimeException("Expected IncompatibleClassChangeError was not thrown.");
        } catch (IncompatibleClassChangeError e) {
            String errorMsg = e.getMessage();
            if (!errorMsg.equals(expectedErrorMessage4)) {
                System.out.println("Expected: " + expectedErrorMessage4 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of IncompatibleClassChangeError.");
            }
            System.out.println("Test 4 passed with message: " + errorMsg);
        } catch (Throwable e) {
            throw new RuntimeException("Caught unexpected exception: " + e);
        }
    }

    public static void main(String[] args) throws Exception {
        if (!setup_test()) {
            return;
        }
        test_iccInt();
        test_icc_compiled_itable_stub();
        test3_implementsClass();
        test4_extendsInterface();
    }
}





interface InterfaceICCE0 {
    public String firstFunctionOfMyInterface0();
    public String secondFunctionOfMyInterface0();
}

interface InterfaceICCE1 {

    public String firstFunctionOfMyInterface();

    public String secondFunctionOfMyInterface();

    public String aFunctionOfMyInterface();
}

abstract class AbstractICCE0 implements InterfaceICCE0 {
    abstract public String firstAbstractMethod();
    abstract public String secondAbstractMethod();

    abstract public String anAbstractMethod();
}

class ImplementsSomeInterfaces extends
        AbstractICCE0
    implements InterfaceICCE1
{

    public String firstAbstractMethod() {
        return this.getClass().getName();
    }

    public String secondAbstractMethod() {
        return this.getClass().getName();
    }

    public String anAbstractMethod() {
        return this.getClass().getName();
    }

    public String firstFunctionOfMyInterface0() {
        return this.getClass().getName();
    }

    public String secondFunctionOfMyInterface0() {
        return this.getClass().getName();
    }

    public String firstFunctionOfMyInterface() {
        return this.getClass().getName();
    }

    public String secondFunctionOfMyInterface() {
        return this.getClass().getName();
    }

    public String aFunctionOfMyInterface() {
        return this.getClass().getName();
    }
}


interface ICC2_iA {
    public void a();
}

interface ICC2_iB {
    public void b();
}

class ICC2_B implements ICC2_iA,
                       ICC2_iB {
    public void a() {
        System.out.print("B.a() ");
    }

    public void b() {
        System.out.print("B.b() ");
    }
}

class ICC2_C implements ICC2_iA, ICC2_iB {
    public void a() {
        System.out.print("C.a() ");
    }

    public void b() {
        System.out.print("C.b() ");
    }
}

class ICC2_D implements ICC2_iA, ICC2_iB {
    public void a() {
        System.out.print("D.a() ");
    }

    public void b() {
        System.out.print("D.b() ");
    }
}

class ICC2_E implements ICC2_iA, ICC2_iB {
    public void a() {
        System.out.print("E.a() ");
    }

    public void b() {
        System.out.print("E.b() ");
    }
}


class ICC3_A {
}

class ICC3_B extends ICC3_A {
}


interface ICC4_iA {
}

class ICC4_B implements ICC4_iA {
}
