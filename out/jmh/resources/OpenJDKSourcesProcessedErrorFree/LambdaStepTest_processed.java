/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test stepping through lambdas
 * @author Staffan Larsen
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g LambdaStepTest.java
 * @run driver LambdaStepTest
 */
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.StepEvent;

 /********** target program **********/

interface DefaultTest {
    default void defaultMethod() {
        String from = "default";
        System.out.println("Hello from " + from);
    }
}
class LambdaStepTestTarg implements DefaultTest {
    private void test() {
        String from = "test";
        System.out.println("Hello from " + from);
    }
    private static void instanceTest() {
        LambdaStepTestTarg l = new LambdaStepTestTarg();
        l.test();
    }
    private static void lambdaTest() {
        Runnable r = () -> {
            String from = "lambda";
            System.out.println("Hello from " + from);
        };
        r.run();
    }
    private static void defaultTest() {
        LambdaStepTestTarg l = new LambdaStepTestTarg();
        l.defaultMethod();
    }
    public static void main(String[] args) {
        instanceTest();
        lambdaTest();
        defaultTest();
        System.out.println("Goodbye from LambdaStepTestTarg!");
    }
}


 /********** test program **********/

public class LambdaStepTest extends TestScaffold {
    LambdaStepTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)
        throws Exception
    {
        new LambdaStepTest (args).startTests();
    }

    /********** test core **********/

    protected void runTests()
        throws Exception
    {

        BreakpointEvent bpe = startTo("LambdaStepTestTarg", "instanceTest", "()V");
        ThreadReference thread = bpe.thread();

        StepEvent se = stepOverLine(thread);
        System.out.println(se.thread().frame(0));

        se = stepIntoLine(thread);
        System.out.println(se.thread().frame(0));

        se = stepOverLine(thread);
        System.out.println(se.thread().frame(0));

        StackFrame frame = se.thread().frame(0);
        LocalVariable lv = frame.visibleVariableByName("from");
        System.out.println(lv);
        StringReference sr = (StringReference) frame.getValue(lv);
        if (!sr.value().equals("test")) {
            throw new Exception("Unexpected variable value in instanceTest: "+sr.value());
        }



        bpe = resumeTo("LambdaStepTestTarg", "lambdaTest", "()V");
        thread = bpe.thread();

        se = stepOverLine(thread);
        System.out.println(se.thread().frame(0));

        se = stepIntoLine(thread);
        System.out.println(se.thread().frame(0));

        se = stepOverLine(thread);
        System.out.println(se.thread().frame(0));

        frame = se.thread().frame(0);
        lv = frame.visibleVariableByName("from");
        System.out.println(lv);
        sr = (StringReference) frame.getValue(lv);
        if (!sr.value().equals("lambda")) {
            throw new Exception("Unexpected variable value in lambdaTest: "+sr.value());
        }



        bpe = resumeTo("LambdaStepTestTarg", "defaultTest", "()V");
        thread = bpe.thread();

        se = stepOverLine(thread);
        System.out.println(se.thread().frame(0));

        se = stepIntoLine(thread);
        System.out.println(se.thread().frame(0));

        se = stepOverLine(thread);
        System.out.println(se.thread().frame(0));

        frame = se.thread().frame(0);
        lv = frame.visibleVariableByName("from");
        System.out.println(lv);
        sr = (StringReference) frame.getValue(lv);
        if (!sr.value().equals("default")) {
            throw new Exception("Unexpected variable value in lambdaTest: "+sr.value());
        }


        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

    }
}
