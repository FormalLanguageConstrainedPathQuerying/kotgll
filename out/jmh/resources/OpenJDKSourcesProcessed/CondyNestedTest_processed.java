/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186046
 * @summary Test nested dynamic constant declarations that are recursive
 * @compile CondyNestedTest_Code.jcod
 * @enablePreview
 * @run testng CondyNestedTest
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyNestedTest
 */

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CondyNestedTest {

    static final Class[] THROWABLES = {InvocationTargetException.class, StackOverflowError.class};
    private static final MethodHandles.Lookup L = MethodHandles.lookup();

    Class<?> c;


    static void test(Method m, Class<? extends Throwable>... ts) {
        Throwable caught = null;
        try {
            m.invoke(null);
        } catch (Throwable t) {
            caught = t;
        }

        if (caught == null) {
            Assert.fail("Throwable expected");
        }

        String actualMessage = null;
        for (int i = 0; i < ts.length; i++) {
            actualMessage = caught.getMessage();
            Assert.assertNotNull(caught);
            Assert.assertTrue(ts[i].isAssignableFrom(caught.getClass()));
            caught = caught.getCause();
        }
    }

    @BeforeClass
    public void findClass() throws Exception {
        c = Class.forName("CondyNestedTest_Code");
    }

    /**
     * Testing an ldc of a dynamic constant, C say, with a BSM whose static
     * argument is C.
     */
    @Test
    public void testCondyBsmCondyBsm() throws Exception {
        test("condy_bsm_condy_bsm", THROWABLES);
    }

    /**
     * Testing an invokedynamic with a BSM whose static argument is a constant
     * dynamic, C say, with a BSM whose static argument is C.
     */
    @Test
    public void testIndyBsmIndyCondyBsm() throws Exception {
        test("indy_bsmIndy_condy_bsm", THROWABLES);
    }

    /**
     * Testing an invokedynamic with a BSM, B say, whose static argument is
     * a dynamic constant, C say, that uses BSM B.
     */
    @Test
    public void testIndyBsmCondyBsm() throws Exception {
        test("indy_bsm_condy_bsm", THROWABLES);
    }

    void test(String methodName, Class<? extends Throwable>... ts) throws Exception {
        Method m = c.getMethod(methodName);
        m.setAccessible(true);
        test(m, ts);
    }

}
