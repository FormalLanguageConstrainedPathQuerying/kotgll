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
 * @run testng/othervm VarHandleTestMethodTypeString
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeString
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false VarHandleTestMethodTypeString
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeString
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

public class VarHandleTestMethodTypeString extends VarHandleBaseTest {
    static final String static_final_v = "foo";

    static String static_v = "foo";

    final String final_v = "foo";

    String v = "foo";

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeString.class, "final_v", String.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeString.class, "v", String.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeString.class, "static_final_v", String.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeString.class, "static_v", String.class);

        vhArray = MethodHandles.arrayElementVarHandle(String[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypeString::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypeString::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypeString::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypeString::testArrayWrongMethodType,
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


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeString recv, VarHandle vh) throws Throwable {
        checkNPE(() -> { 
            String x = (String) vh.get(null);
        });
        checkCCE(() -> { 
            String x = (String) vh.get(Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.get(0);
        });
        checkCCE(() -> { 
            Void x = (Void) vh.get(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.get(recv);
        });
        checkWMTE(() -> { 
            String x = (String) vh.get();
        });
        checkWMTE(() -> { 
            String x = (String) vh.get(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.set(null, "foo");
        });
        checkCCE(() -> { 
            vh.set(Void.class, "foo");
        });
        checkCCE(() -> { 
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.set(0, "foo");
        });
        checkWMTE(() -> { 
            vh.set();
        });
        checkWMTE(() -> { 
            vh.set(recv, "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getVolatile(null);
        });
        checkCCE(() -> { 
            String x = (String) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getVolatile(0);
        });
        checkCCE(() -> { 
            Void x = (Void) vh.getVolatile(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getVolatile(recv);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getVolatile();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getVolatile(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.setVolatile(null, "foo");
        });
        checkCCE(() -> { 
            vh.setVolatile(Void.class, "foo");
        });
        checkCCE(() -> { 
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.setVolatile(0, "foo");
        });
        checkWMTE(() -> { 
            vh.setVolatile();
        });
        checkWMTE(() -> { 
            vh.setVolatile(recv, "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getOpaque(null);
        });
        checkCCE(() -> { 
            String x = (String) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getOpaque(0);
        });
        checkCCE(() -> { 
            Void x = (Void) vh.getOpaque(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getOpaque(recv);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getOpaque();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getOpaque(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.setOpaque(null, "foo");
        });
        checkCCE(() -> { 
            vh.setOpaque(Void.class, "foo");
        });
        checkCCE(() -> { 
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.setOpaque(0, "foo");
        });
        checkWMTE(() -> { 
            vh.setOpaque();
        });
        checkWMTE(() -> { 
            vh.setOpaque(recv, "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getAcquire(null);
        });
        checkCCE(() -> { 
            String x = (String) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAcquire(0);
        });
        checkCCE(() -> { 
            Void x = (Void) vh.getAcquire(recv);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAcquire(recv);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAcquire();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAcquire(recv, Void.class);
        });


        checkNPE(() -> { 
            vh.setRelease(null, "foo");
        });
        checkCCE(() -> { 
            vh.setRelease(Void.class, "foo");
        });
        checkCCE(() -> { 
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { 
            vh.setRelease(0, "foo");
        });
        checkWMTE(() -> { 
            vh.setRelease();
        });
        checkWMTE(() -> { 
            vh.setRelease(recv, "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.compareAndSet(null, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(recv, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(recv, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(recv, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(null, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(recv, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(recv, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSet(null, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(recv, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(recv, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(recv, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(null, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(recv, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(recv, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(null, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(recv, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(recv, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.compareAndExchange(null, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchange(Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchange(recv, Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchange(recv, "foo", Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchange(0, "foo", "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.compareAndExchange(recv, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchange(recv, "foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchange();
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchange(recv, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(null, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(recv, Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(recv, "foo", Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(0, "foo", "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.compareAndExchangeAcquire(recv, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, "foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(recv, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(null, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(recv, Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(recv, "foo", Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(0, "foo", "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.compareAndExchangeRelease(recv, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, "foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(recv, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getAndSet(null, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSet(Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSet(0, "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.getAndSet(recv, "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSet(recv, "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSet();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSet(recv, "foo", Void.class);
        });

        checkNPE(() -> { 
            String x = (String) vh.getAndSetAcquire(null, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSetAcquire(Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetAcquire(0, "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.getAndSetAcquire(recv, "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetAcquire(recv, "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetAcquire(recv, "foo", Void.class);
        });

        checkNPE(() -> { 
            String x = (String) vh.getAndSetRelease(null, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSetRelease(Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetRelease(0, "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.getAndSetRelease(recv, "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetRelease(recv, "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetRelease();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetRelease(recv, "foo", Void.class);
        });


    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeString recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            checkNPE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class)).
                    invokeExact((VarHandleTestMethodTypeString) null);
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, int.class)).
                    invokeExact(0);
            });
            hs.checkWMTEOrCCE(() -> { 
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeString.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            checkNPE(() -> { 
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact((VarHandleTestMethodTypeString) null, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                hs.get(am, methodType(void.class, Class.class, String.class)).
                    invokeExact(Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeString.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int.class, String.class)).
                    invokeExact(0, "foo");
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeString.class, String.class, Class.class)).
                    invokeExact(recv, "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            checkNPE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, String.class, String.class)).
                    invokeExact((VarHandleTestMethodTypeString) null, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, String.class, String.class)).
                    invokeExact(Void.class, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, Class.class, String.class)).
                    invokeExact(recv, Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, String.class, Class.class)).
                    invokeExact(recv, "foo", Void.class);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , String.class, String.class)).
                    invokeExact(0, "foo", "foo");
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, String.class, String.class, Class.class)).
                    invokeExact(recv, "foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class, String.class)).
                    invokeExact((VarHandleTestMethodTypeString) null, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, Class.class, String.class, String.class)).
                    invokeExact(Void.class, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, Class.class, String.class)).
                    invokeExact(recv, Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class, Class.class)).
                    invokeExact(recv, "foo", Void.class);
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, int.class , String.class, String.class)).
                    invokeExact(0, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeString.class , String.class, String.class)).
                    invokeExact(recv, "foo", "foo");
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class , String.class, String.class)).
                    invokeExact(recv, "foo", "foo");
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class, String.class, Class.class)).
                    invokeExact(recv, "foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact((VarHandleTestMethodTypeString) null, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, Class.class, String.class)).
                    invokeExact(Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, int.class, String.class)).
                    invokeExact(0, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact(recv, "foo");
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact(recv, "foo");
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact(recv, "foo", Void.class);
            });
        }


    }


    static void testStaticFieldWrongMethodType(VarHandle vh) throws Throwable {
        checkCCE(() -> { 
            Void x = (Void) vh.get();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.get();
        });
        checkWMTE(() -> { 
            String x = (String) vh.get(Void.class);
        });


        checkCCE(() -> { 
            vh.set(Void.class);
        });
        checkWMTE(() -> { 
            vh.set();
        });
        checkWMTE(() -> { 
            vh.set("foo", Void.class);
        });


        checkCCE(() -> { 
            Void x = (Void) vh.getVolatile();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getVolatile();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getVolatile(Void.class);
        });


        checkCCE(() -> { 
            vh.setVolatile(Void.class);
        });
        checkWMTE(() -> { 
            vh.setVolatile();
        });
        checkWMTE(() -> { 
            vh.setVolatile("foo", Void.class);
        });


        checkCCE(() -> { 
            Void x = (Void) vh.getOpaque();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getOpaque();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getOpaque(Void.class);
        });


        checkCCE(() -> { 
            vh.setOpaque(Void.class);
        });
        checkWMTE(() -> { 
            vh.setOpaque();
        });
        checkWMTE(() -> { 
            vh.setOpaque("foo", Void.class);
        });


        checkCCE(() -> { 
            Void x = (Void) vh.getAcquire();
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAcquire();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAcquire(Void.class);
        });


        checkCCE(() -> { 
            vh.setRelease(Void.class);
        });
        checkWMTE(() -> { 
            vh.setRelease();
        });
        checkWMTE(() -> { 
            vh.setRelease("foo", Void.class);
        });


        checkCCE(() -> { 
            boolean r = vh.compareAndSet(Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet("foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet("foo", "foo", Void.class);
        });


        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain("foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain("foo", "foo", Void.class);
        });


        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet("foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet("foo", "foo", Void.class);
        });


        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire("foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire("foo", "foo", Void.class);
        });


        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease("foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease("foo", "foo", Void.class);
        });


        checkCCE(() -> { 
            String x = (String) vh.compareAndExchange(Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchange("foo", Void.class);
        });
        checkCCE(() -> { 
            Void r = (Void) vh.compareAndExchange("foo", "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchange("foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchange();
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchange("foo", "foo", Void.class);
        });


        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire("foo", Void.class);
        });
        checkCCE(() -> { 
            Void r = (Void) vh.compareAndExchangeAcquire("foo", "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeAcquire("foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire("foo", "foo", Void.class);
        });


        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeRelease("foo", Void.class);
        });
        checkCCE(() -> { 
            Void r = (Void) vh.compareAndExchangeRelease("foo", "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeRelease("foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeRelease("foo", "foo", Void.class);
        });


        checkCCE(() -> { 
            String x = (String) vh.getAndSet(Void.class);
        });
        checkCCE(() -> { 
            Void r = (Void) vh.getAndSet("foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSet("foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSet();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSet("foo", Void.class);
        });


        checkCCE(() -> { 
            String x = (String) vh.getAndSetAcquire(Void.class);
        });
        checkCCE(() -> { 
            Void r = (Void) vh.getAndSetAcquire("foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetAcquire("foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetAcquire("foo", Void.class);
        });


        checkCCE(() -> { 
            String x = (String) vh.getAndSetRelease(Void.class);
        });
        checkCCE(() -> { 
            Void r = (Void) vh.getAndSetRelease("foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetRelease("foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetRelease();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetRelease("foo", Void.class);
        });


    }

    static void testStaticFieldWrongMethodType(Handles hs) throws Throwable {
        int i = 0;

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            hs.checkWMTEOrCCE(() -> { 
                Void x = (Void) hs.get(am, methodType(Void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(Class.class)).
                    invokeExact(Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            hs.checkWMTEOrCCE(() -> { 
                hs.get(am, methodType(void.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, String.class, Class.class)).
                    invokeExact("foo", Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, String.class)).
                    invokeExact(Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String.class, Class.class)).
                    invokeExact("foo", Void.class);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String.class, String.class, Class.class)).
                    invokeExact("foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, Class.class, String.class)).
                    invokeExact(Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String.class, Class.class)).
                    invokeExact("foo", Void.class);
            });
            hs.checkWMTEOrCCE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, String.class, String.class)).
                    invokeExact("foo", "foo");
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String.class, String.class)).
                    invokeExact("foo", "foo");
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String.class, String.class, Class.class)).
                    invokeExact("foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, Class.class)).
                    invokeExact(Void.class);
            });
            hs.checkWMTEOrCCE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, String.class)).
                    invokeExact("foo");
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String.class)).
                    invokeExact("foo");
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String.class, Class.class)).
                    invokeExact("foo", Void.class);
            });
        }


    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        String[] array = new String[10];
        Arrays.fill(array, "foo");

        checkNPE(() -> { 
            String x = (String) vh.get(null, 0);
        });
        checkCCE(() -> { 
            String x = (String) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.get(0, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.get(array, Void.class);
        });
        checkCCE(() -> { 
            Void x = (Void) vh.get(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.get(array, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.get();
        });
        checkWMTE(() -> { 
            String x = (String) vh.get(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.set(null, 0, "foo");
        });
        checkCCE(() -> { 
            vh.set(Void.class, 0, "foo");
        });
        checkCCE(() -> { 
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.set(0, 0, "foo");
        });
        checkWMTE(() -> { 
            vh.set(array, Void.class, "foo");
        });
        checkWMTE(() -> { 
            vh.set();
        });
        checkWMTE(() -> { 
            vh.set(array, 0, "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { 
            String x = (String) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getVolatile(array, Void.class);
        });
        checkCCE(() -> { 
            Void x = (Void) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getVolatile();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getVolatile(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.setVolatile(null, 0, "foo");
        });
        checkCCE(() -> { 
            vh.setVolatile(Void.class, 0, "foo");
        });
        checkCCE(() -> { 
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.setVolatile(0, 0, "foo");
        });
        checkWMTE(() -> { 
            vh.setVolatile(array, Void.class, "foo");
        });
        checkWMTE(() -> { 
            vh.setVolatile();
        });
        checkWMTE(() -> { 
            vh.setVolatile(array, 0, "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { 
            String x = (String) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getOpaque(array, Void.class);
        });
        checkCCE(() -> { 
            Void x = (Void) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getOpaque();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getOpaque(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.setOpaque(null, 0, "foo");
        });
        checkCCE(() -> { 
            vh.setOpaque(Void.class, 0, "foo");
        });
        checkCCE(() -> { 
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.setOpaque(0, 0, "foo");
        });
        checkWMTE(() -> { 
            vh.setOpaque(array, Void.class, "foo");
        });
        checkWMTE(() -> { 
            vh.setOpaque();
        });
        checkWMTE(() -> { 
            vh.setOpaque(array, 0, "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { 
            String x = (String) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAcquire(array, Void.class);
        });
        checkCCE(() -> { 
            Void x = (Void) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAcquire();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAcquire(array, 0, Void.class);
        });


        checkNPE(() -> { 
            vh.setRelease(null, 0, "foo");
        });
        checkCCE(() -> { 
            vh.setRelease(Void.class, 0, "foo");
        });
        checkCCE(() -> { 
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            vh.setRelease(0, 0, "foo");
        });
        checkWMTE(() -> { 
            vh.setRelease(array, Void.class, "foo");
        });
        checkWMTE(() -> { 
            vh.setRelease();
        });
        checkWMTE(() -> { 
            vh.setRelease(array, 0, "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.compareAndSet(null, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.compareAndSet(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, Void.class, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.compareAndSet(array, 0, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(null, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetPlain(array, 0, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSet(null, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSet(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, Void.class, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSet(array, 0, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(null, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetAcquire(array, 0, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(null, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { 
            boolean r = vh.weakCompareAndSetRelease(array, 0, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.compareAndExchange(null, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchange(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchange(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchange(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchange(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchange(array, Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.compareAndExchange(array, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchange(array, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchange();
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchange(array, 0, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(null, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(array, Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeAcquire(array, 0, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(null, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(array, Void.class, "foo", "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, "foo", "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { 
            String x = (String) vh.compareAndExchangeRelease(array, 0, "foo", "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getAndSet(null, 0, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSet(Void.class, 0, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSet(0, 0, "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSet(array, Void.class, "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.getAndSet(array, 0, "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSet(array, 0, "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSet();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSet(array, 0, "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getAndSetAcquire(null, 0, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSetAcquire(Void.class, 0, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetAcquire(0, 0, "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetAcquire(array, Void.class, "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.getAndSetAcquire(array, 0, "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetAcquire(array, 0, "foo", Void.class);
        });


        checkNPE(() -> { 
            String x = (String) vh.getAndSetRelease(null, 0, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSetRelease(Void.class, 0, "foo");
        });
        checkCCE(() -> { 
            String x = (String) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetRelease(0, 0, "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetRelease(array, Void.class, "foo");
        });
        checkCCE(() -> { 
            Void r = (Void) vh.getAndSetRelease(array, 0, "foo");
        });
        checkWMTE(() -> { 
            boolean x = (boolean) vh.getAndSetRelease(array, 0, "foo");
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetRelease();
        });
        checkWMTE(() -> { 
            String x = (String) vh.getAndSetRelease(array, 0, "foo", Void.class);
        });


    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        String[] array = new String[10];
        Arrays.fill(array, "foo");

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            checkNPE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class)).
                    invokeExact((String[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            hs.checkWMTEOrCCE(() -> { 
                Void x = (Void) hs.get(am, methodType(Void.class, String[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            checkNPE(() -> { 
                hs.get(am, methodType(void.class, String[].class, int.class, String.class)).
                    invokeExact((String[]) null, 0, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                hs.get(am, methodType(void.class, Class.class, int.class, String.class)).
                    invokeExact(Void.class, 0, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                hs.get(am, methodType(void.class, String[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, int.class, int.class, String.class)).
                    invokeExact(0, 0, "foo");
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, String[].class, Class.class, String.class)).
                    invokeExact(array, Void.class, "foo");
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                hs.get(am, methodType(void.class, String[].class, int.class, Class.class)).
                    invokeExact(array, 0, "foo", Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            checkNPE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class, String.class)).
                    invokeExact((String[]) null, 0, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, String.class, String.class)).
                    invokeExact(Void.class, 0, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, Class.class, String.class)).
                    invokeExact(array, 0, Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", Void.class);
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, String.class, String.class)).
                    invokeExact(0, 0, "foo", "foo");
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, Class.class, String.class, String.class)).
                    invokeExact(array, Void.class, "foo", "foo");
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class, String.class)).
                    invokeExact((String[]) null, 0, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, Class.class, int.class, String.class, String.class)).
                    invokeExact(Void.class, 0, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, Class.class, String.class)).
                    invokeExact(array, 0, Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", Void.class);
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, int.class, int.class, String.class, String.class)).
                    invokeExact(0, 0, "foo", "foo");
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, Class.class, String.class, String.class)).
                    invokeExact(array, Void.class, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, String[].class, int.class, String.class, String.class)).
                    invokeExact(array, 0, "foo", "foo");
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class, String.class)).
                    invokeExact(array, 0, "foo", "foo");
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class)).
                    invokeExact((String[]) null, 0, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, Class.class, int.class, String.class)).
                    invokeExact(Void.class, 0, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, int.class, int.class, String.class)).
                    invokeExact(0, 0, "foo");
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, Class.class, String.class)).
                    invokeExact(array, Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { 
                Void r = (Void) hs.get(am, methodType(Void.class, String[].class, int.class, String.class)).
                    invokeExact(array, 0, "foo");
            });
            checkWMTE(() -> { 
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class)).
                    invokeExact(array, 0, "foo");
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { 
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", Void.class);
            });
        }


    }
}
