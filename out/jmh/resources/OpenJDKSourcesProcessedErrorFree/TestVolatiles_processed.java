/*
 * Copyright (c) 2018, 2020, Red Hat, Inc. All rights reserved.
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * common code to run and validate tests of code generation for
 * volatile ops on AArch64
 *
 * incoming args are <testclass> <testtype>
 *
 * where <testclass> in {TestVolatileLoad,
 *                       TestVolatileStore,
 *                       TestUnsafeVolatileLoad,
 *                       TestUnsafeVolatileStore,
 *                       TestUnsafeVolatileCAS,
 *                       TestUnsafeVolatileWeakCAS,
 *                       TestUnsafeVolatileCAE,
 *                       TestUnsafeVolatileGAS}
 * and <testtype> in {G1,
 *                    Serial,
 *                    Parallel,
 *                    Shenandoah,
 *                    ShenandoahIU}
 */


package compiler.c2.aarch64;

import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.io.*;

import jdk.test.lib.Asserts;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.whitebox.WhiteBox;


public class TestVolatiles {
    public void runtest(String classname, String testType) throws Throwable {
        String fullclassname = "compiler.c2.aarch64." + classname;
        String[] procArgs;
        int argcount;
        switch(testType) {
        case "G1":
            argcount = 9;
            procArgs = new String[argcount];
            procArgs[argcount - 2] = "-XX:+UseG1GC";
            break;
        case "Parallel":
            argcount = 9;
            procArgs = new String[argcount];
            procArgs[argcount - 2] = "-XX:+UseParallelGC";
            break;
        case "Serial":
            argcount = 9;
            procArgs = new String[argcount];
            procArgs[argcount - 2] = "-XX:+UseSerialGC";
            break;
        case "Shenandoah":
            argcount = 10;
            procArgs = new String[argcount];
            procArgs[argcount - 3] = "-XX:+UnlockExperimentalVMOptions";
            procArgs[argcount - 2] = "-XX:+UseShenandoahGC";
            break;
        case "ShenandoahIU":
            argcount = 11;
            procArgs = new String[argcount];
            procArgs[argcount - 4] = "-XX:+UnlockExperimentalVMOptions";
            procArgs[argcount - 3] = "-XX:+UseShenandoahGC";
            procArgs[argcount - 2] = "-XX:ShenandoahGCMode=iu";
            break;
        default:
            throw new RuntimeException("unexpected test type " + testType);
        }



        procArgs[0] = "-XX:+UseCompressedOops";
        procArgs[1] = "-XX:-BackgroundCompilation";
        procArgs[2] = "-XX:-TieredCompilation";
        procArgs[3] = "-XX:+PrintOptoAssembly";
        procArgs[4] = "-XX:CompileCommand=compileonly," + fullclassname + "::" + "test*";
        procArgs[5] = "--add-exports";
        procArgs[6] = "java.base/jdk.internal.misc=ALL-UNNAMED";
        procArgs[argcount - 1] = fullclassname;

        runtest(classname, testType, true, procArgs);

        if (!classname.equals("TestUnsafeVolatileGAA")) {
            procArgs[0] = "-XX:-UseCompressedOops";
            runtest(classname, testType, false, procArgs);
        }
    }


    public void runtest(String classname, String testType, boolean useCompressedOops, String[] procArgs) throws Throwable {
        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder(procArgs);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        output.stderrShouldBeEmptyIgnoreVMWarnings();
        output.stdoutShouldNotBeEmpty();
        output.shouldHaveExitValue(0);


        checkoutput(output, classname, testType, useCompressedOops);
    }

    private String skipTo(Iterator<String> iter, String substring)
    {
        while (iter.hasNext()) {
            String nextLine = iter.next();
            if (nextLine.matches(".*" + substring + ".*")) {
                return nextLine;
            }
        }
        return null;
    }


    private boolean checkCompile(Iterator<String> iter, String methodname, String[] expected, OutputAnalyzer output, boolean do_throw)
    {
        System.out.println("checkCompile(" + methodname + ",");
        String sepr = "  { ";
        for (String s : expected) {
            System.out.print(sepr);
            System.out.print(s);
            sepr = ",\n    ";
        }
        System.out.println(" })");

        String match = skipTo(iter, Pattern.quote("{method}"));
        if (match == null) {
            if (do_throw) {
                throw new RuntimeException("Missing compiler output for " + methodname + "!\n\n" + output.getOutput());
            }
            return false;
        }
        match = skipTo(iter, Pattern.quote("- name:"));
        if (match == null) {
            if (do_throw) {
                throw new RuntimeException("Missing compiled method name!\n\n" + output.getOutput());
            }
            return false;
        }
        if (!match.contains(methodname)) {
            if (do_throw) {
                throw new RuntimeException("Wrong method " + match + "!\n  -- expecting " + methodname + "\n\n" + output.getOutput());
            }
            return false;
        }
        for (String s : expected) {
            match = skipTo(iter, s);
            if (match == null) {
                if (do_throw) {
                    throw new RuntimeException("Missing expected output " + s + "!\n\n" + output.getOutput());
                }
                return false;
            }
        }
        return true;
    }


    private void checkload(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();


        String[] matches;
        matches = new String[] {
            "ldarw",
            "membar_acquire \\(elided\\)",
            "ret"
        };
        checkCompile(iter, "testInt", matches, output, true);

        matches = new String[] {
            useCompressedOops ? "ldarw?" : "ldar",
            "membar_acquire \\(elided\\)",
            "ret"
        };
        checkCompile(iter, "testObj", matches, output, true);

    }


    private void checkstore(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();

        String[] matches;

        matches = new String[] {
            "membar_release \\(elided\\)",
            "stlrw",
            "membar_volatile \\(elided\\)",
            "ret"
        };
        checkCompile(iter, "testInt", matches, output, true);

        switch (testType) {
        default:
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "stlrw?" : "stlr",
                "membar_volatile \\(elided\\)",
                "ret"
            };
            break;
        case "G1":
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "stlrw?" : "stlr",
                "membar_volatile \\(elided\\)",
                "ret",
                "membar_volatile",
                "dmb ish",
                "strb"
            };
            break;
        case "Shenandoah":
        case "ShenandoahIU":
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "stlrw?" : "stlr",
                "membar_volatile \\(elided\\)",
                "ret"
            };
            break;
        }

        checkCompile(iter, "testObj", matches, output, true);
    }


    private void checkcas(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();

        String[] matches;
        String[][] tests = {
            { "testInt", "cmpxchgw" },
            { "testLong", "cmpxchg" },
            { "testByte", "cmpxchgb" },
            { "testShort", "cmpxchgs" },
        };

        for (String[] test : tests) {
            matches = new String[] {
                "membar_release \\(elided\\)",
                test[1] + "_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            checkCompile(iter, test[0], matches, output, true);
        }

        switch (testType) {
        default:
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                "strb",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        case "G1":
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                "membar_acquire \\(elided\\)",
                "ret",
                "membar_volatile",
                "dmb ish",
                "strb"
            };
            break;
        case "Shenandoah":
        case "ShenandoahIU":
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq_shenandoah" : "cmpxchg_acq_shenandoah",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        }
        checkCompile(iter, "testObj", matches, output, true);
    }

    private void checkcae(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        ListIterator<String> iter = output.asLines().listIterator();

        String[] matches;
        String[][] tests = {
            { "testInt", "cmpxchgw" },
            { "testLong", "cmpxchg" },
            { "testByte", "cmpxchgb" },
            { "testShort", "cmpxchgs" },
        };

        for (String[] test : tests) {
            matches = new String[] {
                "membar_release \\(elided\\)",
                test[1] + "_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            checkCompile(iter, test[0], matches, output, true);
        }

        switch (testType) {
        default:
            matches = new String[] {
                "membar_release \\(elided\\)",
                "strb",
                useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };

            int idx = iter.nextIndex();
            if (!checkCompile(iter, "testObj", matches, output, false)) {
                iter = output.asLines().listIterator(idx);

                matches = new String[] {
                    "membar_release \\(elided\\)",
                    useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                    "strb",
                    "membar_acquire \\(elided\\)",
                    "ret"
                };

                checkCompile(iter, "testObj", matches, output, true);
            }
            return;

        case "G1":
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                "membar_acquire \\(elided\\)",
                "ret",
                "membar_volatile",
                "dmb ish",
                "strb"
            };
            break;
        case "Shenandoah":
        case "ShenandoahIU":
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq_shenandoah" : "cmpxchg_acq_shenandoah",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        }
        checkCompile(iter, "testObj", matches, output, true);
    }

    private void checkgas(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();

        String[] matches;
        String[][] tests = {
            { "testInt", "atomic_xchgw" },
            { "testLong", "atomic_xchg" },
        };

        for (String[] test : tests) {
            matches = new String[] {
                "membar_release \\(elided\\)",
                test[1] + "_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            checkCompile(iter, test[0], matches, output, true);
        }

        switch (testType) {
        default:
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "atomic_xchgw?_acq" : "atomic_xchg_acq",
                "strb",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        case "G1":
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "atomic_xchgw?_acq" : "atomic_xchg_acq",
                "membar_acquire \\(elided\\)",
                "ret",
                "membar_volatile",
                "dmb ish",
                "strb"
            };
            break;
        case "Shenandoah":
        case "ShenandoahIU":
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "atomic_xchgw?_acq" : "atomic_xchg_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        }

        checkCompile(iter, "testObj", matches, output, true);
    }

    private void checkgaa(OutputAnalyzer output, String testType) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();

        String[] matches;
        String[][] tests = {
            { "testInt", "get_and_addI" },
            { "testLong", "get_and_addL" },
        };

        for (String[] test : tests) {
            matches = new String[] {
                "membar_release \\(elided\\)",
                test[1] + "_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            checkCompile(iter, test[0], matches, output, true);
        }

    }


    private void checkoutput(OutputAnalyzer output, String classname, String testType, boolean useCompressedOops) throws Throwable
    {
        System.out.println("checkoutput(" +
                           classname + ", " +
                           testType + ")\n" +
                           output.getOutput());

        switch (classname) {
        case "TestVolatileLoad":
            checkload(output, testType, useCompressedOops);
            break;
        case "TestVolatileStore":
            checkstore(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileLoad":
            checkload(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileStore":
            checkstore(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileCAS":
        case "TestUnsafeVolatileWeakCAS":
            checkcas(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileCAE":
            checkcae(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileGAS":
            checkgas(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileGAA":
            checkgaa(output, testType);
            break;
        }
    }
}
