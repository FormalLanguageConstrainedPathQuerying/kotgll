/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared.executor;

import nsk.share.Pair;
import nsk.share.TestFailure;
import nsk.share.test.TestUtils;
import vm.runtime.defmeth.shared.data.AbstractVisitor;
import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.MemoryClassLoader;
import vm.runtime.defmeth.shared.Util;
import vm.runtime.defmeth.shared.data.Visitor;
import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.Tester;
import vm.runtime.defmeth.shared.data.method.body.CallMethod;
import vm.runtime.defmeth.shared.data.method.param.*;
import vm.runtime.defmeth.shared.data.method.result.IntResult;
import vm.runtime.defmeth.shared.data.method.result.ThrowExResult;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

/**
 * Test runner for invocation mode through MethodHandle.invokeWithArguments(...).
 */
public class MHInvokeWithArgsTest extends AbstractReflectionTest {


    public MHInvokeWithArgsTest(MemoryClassLoader cl, DefMethTest testInstance,
                                Collection<? extends Tester> tests) {
        super(testInstance, cl, tests);
    }

    public MHInvokeWithArgsTest(MemoryClassLoader cl, DefMethTest testInstance, Tester... tests) {
        super(testInstance, cl, Arrays.asList(tests));
    }

    private MethodType descToMethodType(String desc) throws ClassNotFoundException {
        Pair<String[],String> p = Util.parseDesc(desc);
        Class rtype = Util.decodeClass(p.second, cl);
        if (p.first.length > 0) {
            Class[] ptypes = new Class[p.first.length];
            for (int i = 0; i < ptypes.length; i++) {
                ptypes[i] = Util.decodeClass(p.first[i], cl);
            }
            return MethodType.methodType(rtype, ptypes);
        } else {
            return MethodType.methodType(rtype);
        }
    }
    private class InvokeWithArgsVisitor extends AbstractVisitor implements Visitor {
        private CallMethod call;

        private CallMethod.Invoke invokeType;
        Class<?> declaringClass;
        String methodName;
        MethodType methodType;
        private Object[] args;

        @Override
        public void visitTester(Tester t) {
            t.getCall().visit(this);

            t.getResult().visit(this);
        }

        private void prepareForInvocation()
                throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException {
            final Clazz staticClass = call.staticClass();
            final Clazz receiverClass = call.receiverClass();

            final Param[] params = call.params();

            invokeType = call.invokeInsn();
            declaringClass = resolve(staticClass);
            methodName = call.methodName();
            methodType = descToMethodType(call.methodDesc());

            Object[] values = values(params);

            if (!call.isConstructorCall() && call.invokeInsn() != CallMethod.Invoke.STATIC) {
                args = new Object[params.length+1];
                Class recClass = resolve(receiverClass);
                args[0] = recClass.newInstance();
                System.arraycopy(values, 0, args, 1, values.length);
            } else {
                args = values; 
            }
        }

        @Override
        public void visitCallMethod(CallMethod call) {
            this.call = call;

        }

        @Override
        public void visitResultInt(IntResult res) {
            try {
                prepareForInvocation();
                int result = (int) invokeInTestContext();
                TestUtils.assertEquals(res.getExpected(), result);
            } catch (Throwable e) {
                throw new TestFailure("Unexpected exception", e);
            }
        }

        @Override
        public void visitResultThrowExc(ThrowExResult res) {
            String expectedExcName = res.getExc().name();
            String originalExpectedExcName = expectedExcName;
            switch (expectedExcName) {
                case "java.lang.IllegalAccessError":
                case "java.lang.InstantiationError":
                    expectedExcName = expectedExcName.replace("Error", "Exception");
                    break;
            }

            try {
                prepareForInvocation(); 
                invokeInTestContext();
                throw new TestFailure("No exception was thrown: " + expectedExcName);
            } catch (Throwable ex) {
                Throwable target = ex;
                Class<?> actualExc = (target.getCause() != null) ? target.getCause().getClass()
                                                              : target.getClass();
                Class<?> expectedExc;
                try {
                    expectedExc = cl.loadClass(expectedExcName);
                } catch (ClassNotFoundException e) {
                    throw new Error(e);
                }
                if (!expectedExc.isAssignableFrom(actualExc) &&
                    !originalExpectedExcName.equals(actualExc.getName()) &&
                    !expectedExc.isAssignableFrom(target.getClass())) {
                    throw new TestFailure(
                            String.format("Caught exception as expected, but it's type is wrong: expected: %s; actual: %s.",
                                    expectedExcName, actualExc.getName()), target);
                }
            }
        }

        @Override
        public void visitResultIgnore() {
            try {
                prepareForInvocation();
                invokeInTestContext();
            } catch (Throwable e) {
                throw new TestFailure("Unexpected exception", e);
            }
        }

        private Object invokeInTestContext() throws Throwable {
            Class<?> context = cl.getTestContext();
            MethodHandle invoker;
            try {
                invoker = MethodHandles.lookup().
                            findStatic(context, "invokeWithArguments",
                                    MethodType.methodType(Object.class, CallMethod.Invoke.class, Class.class,
                                            String.class, MethodType.class, Object[].class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new TestFailure("Exception during reflection invocation", e.getCause());
            }

            return invoker.invokeExact(invokeType, declaringClass, methodName, methodType, args);

        }
    }

    /**
     * Run individual assertion for the test by it's name.
     *
     * @param test
     * @throws Throwable
     */
    public void run(Tester test) throws Throwable {
        test.visit(new InvokeWithArgsVisitor());
    }
}
