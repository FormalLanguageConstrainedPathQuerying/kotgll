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
 * @run testng/othervm VarHandleTestMethodTypeChar
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeChar
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false VarHandleTestMethodTypeChar
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeChar
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

public class VarHandleTestMethodTypeChar extends VarHandleBaseTest {
    static final char static_final_v = '\u0123';

    static char static_v = '\u0123';

    final char final_v = '\u0123';

    char v = '\u0123';

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeChar.class, "final_v", char.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeChar.class, "v", char.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeChar.class, "static_final_v", char.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeChar.class, "static_v", char.class);

        vhArray = MethodHandles.arrayElementVarHandle(char[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypeChar::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypeChar::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypeChar::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypeChar::testArrayWrongMethodType,
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


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeChar recv, VarHandle vh) throws Throwable {
        checkNPE(() -> { 
            char x = (char) vh.get(null);
        });
        checkCCE(() -> { 
            char x = (char) vh.get(Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.get(0);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.get(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.get(recv);
        });
        checkWMTE(() -> { 
            char x = (char) vh.get();
        });
        checkWMTE(() -> { 
            char x = (char) vh.get(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.set(null, '\u0123');
        });
        checkCCE(() -> { 
            vh.set(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.set(0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.set();
        });
        checkWMTE(() -> { 
            vh.set(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getVolatile(null);
        });
        checkCCE(() -> { 
            char x = (char) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getVolatile(0);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getVolatile(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getVolatile(recv);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getVolatile();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getVolatile(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.setVolatile(null, '\u0123');
        });
        checkCCE(() -> { 
            vh.setVolatile(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.setVolatile(0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setVolatile();
        });
        checkWMTE(() -> { 
            vh.setVolatile(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getOpaque(null);
        });
        checkCCE(() -> { 
            char x = (char) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getOpaque(0);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getOpaque(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getOpaque(recv);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getOpaque();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getOpaque(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.setOpaque(null, '\u0123');
        });
        checkCCE(() -> { 
            vh.setOpaque(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.setOpaque(0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setOpaque();
        });
        checkWMTE(() -> { 
            vh.setOpaque(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAcquire(null);
        });
        checkCCE(() -> { 
            char x = (char) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAcquire(0);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getAcquire(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAcquire(recv);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAcquire(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.setRelease(null, '\u0123');
        });
        checkCCE(() -> { 
            vh.setRelease(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.setRelease(0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setRelease();
        });
        checkWMTE(() -> { 
            vh.setRelease(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.compareAndSet(null, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(recv, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(recv, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(recv, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(null, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(recv, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(recv, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSet(null, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(recv, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(recv, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(recv, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(null, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(recv, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(recv, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(null, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(recv, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(recv, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.compareAndExchange(null, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.compareAndExchange(Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(recv, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(recv, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchange(recv, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchange(recv, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange();
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(recv, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(null, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(recv, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(recv, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeAcquire(recv, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(recv, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(null, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(recv, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(recv, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeRelease(recv, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(recv, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndSet(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndSet(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSet(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSet(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet(recv, '\u0123', Void.class);
        });

        checkNPE(() -> { 
            char x = (char) vh.getAndSetAcquire(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndSetAcquire(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire(recv, '\u0123', Void.class);
        });

        checkNPE(() -> { 
            char x = (char) vh.getAndSetRelease(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndSetRelease(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetRelease(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetRelease(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease(recv, '\u0123', Void.class);
        });

        checkNPE(() -> { 
            char x = (char) vh.getAndAdd(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndAdd(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAdd(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAdd(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd(recv, '\u0123', Void.class);
        });

        checkNPE(() -> { 
            char x = (char) vh.getAndAddAcquire(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndAddAcquire(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire(recv, '\u0123', Void.class);
        });

        checkNPE(() -> { 
            char x = (char) vh.getAndAddRelease(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndAddRelease(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddRelease(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddRelease(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease(recv, '\u0123', Void.class);
        });

        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseOr(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseOr(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOr(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOr(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseOr(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOr(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOr(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAnd(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAnd(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseXor(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseXor(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXor(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXor(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(recv, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease(null, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseXor(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(0, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXor(recv, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXor(recv, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(recv, '\u0123', Void.class);
        });
    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeChar recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class)).
                    invokeExact((VarHandleTestMethodTypeChar) null);
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class)).
                    invokeExact(0);
            });
            checkWMTE(() -> { 
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeChar.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeChar.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            checkNPE(() -> { 
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact((VarHandleTestMethodTypeChar) null, '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                hs.get(am, methodType(void.class, Class.class, char.class)).
                    invokeExact(Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeChar.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int.class, char.class)).
                    invokeExact(0, '\u0123');
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeChar.class, char.class, Class.class)).
                    invokeExact(recv, '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            checkNPE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeChar.class, char.class, char.class)).
                    invokeExact((VarHandleTestMethodTypeChar) null, '\u0123', '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, char.class, char.class)).
                    invokeExact(Void.class, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeChar.class, Class.class, char.class)).
                    invokeExact(recv, Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeChar.class, char.class, Class.class)).
                    invokeExact(recv, '\u0123', Void.class);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , char.class, char.class)).
                    invokeExact(0, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeChar.class, char.class, char.class, Class.class)).
                    invokeExact(recv, '\u0123', '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, char.class, char.class)).
                    invokeExact((VarHandleTestMethodTypeChar) null, '\u0123', '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, char.class, char.class)).
                    invokeExact(Void.class, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, Class.class, char.class)).
                    invokeExact(recv, Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, char.class, Class.class)).
                    invokeExact(recv, '\u0123', Void.class);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class , char.class, char.class)).
                    invokeExact(0, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeChar.class , char.class, char.class)).
                    invokeExact(recv, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeChar.class , char.class, char.class)).
                    invokeExact(recv, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, char.class, char.class, Class.class)).
                    invokeExact(recv, '\u0123', '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact((VarHandleTestMethodTypeChar) null, '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, char.class)).
                    invokeExact(Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class, char.class)).
                    invokeExact(0, '\u0123');
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact(recv, '\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact(recv, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact(recv, '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact((VarHandleTestMethodTypeChar) null, '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, char.class)).
                    invokeExact(Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class, char.class)).
                    invokeExact(0, '\u0123');
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact(recv, '\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact(recv, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact(recv, '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact((VarHandleTestMethodTypeChar) null, '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, char.class)).
                    invokeExact(Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class, char.class)).
                    invokeExact(0, '\u0123');
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact(recv, '\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact(recv, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, VarHandleTestMethodTypeChar.class, char.class)).
                    invokeExact(recv, '\u0123', Void.class);
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
            char x = (char) vh.get(Void.class);
        });


        checkWMTE(() -> { 
            vh.set(Void.class);
        });
        checkWMTE(() -> { 
            vh.set();
        });
        checkWMTE(() -> { 
            vh.set('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            Void x = (Void) vh.getVolatile();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getVolatile();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getVolatile(Void.class);
        });


        checkWMTE(() -> { 
            vh.setVolatile(Void.class);
        });
        checkWMTE(() -> { 
            vh.setVolatile();
        });
        checkWMTE(() -> { 
            vh.setVolatile('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            Void x = (Void) vh.getOpaque();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getOpaque();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getOpaque(Void.class);
        });


        checkWMTE(() -> { 
            vh.setOpaque(Void.class);
        });
        checkWMTE(() -> { 
            vh.setOpaque();
        });
        checkWMTE(() -> { 
            vh.setOpaque('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            Void x = (Void) vh.getAcquire();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAcquire(Void.class);
        });


        checkWMTE(() -> { 
            vh.setRelease(Void.class);
        });
        checkWMTE(() -> { 
            vh.setRelease();
        });
        checkWMTE(() -> { 
            vh.setRelease('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet('\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet('\u0123', '\u0123', Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain('\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain('\u0123', '\u0123', Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet('\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet('\u0123', '\u0123', Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire('\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire('\u0123', '\u0123', Void.class);
        });


        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease('\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease('\u0123', '\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange('\u0123', Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchange('\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchange('\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange();
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange('\u0123', '\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire('\u0123', Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeAcquire('\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeAcquire('\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire('\u0123', '\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease('\u0123', Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeRelease('\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeRelease('\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease('\u0123', '\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndSet(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSet('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSet('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetAcquire('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetAcquire('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetRelease('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetRelease('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease('\u0123', Void.class);
        });

        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAdd('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAdd('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddAcquire('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddAcquire('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddRelease('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddRelease('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease('\u0123', Void.class);
        });

        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOr('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOr('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrAcquire('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrAcquire('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrRelease('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrRelease('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAnd('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAnd('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndAcquire('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndAcquire('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndRelease('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndRelease('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXor('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXor('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorAcquire('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorAcquire('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire('\u0123', Void.class);
        });


        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease(Void.class);
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorRelease('\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorRelease('\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease('\u0123', Void.class);
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
                char x = (char) hs.get(am, methodType(Class.class)).
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
                hs.get(am, methodType(void.class, char.class, Class.class)).
                    invokeExact('\u0123', Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, char.class)).
                    invokeExact(Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, char.class, Class.class)).
                    invokeExact('\u0123', Void.class);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, char.class, char.class, Class.class)).
                    invokeExact('\u0123', '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, char.class)).
                    invokeExact(Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char.class, Class.class)).
                    invokeExact('\u0123', Void.class);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, char.class, char.class)).
                    invokeExact('\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, char.class, char.class)).
                    invokeExact('\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char.class, char.class, Class.class)).
                    invokeExact('\u0123', '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, char.class)).
                    invokeExact('\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, char.class)).
                    invokeExact('\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char.class, Class.class)).
                    invokeExact('\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, char.class)).
                    invokeExact('\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, char.class)).
                    invokeExact('\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char.class, Class.class)).
                    invokeExact('\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, char.class)).
                    invokeExact('\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, char.class)).
                    invokeExact('\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char.class, Class.class)).
                    invokeExact('\u0123', Void.class);
            });
        }
    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        char[] array = new char[10];
        Arrays.fill(array, '\u0123');

        checkNPE(() -> { 
            char x = (char) vh.get(null, 0);
        });
        checkCCE(() -> { 
            char x = (char) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.get(0, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.get(array, Void.class);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.get(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.get(array, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.get();
        });
        checkWMTE(() -> { 
            char x = (char) vh.get(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.set(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            vh.set(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.set(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.set(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            vh.set();
        });
        checkWMTE(() -> { 
            vh.set(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { 
            char x = (char) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getVolatile(array, Void.class);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getVolatile();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getVolatile(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.setVolatile(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            vh.setVolatile(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.setVolatile(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setVolatile(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setVolatile();
        });
        checkWMTE(() -> { 
            vh.setVolatile(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { 
            char x = (char) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getOpaque(array, Void.class);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getOpaque();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getOpaque(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.setOpaque(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            vh.setOpaque(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.setOpaque(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setOpaque(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setOpaque();
        });
        checkWMTE(() -> { 
            vh.setOpaque(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { 
            char x = (char) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAcquire(array, Void.class);
        });
        checkWMTE(() -> { 
            Void x = (Void) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAcquire(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.setRelease(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            vh.setRelease(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.setRelease(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setRelease(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            vh.setRelease();
        });
        checkWMTE(() -> { 
            vh.setRelease(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.compareAndSet(null, 0, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(Void.class, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, 0, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, 0, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(0, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, 0, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(null, 0, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, 0, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(0, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, 0, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSet(null, 0, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(Void.class, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, 0, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(0, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, 0, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(null, 0, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, 0, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(0, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, 0, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(null, 0, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, 0, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(0, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, 0, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.compareAndExchange(null, 0, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.compareAndExchange(Void.class, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(array, 0, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(array, 0, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(0, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(array, Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchange(array, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchange(array, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange();
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchange(array, 0, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(null, 0, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(Void.class, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(array, 0, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(array, 0, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(0, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(array, Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeAcquire(array, 0, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(null, 0, '\u0123', '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(Void.class, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(array, 0, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(array, 0, '\u0123', Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(0, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(array, Void.class, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, '\u0123', '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.compareAndExchangeRelease(array, 0, '\u0123', '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndSet(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndSet(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSet(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSet(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSet(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndSetAcquire(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndSetAcquire(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetAcquire(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndSetRelease(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndSetRelease(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndSetRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndSetRelease(array, 0, '\u0123', Void.class);
        });

        checkNPE(() -> { 
            char x = (char) vh.getAndAdd(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndAdd(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAdd(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAdd(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAdd(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndAddAcquire(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndAddAcquire(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddAcquire(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndAddRelease(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndAddRelease(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndAddRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndAddRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndAddRelease(array, 0, '\u0123', Void.class);
        });

        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseOr(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseOr(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOr(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOr(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOr(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrAcquire(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseOrRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseOrRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseOrRelease(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAnd(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAnd(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAnd(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndAcquire(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseAndRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseAndRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseAndRelease(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseXor(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseXor(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXor(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXor(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXor(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorAcquire(array, 0, '\u0123', Void.class);
        });


        checkNPE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease(null, 0, '\u0123');
        });
        checkCCE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease(Void.class, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease(0, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease(array, Void.class, '\u0123');
        });
        checkWMTE(() -> { 
            Void r = (Void) vh.getAndBitwiseXorRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndBitwiseXorRelease(array, 0, '\u0123');
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { 
            char x = (char) vh.getAndBitwiseXorRelease(array, 0, '\u0123', Void.class);
        });
    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        char[] array = new char[10];
        Arrays.fill(array, '\u0123');

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class)).
                    invokeExact((char[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            checkWMTE(() -> { 
                Void x = (Void) hs.get(am, methodType(Void.class, char[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, char[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            checkNPE(() -> { 
                hs.get(am, methodType(void.class, char[].class, int.class, char.class)).
                    invokeExact((char[]) null, 0, '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                hs.get(am, methodType(void.class, Class.class, int.class, char.class)).
                    invokeExact(Void.class, 0, '\u0123');
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, char[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int.class, int.class, char.class)).
                    invokeExact(0, 0, '\u0123');
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, char[].class, Class.class, char.class)).
                    invokeExact(array, Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, char[].class, int.class, Class.class)).
                    invokeExact(array, 0, '\u0123', Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            checkNPE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, char[].class, int.class, char.class, char.class)).
                    invokeExact((char[]) null, 0, '\u0123', '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, char.class, char.class)).
                    invokeExact(Void.class, 0, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, char[].class, int.class, Class.class, char.class)).
                    invokeExact(array, 0, Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, char[].class, int.class, char.class, Class.class)).
                    invokeExact(array, 0, '\u0123', Void.class);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, char.class, char.class)).
                    invokeExact(0, 0, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, char[].class, Class.class, char.class, char.class)).
                    invokeExact(array, Void.class, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, char[].class, int.class, char.class, char.class, Class.class)).
                    invokeExact(array, 0, '\u0123', '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, char.class, char.class)).
                    invokeExact((char[]) null, 0, '\u0123', '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, int.class, char.class, char.class)).
                    invokeExact(Void.class, 0, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, Class.class, char.class)).
                    invokeExact(array, 0, Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, char.class, Class.class)).
                    invokeExact(array, 0, '\u0123', Void.class);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class, int.class, char.class, char.class)).
                    invokeExact(0, 0, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, Class.class, char.class, char.class)).
                    invokeExact(array, Void.class, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, char[].class, int.class, char.class, char.class)).
                    invokeExact(array, 0, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, char[].class, int.class, char.class, char.class)).
                    invokeExact(array, 0, '\u0123', '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, char.class, char.class, Class.class)).
                    invokeExact(array, 0, '\u0123', '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, char.class)).
                    invokeExact((char[]) null, 0, '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, int.class, char.class)).
                    invokeExact(Void.class, 0, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class, int.class, char.class)).
                    invokeExact(0, 0, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, Class.class, char.class)).
                    invokeExact(array, Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, char[].class, int.class, char.class)).
                    invokeExact(array, 0, '\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, char[].class, int.class, char.class)).
                    invokeExact(array, 0, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, char.class, Class.class)).
                    invokeExact(array, 0, '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, char.class)).
                    invokeExact((char[]) null, 0, '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, int.class, char.class)).
                    invokeExact(Void.class, 0, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class, int.class, char.class)).
                    invokeExact(0, 0, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, Class.class, char.class)).
                    invokeExact(array, Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, char[].class, int.class, char.class)).
                    invokeExact(array, 0, '\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, char[].class, int.class, char.class)).
                    invokeExact(array, 0, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, char.class, Class.class)).
                    invokeExact(array, 0, '\u0123', Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkNPE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, char.class)).
                    invokeExact((char[]) null, 0, '\u0123');
            });
            hs.checkWMTEOrCCE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, Class.class, int.class, char.class)).
                    invokeExact(Void.class, 0, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, int.class, int.class, char.class)).
                    invokeExact(0, 0, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, Class.class, char.class)).
                    invokeExact(array, Void.class, '\u0123');
            });
            checkWMTE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, char[].class, int.class, char.class)).
                    invokeExact(array, 0, '\u0123');
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, char[].class, int.class, char.class)).
                    invokeExact(array, 0, '\u0123');
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                char x = (char) hs.get(am, methodType(char.class, char[].class, int.class, char.class, Class.class)).
                    invokeExact(array, 0, '\u0123', Void.class);
            });
        }
    }
}
