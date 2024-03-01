/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8156486
 * @run testng/othervm VarHandleTestMethodTypeInt
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeInt
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false VarHandleTestMethodTypeInt
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeInt
 */

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

import static java.lang.invoke.MethodType.*;

public class VarHandleTestMethodTypeInt extends VarHandleBaseTest {
    static final int static_final_v = 0x01234567;

    static int static_v = 0x01234567;

    final int final_v = 0x01234567;

    int v = 0x01234567;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeInt.class, "final_v", int.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeInt.class, "v", int.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeInt.class, "static_final_v", int.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeInt.class, "static_v", int.class);

        vhArray = MethodHandles.arrayElementVarHandle(int[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypeInt::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypeInt::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypeInt::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypeInt::testArrayWrongMethodType,
                                                     false));
        }
        return cases.stream().map(tc -> new Object[]{tc.toString(), tc}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "accessTestCaseProvider")
    public <T> void testAccess(String desc, AccessTestCase<T> atc) throws Throwable {
        T t = atc.get();
        int iters = atc.requiresLoop() ? ITERS : 1;
        for (int c = 0; c < iters; c++) {
            atc.testAccess(t);
        }
    }


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeInt recv, VarHandle vh) throws Throwable {
        checkNPE(() -> { 
            int x = (int) vh.get(null);
        });
        checkCCE(() -> { 
            int x = (int) vh.get(Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.get(0);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.get(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.get(recv);
        });
        checkWMTE(() -> { 
            int x = (int) vh.get();
        });
        checkWMTE(() -> { 
            int x = (int) vh.get(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.set(null, 0x01234567);
        });
        checkCCE(() -> { 
            vh.set(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.set(0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.set();
        });
        checkWMTE(() -> { 
            vh.set(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getVolatile(null);
        });
        checkCCE(() -> { 
            int x = (int) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getVolatile(0);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getVolatile(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getVolatile(recv);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getVolatile();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getVolatile(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.setVolatile(null, 0x01234567);
        });
        checkCCE(() -> { 
            vh.setVolatile(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.setVolatile(0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setVolatile();
        });
        checkWMTE(() -> { 
            vh.setVolatile(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getOpaque(null);
        });
        checkCCE(() -> { 
            int x = (int) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getOpaque(0);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getOpaque(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getOpaque(recv);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getOpaque();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getOpaque(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.setOpaque(null, 0x01234567);
        });
        checkCCE(() -> { 
            vh.setOpaque(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.setOpaque(0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setOpaque();
        });
        checkWMTE(() -> { 
            vh.setOpaque(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAcquire(null);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAcquire(0);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getAcquire(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAcquire(recv);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAcquire(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.setRelease(null, 0x01234567);
        });
        checkCCE(() -> { 
            vh.setRelease(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.setRelease(0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setRelease();
        });
        checkWMTE(() -> { 
            vh.setRelease(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.compareAndSet(null, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(recv, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(recv, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(recv, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(null, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(recv, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(recv, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSet(null, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(recv, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(recv, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(recv, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(null, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(recv, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(recv, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(null, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(recv, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(recv, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.compareAndExchange(null, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.compareAndExchange(Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(recv, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(recv, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchange(recv, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchange(recv, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange();
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(recv, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(null, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(recv, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(recv, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeAcquire(recv, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(recv, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(null, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(recv, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(recv, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeRelease(recv, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(recv, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndSet(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndSet(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSet(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSet(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet(recv, 0x01234567, Void.class);
        });

        checkNPE(() -> { 
            int x = (int) vh.getAndSetAcquire(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndSetAcquire(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire(recv, 0x01234567, Void.class);
        });

        checkNPE(() -> { 
            int x = (int) vh.getAndSetRelease(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndSetRelease(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetRelease(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetRelease(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease(recv, 0x01234567, Void.class);
        });

        checkNPE(() -> { 
            int x = (int) vh.getAndAdd(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndAdd(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAdd(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAdd(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd(recv, 0x01234567, Void.class);
        });

        checkNPE(() -> { 
            int x = (int) vh.getAndAddAcquire(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndAddAcquire(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire(recv, 0x01234567, Void.class);
        });

        checkNPE(() -> { 
            int x = (int) vh.getAndAddRelease(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndAddRelease(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddRelease(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddRelease(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease(recv, 0x01234567, Void.class);
        });

        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseOr(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseOr(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOr(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOr(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseOr(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOr(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOr(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAnd(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAnd(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseXor(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseXor(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXor(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXor(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(recv, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease(null, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseXor(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(0, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXor(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXor(recv, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(recv, 0x01234567, Void.class);
        });
    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeInt recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class)).
                    invokeExact((VarHandleTestMethodTypeInt) null);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class)).
                    invokeExact(0);
            });
            checkWMTE(() -> { 
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeInt.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeInt.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            checkNPE(() -> { 
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact((VarHandleTestMethodTypeInt) null, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                hs.get(am, methodType(void.class, Class.class, int.class)).
                    invokeExact(Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeInt.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int.class, int.class)).
                    invokeExact(0, 0x01234567);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeInt.class, int.class, Class.class)).
                    invokeExact(recv, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            checkNPE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeInt.class, int.class, int.class)).
                    invokeExact((VarHandleTestMethodTypeInt) null, 0x01234567, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, int.class)).
                    invokeExact(Void.class, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeInt.class, Class.class, int.class)).
                    invokeExact(recv, Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeInt.class, int.class, Class.class)).
                    invokeExact(recv, 0x01234567, Void.class);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , int.class, int.class)).
                    invokeExact(0, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeInt.class, int.class, int.class, Class.class)).
                    invokeExact(recv, 0x01234567, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, int.class, int.class)).
                    invokeExact((VarHandleTestMethodTypeInt) null, 0x01234567, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class, int.class)).
                    invokeExact(Void.class, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, Class.class, int.class)).
                    invokeExact(recv, Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, int.class, Class.class)).
                    invokeExact(recv, 0x01234567, Void.class);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class , int.class, int.class)).
                    invokeExact(0, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeInt.class , int.class, int.class)).
                    invokeExact(recv, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeInt.class , int.class, int.class)).
                    invokeExact(recv, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, int.class, int.class, Class.class)).
                    invokeExact(recv, 0x01234567, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact((VarHandleTestMethodTypeInt) null, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class)).
                    invokeExact(Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, int.class)).
                    invokeExact(0, 0x01234567);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact(recv, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact(recv, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact(recv, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact((VarHandleTestMethodTypeInt) null, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class)).
                    invokeExact(Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, int.class)).
                    invokeExact(0, 0x01234567);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact(recv, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact(recv, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact(recv, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact((VarHandleTestMethodTypeInt) null, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class)).
                    invokeExact(Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, int.class)).
                    invokeExact(0, 0x01234567);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact(recv, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact(recv, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeInt.class, int.class)).
                    invokeExact(recv, 0x01234567, Void.class);
            });
        }
    }


    static void testStaticFieldWrongMethodType(VarHandle vh) throws Throwable {
        checkWMTE(() -> { 
            Void x = (Void) vh.get();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.get();
        });
        checkWMTE(() -> { 
            int x = (int) vh.get(Void.class);
        });


        checkWMTE(() -> { 
            vh.set(Void.class);
        });
        checkWMTE(() -> { 
            vh.set();
        });
        checkWMTE(() -> { 
            vh.set(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            Void x = (Void) vh.getVolatile();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getVolatile();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getVolatile(Void.class);
        });


        checkWMTE(() -> { 
            vh.setVolatile(Void.class);
        });
        checkWMTE(() -> { 
            vh.setVolatile();
        });
        checkWMTE(() -> { 
            vh.setVolatile(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            Void x = (Void) vh.getOpaque();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getOpaque();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getOpaque(Void.class);
        });


        checkWMTE(() -> { 
            vh.setOpaque(Void.class);
        });
        checkWMTE(() -> { 
            vh.setOpaque();
        });
        checkWMTE(() -> { 
            vh.setOpaque(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            Void x = (Void) vh.getAcquire();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAcquire(Void.class);
        });


        checkWMTE(() -> { 
            vh.setRelease(Void.class);
        });
        checkWMTE(() -> { 
            vh.setRelease();
        });
        checkWMTE(() -> { 
            vh.setRelease(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(0x01234567, 0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(0x01234567, 0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(0x01234567, 0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(0x01234567, 0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(0x01234567, 0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchange(0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchange(0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange();
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(0x01234567, 0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeAcquire(0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeAcquire(0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(0x01234567, 0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeRelease(0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeRelease(0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(0x01234567, 0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndSet(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSet(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSet(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetRelease(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetRelease(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease(0x01234567, Void.class);
        });

        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAdd(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAdd(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddRelease(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddRelease(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease(0x01234567, Void.class);
        });

        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOr(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOr(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrRelease(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrRelease(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAnd(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAnd(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndRelease(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndRelease(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXor(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXor(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(0x01234567, Void.class);
        });


        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorRelease(0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorRelease(0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease(0x01234567, Void.class);
        });
    }

    static void testStaticFieldWrongMethodType(Handles hs) throws Throwable {
        int i = 0;

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            checkWMTE(() -> { 
                Void x = (Void) hs.get(am, methodType(Void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(Class.class)).
                    invokeExact(Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int.class, Class.class)).
                    invokeExact(0x01234567, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class)).
                    invokeExact(Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, Class.class)).
                    invokeExact(0x01234567, Void.class);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, Class.class)).
                    invokeExact(0x01234567, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class)).
                    invokeExact(Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, Class.class)).
                    invokeExact(0x01234567, Void.class);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, int.class, int.class)).
                    invokeExact(0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class)).
                    invokeExact(0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, int.class, Class.class)).
                    invokeExact(0x01234567, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, int.class)).
                    invokeExact(0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class)).
                    invokeExact(0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, Class.class)).
                    invokeExact(0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, int.class)).
                    invokeExact(0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class)).
                    invokeExact(0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, Class.class)).
                    invokeExact(0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, int.class)).
                    invokeExact(0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class)).
                    invokeExact(0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, Class.class)).
                    invokeExact(0x01234567, Void.class);
            });
        }
    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        int[] array = new int[10];
        Arrays.fill(array, 0x01234567);

        checkNPE(() -> { 
            int x = (int) vh.get(null, 0);
        });
        checkCCE(() -> { 
            int x = (int) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.get(0, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.get(array, Void.class);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.get(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.get(array, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.get();
        });
        checkWMTE(() -> { 
            int x = (int) vh.get(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.set(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            vh.set(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.set(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.set(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.set();
        });
        checkWMTE(() -> { 
            vh.set(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { 
            int x = (int) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getVolatile(array, Void.class);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getVolatile();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getVolatile(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.setVolatile(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            vh.setVolatile(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.setVolatile(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setVolatile(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setVolatile();
        });
        checkWMTE(() -> { 
            vh.setVolatile(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { 
            int x = (int) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getOpaque(array, Void.class);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getOpaque();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getOpaque(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.setOpaque(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            vh.setOpaque(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.setOpaque(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setOpaque(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setOpaque();
        });
        checkWMTE(() -> { 
            vh.setOpaque(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAcquire(array, Void.class);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAcquire(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.setRelease(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            vh.setRelease(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.setRelease(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setRelease(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            vh.setRelease();
        });
        checkWMTE(() -> { 
            vh.setRelease(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.compareAndSet(null, 0, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(Void.class, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, 0, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, 0, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(0, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, 0, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(null, 0, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, 0, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(0, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, 0, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSet(null, 0, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(Void.class, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, 0, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(0, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, 0, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(null, 0, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, 0, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(0, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, 0, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(null, 0, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, 0, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(0, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, 0, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.compareAndExchange(null, 0, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.compareAndExchange(Void.class, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(array, 0, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(array, 0, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(0, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(array, Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchange(array, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchange(array, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange();
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchange(array, 0, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(null, 0, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(Void.class, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(array, 0, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(array, 0, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(0, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(array, Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeAcquire(array, 0, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(null, 0, 0x01234567, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(Void.class, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(array, 0, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(array, 0, 0x01234567, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(0, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(array, Void.class, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, 0x01234567, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.compareAndExchangeRelease(array, 0, 0x01234567, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndSet(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndSet(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSet(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSet(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSet(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndSetAcquire(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndSetAcquire(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetAcquire(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndSetRelease(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndSetRelease(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndSetRelease(array, 0, 0x01234567, Void.class);
        });

        checkNPE(() -> { 
            int x = (int) vh.getAndAdd(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndAdd(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAdd(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAdd(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAdd(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndAddAcquire(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndAddAcquire(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddAcquire(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndAddRelease(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndAddRelease(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndAddRelease(array, 0, 0x01234567, Void.class);
        });

        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseOr(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseOr(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOr(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOr(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOr(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrAcquire(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseOrRelease(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAnd(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAnd(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAnd(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndAcquire(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseAndRelease(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseXor(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseXor(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXor(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXor(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXor(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorAcquire(array, 0, 0x01234567, Void.class);
        });


        checkNPE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease(null, 0, 0x01234567);
        });
        checkCCE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease(Void.class, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease(0, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease(array, Void.class, 0x01234567);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorRelease(array, 0, 0x01234567);
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { 
            int x = (int) vh.getAndBitwiseXorRelease(array, 0, 0x01234567, Void.class);
        });
    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        int[] array = new int[10];
        Arrays.fill(array, 0x01234567);

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class)).
                    invokeExact((int[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            checkWMTE(() -> { 
                Void x = (Void) hs.get(am, methodType(Void.class, int[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            checkNPE(() -> { 
                hs.get(am, methodType(void.class, int[].class, int.class, int.class)).
                    invokeExact((int[]) null, 0, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                hs.get(am, methodType(void.class, Class.class, int.class, int.class)).
                    invokeExact(Void.class, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int.class, int.class, int.class)).
                    invokeExact(0, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int[].class, Class.class, int.class)).
                    invokeExact(array, Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int[].class, int.class, Class.class)).
                    invokeExact(array, 0, 0x01234567, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            checkNPE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int[].class, int.class, int.class, int.class)).
                    invokeExact((int[]) null, 0, 0x01234567, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, int.class, int.class)).
                    invokeExact(Void.class, 0, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int[].class, int.class, Class.class, int.class)).
                    invokeExact(array, 0, Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int[].class, int.class, int.class, Class.class)).
                    invokeExact(array, 0, 0x01234567, Void.class);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, int.class, int.class)).
                    invokeExact(0, 0, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int[].class, Class.class, int.class, int.class)).
                    invokeExact(array, Void.class, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int[].class, int.class, int.class, int.class, Class.class)).
                    invokeExact(array, 0, 0x01234567, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, int.class, int.class)).
                    invokeExact((int[]) null, 0, 0x01234567, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class, int.class, int.class)).
                    invokeExact(Void.class, 0, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, Class.class, int.class)).
                    invokeExact(array, 0, Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, int.class, Class.class)).
                    invokeExact(array, 0, 0x01234567, Void.class);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, int.class, int.class, int.class)).
                    invokeExact(0, 0, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, Class.class, int.class, int.class)).
                    invokeExact(array, Void.class, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, int[].class, int.class, int.class, int.class)).
                    invokeExact(array, 0, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int[].class, int.class, int.class, int.class)).
                    invokeExact(array, 0, 0x01234567, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, int.class, int.class, Class.class)).
                    invokeExact(array, 0, 0x01234567, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, int.class)).
                    invokeExact((int[]) null, 0, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class, int.class)).
                    invokeExact(Void.class, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, int.class, int.class)).
                    invokeExact(0, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, Class.class, int.class)).
                    invokeExact(array, Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, int[].class, int.class, int.class)).
                    invokeExact(array, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int[].class, int.class, int.class)).
                    invokeExact(array, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, int.class, Class.class)).
                    invokeExact(array, 0, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, int.class)).
                    invokeExact((int[]) null, 0, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class, int.class)).
                    invokeExact(Void.class, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, int.class, int.class)).
                    invokeExact(0, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, Class.class, int.class)).
                    invokeExact(array, Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, int[].class, int.class, int.class)).
                    invokeExact(array, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int[].class, int.class, int.class)).
                    invokeExact(array, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, int.class, Class.class)).
                    invokeExact(array, 0, 0x01234567, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkNPE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, int.class)).
                    invokeExact((int[]) null, 0, 0x01234567);
            });
            hs.checkWMTEOrCCE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, Class.class, int.class, int.class)).
                    invokeExact(Void.class, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int.class, int.class, int.class)).
                    invokeExact(0, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, Class.class, int.class)).
                    invokeExact(array, Void.class, 0x01234567);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, int[].class, int.class, int.class)).
                    invokeExact(array, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int[].class, int.class, int.class)).
                    invokeExact(array, 0, 0x01234567);
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                int x = (int) hs.get(am, methodType(int.class, int[].class, int.class, int.class, Class.class)).
                    invokeExact(array, 0, 0x01234567, Void.class);
            });
        }
    }
}
