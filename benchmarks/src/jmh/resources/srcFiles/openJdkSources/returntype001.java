/*
 * Copyright (c) 2001, 2024, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Method.returnType;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * Method.                                                      <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.Method.returnType()</code>                 <BR>
 * complies with its spec when a type is one of PrimitiveTypes. <BR>
 * <BR>
 * The cases for testing are as follows.                <BR>
 *                                                      <BR>
 * When a gebuggee creates an object of                 <BR>
 * the following class type:                            <BR>
 *    class returntype001aTestClass {                   <BR>
 *        public boolean bl () { return false; }        <BR>
 *        public byte    bt () { return 0;     }        <BR>
 *        public char    ch () { return 0;     }        <BR>
 *        public double  db () { return 0.0d;  }        <BR>
 *        public float   fl () { return 0.0f;  }        <BR>
 *        public int     in () { return 0;     }        <BR>
 *        public long    ln () { return 0;     }        <BR>
 *        public short   sh () { return 0;     }        <BR>
 *   }                                                  <BR>
 *                                                      <BR>
 * for all of the above primitive type return methods,  <BR>
 * a debugger forms their corresponding Type objects.   <BR>
 * <BR>
 */

public class returntype001 {

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    static final String
    sHeader1 = "\n==> nsk/jdi/Method/returnType/returntype001",
    sHeader2 = "--> returntype001: ",
    sHeader3 = "##> returntype001: ";


    public static void main (String argv[]) {
        int result = run(argv, System.out);
        if (result != 0) {
            throw new RuntimeException("TEST FAILED with result " + result);
        }
    }

    public static int run (String argv[], PrintStream out) {
        return new returntype001().runThis(argv, out);
    }



    private static Log  logHandler;

    private static void log1(String message) {
        logHandler.display(sHeader1 + message);
    }
    private static void log2(String message) {
        logHandler.display(sHeader2 + message);
    }
    private static void log3(String message) {
        logHandler.complain(sHeader3 + message);
    }


    private String debuggeeName =
        "nsk.jdi.Method.returnType.returntype001a";

    String mName = "nsk.jdi.Method.returnType";


    static ArgumentHandler      argsHandler;
    static int                  testExitCode = PASSED;


    private int runThis (String argv[], PrintStream out) {

        Debugee debuggee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");  
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);            
        }

        IOPipe pipe     = new IOPipe(debuggee);

        debuggee.redirectStderr(out);
        log2("returntype001a debuggee launched");
        debuggee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        VirtualMachine vm = debuggee.VM();

        log1("      TESTING BEGINS");

        for (int i = 0; ; i++) {
        pipe.println("newcheck");
            line = pipe.readln();

            if (line.equals("checkend")) {
                log2("     : returned string is 'checkend'");
                break ;
            } else if (!line.equals("checkready")) {
                log3("ERROR: returned string is not 'checkready'");
                testExitCode = FAILED;
                break ;
            }

            log1("new check: #" + i);


            List listOfDebuggeeClasses = vm.classesByName(mName + ".returntype001aTestClass");
                if (listOfDebuggeeClasses.size() != 1) {
                    testExitCode = FAILED;
                    log3("ERROR: listOfDebuggeeClasses.size() != 1");
                    break ;
                }

            List   methods = null;
            Method m       = null;

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:                 

                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("bl");
                        m = (Method) methods.get(0);
                        try {
                            BooleanType blType = (BooleanType) m.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (BooleanType) m.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (BooleanType) m.returnType();");
                            expresult = 1;
                            break;
                        }
                        break;

                case 1:                 
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("bt");
                        m = (Method) methods.get(0);
                        try {
                            ByteType btType = (ByteType) m.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (ByteType) m.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (ByteType) m.returnType();");
                            expresult = 1;
                            break;
                        }
                        break;

                case 2:                 
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("ch");
                        m = (Method) methods.get(0);
                        try {
                            CharType chType = (CharType) m.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (CharType) m.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (CharType) m.returnType();");
                            expresult = 1;
                            break;
                        }
                        break;

                case 3:                 
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("db");
                        m = (Method) methods.get(0);
                        try {
                            DoubleType dbType = (DoubleType) m.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (DoublerType) m.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (DoubleType) m.returnType();");
                            expresult = 1;
                            break;
                        }
                        break;

                case 4:                 
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("fl");
                        m = (Method) methods.get(0);
                        try {
                            FloatType flType = (FloatType) m.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (FloatType) m.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (FloatType) m.returnType();");
                            expresult = 1;
                            break;
                        }
                        break;

                case 5:                 
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("in");
                        m = (Method) methods.get(0);
                        try {
                            IntegerType inType = (IntegerType) m.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (IntegerType) m.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (IntegerType) m.returnType();");
                            expresult = 1;
                            break;
                        }
                        break;

                case 6:                 
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("ln");
                        m = (Method) methods.get(0);
                        try {
                            LongType lnType = (LongType) m.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (LongType) m.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (LongType) m.returnType();");
                            expresult = 1;
                            break;
                        }
                        break;

                case 7:                 
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("sh");
                        m = (Method) methods.get(0);
                        try {
                            ShortType shType = (ShortType) m.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (ShortType) m.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (ShortType) m.returnType();");
                            expresult = 1;
                            break;
                        }
                        break;


                default: expresult = 2;
                         break ;
                }

                if (expresult == 2) {
                    log2("      test cases finished");
                    break ;
                } else if (expresult == 1) {
                    log3("ERROR: expresult != true;  check # = " + i);
                    testExitCode = FAILED;
                }
            }
        }
        log1("      TESTING ENDS");


        pipe.println("quit");
        log2("waiting for the debuggee to finish ...");
        debuggee.waitFor();

        int status = debuggee.getStatus();
        if (status != PASSED + PASS_BASE) {
            log3("debuggee returned UNEXPECTED exit status: " +
                    status + " != PASS_BASE");
            testExitCode = FAILED;
        } else {
            log2("debuggee returned expected exit status: " +
                    status + " == PASS_BASE");
        }

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;
    }
}
