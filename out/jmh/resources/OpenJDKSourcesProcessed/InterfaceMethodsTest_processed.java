/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031195
 * @bug 8071657
 * @bug 8165827
 * @summary  JDI: Add support for static, private and default methods in interfaces
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run build InterfaceMethodsTest
 * @run driver InterfaceMethodsTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class InterfaceMethodsTest extends TestScaffold {
    private static final int RESULT_A = 1;
    private static final int RESULT_B = 2;
    private static final int RESULT_TARGET = 3;

    static interface InterfaceA {
        static int staticMethodA() {
            System.out.println("-InterfaceA: static interface method A-");
            return RESULT_A;
        }
        static int staticMethodB() {
            System.out.println("-InterfaceA: static interface method B-");
            return RESULT_A;
        }
        default int defaultMethodA() {
            System.out.println("-InterfaceA: default interface method A-");
            return RESULT_A;
        }
        default int defaultMethodB() {
            System.out.println("-InterfaceA: default interface method B-");
            return RESULT_A;
        }
        default int defaultMethodC() {
            System.out.println("-InterfaceA: default interface method C-");
            return RESULT_A;
        }
        private int privateMethodA() {
            System.out.println("-InterfaceA: private interface method A-");
            return RESULT_A;
        }
        int implementedMethod();
    }

    static interface InterfaceB extends InterfaceA {
        @Override
        default int defaultMethodC() {
            System.out.println("-InterfaceB: overridden default interface method C-");
            return RESULT_B;
        }
        default int defaultMethodD() {
            System.out.println("-InterfaceB: default interface method D-");
            return RESULT_B;
        }
        static int staticMethodB() {
            System.out.println("-InterfaceB: overridden static interface method B-");
            return RESULT_B;
        }
        static int staticMethodC() {
            System.out.println("-InterfaceB: static interface method C-");
            return RESULT_B;
        }
        private int privateMethodB() {
            System.out.println("-InterfaceB: private interface method B-");
            return RESULT_B;
        }
    }

    final static class TargetClass implements InterfaceB {
        public int classMethod() {
            System.out.println("-TargetClass: class only method-");
            return RESULT_TARGET;
        }

        @Override
        public int implementedMethod() {
            System.out.println("-TargetClass: implemented non-default interface method-");
            return RESULT_TARGET;
        }

        @Override
        public int defaultMethodB() {
            System.out.println("-TargetClass: overridden default interface method B");

            return RESULT_TARGET;
        }

        public static void main(String[] args) {
            TargetClass tc = new TargetClass();
            tc.doTests(tc);
        }

        private void doTests(TargetClass ref) {
        }
    }

    public InterfaceMethodsTest(String[] args) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new InterfaceMethodsTest(args).startTests();
    }

    private static final String TEST_CLASS_NAME = InterfaceMethodsTest.class.getName().replace('.', '/');
    private static final String TARGET_CLASS_NAME = TargetClass.class.getName().replace('.', '/');
    private static final String INTERFACEA_NAME = InterfaceA.class.getName().replace('.', '/');
    private static final String INTERFACEB_NAME = InterfaceB.class.getName().replace('.', '/');

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain(TARGET_CLASS_NAME);

        bpe = resumeTo(TARGET_CLASS_NAME, "doTests", "(L" + TARGET_CLASS_NAME +";)V");

        mainThread = bpe.thread();

        StackFrame frame = mainThread.frame(0);
        ObjectReference thisObject = frame.thisObject();
        ObjectReference ref = (ObjectReference)frame.getArgumentValues().get(0);

        ReferenceType targetClass = bpe.location().declaringType();
        testImplementationClass(targetClass, thisObject);

        testInterfaceA(ref);

        testInterfaceB(ref);

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("InterfaceMethodsTest: passed");
        } else {
            throw new Exception("InterfaceMethodsTest: failed");
        }
    }

    private void testInterfaceA(ObjectReference ref) {

        ReferenceType ifaceClass = (ReferenceType)vm().classesByName(INTERFACEA_NAME).get(0);

        /* Private method calls */

        Method m = testLookup(ifaceClass, "privateMethodA", "()I", true, null); 

        testInvokePos(m, ref, vm().mirrorOf(RESULT_A), false);
        testInvokePos(m, ref, vm().mirrorOf(RESULT_A), true);


        /* Default method calls */

        testInvokePos(ifaceClass, ref, "defaultMethodA", "()I", vm().mirrorOf(RESULT_A));

        testInvokePos(ifaceClass, ref, "defaultMethodB", "()I", vm().mirrorOf(RESULT_A));

        testInvokePos(ifaceClass, ref, "defaultMethodC", "()I", vm().mirrorOf(RESULT_A));

        testInvokeNeg(ifaceClass, ref, "defaultMethodD", "()I", vm().mirrorOf(RESULT_B),
                      "Attempted to invoke non-existing method");

        testInvokeNeg(ifaceClass, ref, "implementedMethod", "()I", vm().mirrorOf(TARGET_CLASS_NAME),
                      "Invocation of abstract methods is not supported");

        /* Static method calls */

        testInvokePos(ifaceClass, null, "staticMethodA", "()I", vm().mirrorOf(RESULT_A));

        testInvokeNeg(ifaceClass, ref, "staticMethodA", "()I", vm().mirrorOf(RESULT_A),
                      "Invalid MethodID");

        testInvokePos(ifaceClass, null, "staticMethodB", "()I", vm().mirrorOf(RESULT_A));

        testInvokeNeg(ifaceClass, ref, "staticMethodB", "()I", vm().mirrorOf(RESULT_A),
                      "Invalid MethodID");

        testInvokePos(ifaceClass, ref, "implementedMethod", "()I", vm().mirrorOf(RESULT_TARGET), true);
    }

    private void testInterfaceB(ObjectReference ref) {
        ReferenceType ifaceClass = (ReferenceType)vm().classesByName(INTERFACEB_NAME).get(0);

        /* private method calls */

        /* These should fail but won't because of JDK-8167416
        testLookup(ifaceClass, "privateMethodA", "()I", true, NoSuchMethodError.class); 
        testLookup(ifaceClass, "privateMethodA", "()I", false, NoSuchMethodError.class); 
        */
        Method m = testLookup(ifaceClass, "privateMethodB", "()I", true, null); 
        testInvokePos(m, ref, vm().mirrorOf(RESULT_B), false);
        testInvokePos(m, ref, vm().mirrorOf(RESULT_B), true);

        /* Default method calls */

        testInvokePos(ifaceClass, ref, "defaultMethodA", "()I", vm().mirrorOf(RESULT_A));

        testInvokePos(ifaceClass, ref, "defaultMethodB", "()I", vm().mirrorOf(RESULT_A));

        testInvokePos(ifaceClass, ref, "defaultMethodC", "()I", vm().mirrorOf(RESULT_B));

        testInvokePos(ifaceClass, ref, "defaultMethodD", "()I", vm().mirrorOf(RESULT_B));

        testInvokeNeg(ifaceClass, ref, "implementedMethod", "()I", vm().mirrorOf(RESULT_TARGET),
                "Invocation of non-default methods is not supported");


        /* Static method calls*/

        testInvokeNeg(ifaceClass, null, "staticMethodA", "()I", vm().mirrorOf(RESULT_A),
                "Static interface methods are not inheritable");

        testInvokeNeg(ifaceClass, ref, "staticMethodA", "()I", vm().mirrorOf(RESULT_A),
                "Static interface methods are not inheritable");

        testInvokePos(ifaceClass, null, "staticMethodB", "()I", vm().mirrorOf(RESULT_B));

        testInvokeNeg(ifaceClass, ref, "staticMethodB", "()I", vm().mirrorOf(RESULT_B),
                "Invalid MethodID");

        testInvokePos(ifaceClass, null, "staticMethodC", "()I", vm().mirrorOf(RESULT_B));

        testInvokeNeg(ifaceClass, ref, "staticMethodC", "()I", vm().mirrorOf(RESULT_B),
                "Invalid MethodID");
    }

    private void testImplementationClass(ReferenceType targetClass, ObjectReference thisObject) {


        /* Default method calls */

        testInvokePos(targetClass, thisObject, "defaultMethodA", "()I", vm().mirrorOf(RESULT_A));

        testInvokePos(targetClass, thisObject, "defaultMethodB", "()I", vm().mirrorOf(RESULT_TARGET));

        testInvokePos(targetClass, thisObject, "defaultMethodC", "()I", vm().mirrorOf(RESULT_B));

        testInvokePos(targetClass, thisObject, "defaultMethodD", "()I", vm().mirrorOf(RESULT_B));


        /* Non-default instance method calls */

        testInvokePos(targetClass, thisObject, "classMethod", "()I", vm().mirrorOf(RESULT_TARGET));

        testInvokePos(targetClass, thisObject, "implementedMethod", "()I", vm().mirrorOf(RESULT_TARGET));


        /* Static method calls */

        testInvokeNeg(targetClass, thisObject, "staticMethodA", "()I", vm().mirrorOf(RESULT_A),
                "Static interface methods are not inheritable");

        testInvokeNeg(targetClass, thisObject, "staticMethodB", "()I", vm().mirrorOf(RESULT_B),
                "Static interface methods are not inheritable");

        testInvokeNeg(targetClass, thisObject, "staticMethodC", "()I", vm().mirrorOf(RESULT_B),
                "Static interface methods are not inheritable");

        testInvokeNeg(targetClass, null, "staticMethodA", "()I", vm().mirrorOf(RESULT_A),
                "Static interface methods are not inheritable");

        testInvokeNeg(targetClass, null, "staticMethodB", "()I", vm().mirrorOf(RESULT_B),
                "Static interface methods are not inheritable");

        testInvokeNeg(targetClass, null, "staticMethodC", "()I", vm().mirrorOf(RESULT_B),
                "Static interface methods are not inheritable");
    }

    private void testInvokePos(ReferenceType targetClass, ObjectReference ref, String methodName,
                               String methodSig, Value value) {
        testInvokePos(targetClass, ref, methodName, methodSig, value, false);
    }

    private void testInvokePos(ReferenceType targetClass, ObjectReference ref, String methodName,
                               String methodSig, Value value, boolean virtual) {
        logInvocation(ref, methodName, methodSig, targetClass);
        try {
            invoke(targetClass, ref, methodName, methodSig, value, virtual);
            System.err.println("--- PASSED");
        } catch (Exception e) {
            System.err.println("--- FAILED");
            failure("FAILED: Invocation failed with error message " + e.getLocalizedMessage());
        }
    }

    private void testInvokePos(Method method, ObjectReference ref, Value value, boolean virtual) {
        logInvocation(ref, method.name(), method.signature(), method.declaringType());
        try {
            invoke(method.declaringType(), ref, method, value, virtual);
            System.err.println("--- PASSED");
        } catch (Exception e) {
            System.err.println("--- FAILED");
            failure("FAILED: Invocation failed with error message " + e.getLocalizedMessage());
        }
    }

    private void testInvokeNeg(ReferenceType targetClass, ObjectReference ref, String methodName,
                               String methodSig, Value value, String msg) {
        testInvokeNeg(targetClass, ref, methodName, methodSig, value, msg, false);
    }

    private void testInvokeNeg(ReferenceType targetClass, ObjectReference ref, String methodName,
                               String methodSig, Value value, String msg, boolean virtual) {
        logInvocation(ref, methodName, methodSig, targetClass);
        try {
            invoke(targetClass, ref, methodName, methodSig, value, virtual);
            System.err.println("--- FAILED");
            failure("FAILED: " + msg);
        } catch (Exception e) {
            System.err.println("--- PASSED");

        }
    }

    private void invoke(ReferenceType targetClass, ObjectReference ref, String methodName,
                        String methodSig, Value value, boolean virtual) throws Exception {

        Method method = getMethod(targetClass, methodName, methodSig);
        if (method == null) {
            throw new Exception("Can't find method: " + methodName  + " for class = " + targetClass);
        }
        invoke(targetClass, ref, method, value, virtual);
    }

    private void invoke(ReferenceType targetClass, ObjectReference ref, Method method,
                        Value value, boolean virtual) throws Exception {

        println("Invoking " + (method.isAbstract() ? "abstract " : " ") + "method: " + method);
        println(method.declaringType().toString());

        Value returnValue = null;
        if (ref != null) {
            if (virtual) {
                returnValue = invokeVirtual(ref, method);
            } else {
                returnValue = invokeNonVirtual(ref, method);
            }
        } else {
            returnValue = invokeStatic(targetClass, method);
        }

        println("        return val = " + returnValue);
        if (returnValue.equals(value)) {
            println("         " + method.name() + " return value matches: "
                    + value);
        } else {
            if (value != null) {
                throw new Exception(method.name() + " returned: " + returnValue +
                                    " expected: " + value );
            } else {
                println("         " + method.name() + " return value : " + returnValue);
            }

        }
    }

    private Value invokeNonVirtual(ObjectReference ref, Method method) throws Exception {
        return ref.invokeMethod(mainThread, method, Collections.emptyList(), ObjectReference.INVOKE_NONVIRTUAL);
    }

    private Value invokeVirtual(ObjectReference ref, Method method) throws Exception {
        return ref.invokeMethod(mainThread, method, Collections.emptyList(), 0);
    }

    private Value invokeStatic(ReferenceType refType, Method method) throws Exception {
        if (refType instanceof ClassType) {
            return ((ClassType)refType).invokeMethod(mainThread, method, Collections.emptyList(), ObjectReference.INVOKE_NONVIRTUAL);
        } else {
            return ((InterfaceType)refType).invokeMethod(mainThread, method, Collections.emptyList(), ObjectReference.INVOKE_NONVIRTUAL);
        }
    }

    private Method getMethod(ReferenceType rt, String name, String signature) {
        if (rt == null) return null;
        Method m = findMethod(rt, name, signature);
        if (m == null) {
            if (rt instanceof ClassType) {
                for (Object ifc : ((ClassType)rt).interfaces()) {
                    m = getMethod((ReferenceType)ifc, name, signature);
                    if (m != null) {
                        break;
                    }
                }
                if (m == null) {
                    m = getMethod(((ClassType)rt).superclass(), name, signature);
                } else {
                    if (m.isStatic()) {
                        m = null;
                    }
                }
            } else if (rt instanceof InterfaceType) {
                for(Object ifc : ((InterfaceType)rt).superinterfaces()) {
                    m = getMethod((ReferenceType)ifc, name, signature);
                    if (m != null) {
                        if (m.isStatic()) {
                            m = null;
                        }
                        break;
                    }
                }
            }
        }

        return m;
    }

    private void logInvocation(ObjectReference ref, String methodName, String methodSig, ReferenceType targetClass) {
        if (ref != null) {
            System.err.println("Invoking: " + ref.referenceType().name() + "." +
                    methodName + methodSig + " with target of type " +
                    targetClass.name());
        } else {
            System.err.println("Invoking static : " + targetClass.name() + "." +
                    methodName + methodSig);
        }
    }

    private Method testLookup(ReferenceType targetClass, String methodName, String methodSig,
                              boolean declaredOnly, Class<?> expectedException) {

        System.err.println("Looking up " + targetClass.name() + "." + methodName + methodSig);
        try {
            Method m = declaredOnly ?
                lookupDeclaredMethod(targetClass, methodName, methodSig) :
                lookupMethod(targetClass, methodName, methodSig);

            if (expectedException == null) {
                System.err.println("--- PASSED");
                return m;
            }
            else {
                System.err.println("--- FAILED");
                failure("FAILED: lookup succeeded but expected exception "
                        + expectedException.getSimpleName());
                return null;
            }
        }
        catch (Throwable t) {
            if (t.getClass() != expectedException) {
                System.err.println("--- FAILED");
                failure("FAILED: got exception " + t + " but expected exception "
                        + expectedException.getSimpleName());
                return null;
            }
            else {
                System.err.println("--- PASSED");
                return null;
            }
        }
    }

    private Method lookupMethod(ReferenceType targetClass, String methodName, String methodSig) {
        List methods = targetClass.allMethods();
        Iterator iter = methods.iterator();
        while (iter.hasNext()) {
            Method method = (Method)iter.next();
            if (method.name().equals(methodName) &&
                method.signature().equals(methodSig)) {
                return method;
            }
        }
        throw new NoSuchMethodError();
    }

    private Method lookupDeclaredMethod(ReferenceType targetClass, String methodName, String methodSig) {
        Method m = findMethod(targetClass, methodName, methodSig);
        if (m == null)
            throw new NoSuchMethodError();
        return m;
    }
}
