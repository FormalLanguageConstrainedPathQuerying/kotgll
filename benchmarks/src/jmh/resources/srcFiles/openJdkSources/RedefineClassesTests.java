/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4882798 5067523
 * @summary insure redefine is supported. exercise a class, then redefine it and do it again
 * @author Gabriel Adauto, Wily Technology
 *
 * @run build RedefineClassesTests
 * @run shell RedefineSetUp.sh
 * @run shell MakeJAR.sh redefineAgent
 * @run main/othervm -javaagent:redefineAgent.jar RedefineClassesTests RedefineClassesTests
 */

import java.io.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
public class
RedefineClassesTests
    extends ASimpleInstrumentationTestCase
{

    /**
     * Constructor for RedefineClassesTests.
     * @param name
     */
    public RedefineClassesTests(String name)
    {
        super(name);
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new RedefineClassesTests(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        testIsRedefineClassesSupported();
        testSimpleRedefineClasses();
        testUnmodifiableClassException();
    }


    public void
    testIsRedefineClassesSupported()
    {
        boolean canRedef = fInst.isRedefineClassesSupported();
        assertTrue("Cannot redefine classes", canRedef);
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

        fInst.redefineClasses(new ClassDefinition[] {redefineParamBlock});

        int thirdGet = ex.get();
        ex.doSomething();
        int fourthGet = ex.get();
        assertEquals(thirdGet + 1, fourthGet);
    }

    public void
    testUnmodifiableClassException()
        throws Throwable
    {
        System.out.println("Testing UnmodifiableClassException");

        File f = new File("Different_ExampleRedefine.class");
        InputStream redefineStream = new FileInputStream(f);
        byte[] redefineBuffer = NamedBuffer.loadBufferFromStream(redefineStream);

        System.out.println("Try to redefine class for primitive type");
        try {
            ClassDefinition redefineParamBlock =
                new ClassDefinition( byte.class, redefineBuffer );
            fInst.redefineClasses(new ClassDefinition[] {redefineParamBlock});
            fail();
        } catch (UnmodifiableClassException x) {
        }

        System.out.println("Try to redefine class for array type");
        try {
            ClassDefinition redefineParamBlock =
                new ClassDefinition( byte[].class, redefineBuffer );
            fInst.redefineClasses(new ClassDefinition[] {redefineParamBlock});
            fail();
        } catch (UnmodifiableClassException x) {
        }

    }

}
