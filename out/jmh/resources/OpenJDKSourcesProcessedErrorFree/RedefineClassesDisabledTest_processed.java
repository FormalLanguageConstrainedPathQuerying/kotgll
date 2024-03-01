/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4882798
 * @summary test that redefineClasses and isRedefineClassesSupported behave correctly when redefine is not enabled
 * @author Robert Field, Sun Microsystems -- as modified from the work of Gabriel Adauto, Wily Technology
 *
 * @run build RedefineClassesDisabledTest
 * @run shell RedefineSetUp.sh
 * @run shell MakeJAR.sh basicAgent
 * @run main/othervm -javaagent:basicAgent.jar RedefineClassesDisabledTest RedefineClassesDisabledTest
 */

import java.io.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
public class
RedefineClassesDisabledTest
    extends ASimpleInstrumentationTestCase
{

    /**
     * Constructor for RedefineClassesDisabledTest.
     * @param name
     */
    public RedefineClassesDisabledTest(String name)
    {
        super(name);
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new RedefineClassesDisabledTest(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        testIsRedefineClassesSupported();
        testSimpleRedefineClasses();
    }


    public void
    testIsRedefineClassesSupported()
    {
        boolean canRedef = fInst.isRedefineClassesSupported();
        assertTrue("Can redefine classes flag set incorrectly (true)", !canRedef);
    }

    public void
    testSimpleRedefineClasses()
        throws Throwable
    {
        ExampleRedefine ex = new ExampleRedefine();

        int firstGet = ex.get();
        ex.doSomething();
        int secondGet = ex.get();

        assertEquals(firstGet, secondGet);


        File f = new File("Different_ExampleRedefine.class");
        System.out.println("Reading test class from " + f);
        InputStream redefineStream = new FileInputStream(f);

        byte[] redefineBuffer = NamedBuffer.loadBufferFromStream(redefineStream);

        ClassDefinition redefineParamBlock = new ClassDefinition(   ExampleRedefine.class,
                                                                    redefineBuffer);

        boolean caught = false;
        try {
            fInst.redefineClasses(new ClassDefinition[] {redefineParamBlock});
        } catch (UnsupportedOperationException uoe) {
            caught = true;
        }
        assertTrue(caught);
    }

}
