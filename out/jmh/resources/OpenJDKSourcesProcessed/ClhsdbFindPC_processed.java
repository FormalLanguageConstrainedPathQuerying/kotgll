/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Platform;
import jdk.test.lib.util.CoreUtils;
import jtreg.SkippedException;

/**
 * @test id=xcomp-process
 * @bug 8193124
 * @summary Test the clhsdb 'findpc' command with Xcomp on live process
 * @requires vm.compMode != "Xcomp"
 * @requires vm.hasSA
 * @requires vm.compiler1.enabled
 * @requires vm.opt.DeoptimizeALot != true
 * @library /test/lib
 * @run main/othervm/timeout=480 ClhsdbFindPC true false
 */

/**
 * @test id=xcomp-core
 * @bug 8193124
 * @summary Test the clhsdb 'findpc' command with Xcomp on core file
 * @requires vm.compMode != "Xcomp"
 * @requires vm.hasSA
 * @requires vm.compiler1.enabled
 * @requires vm.opt.DeoptimizeALot != true
 * @library /test/lib
 * @run main/othervm/timeout=480 ClhsdbFindPC true true
 */

/**
 * @test id=no-xcomp-process
 * @bug 8193124
 * @summary Test the clhsdb 'findpc' command w/o Xcomp on live process
 * @requires vm.hasSA
 * @requires vm.compiler1.enabled
 * @library /test/lib
 * @run main/othervm/timeout=480 ClhsdbFindPC false false
 */

/**
 * @test id=no-xcomp-core
 * @bug 8193124
 * @summary Test the clhsdb 'findpc' command w/o Xcomp on core file
 * @requires vm.hasSA
 * @requires vm.compiler1.enabled
 * @library /test/lib
 * @run main/othervm/timeout=480 ClhsdbFindPC false true
 */

public class ClhsdbFindPC {
    static LingeredApp theApp = null;
    static String coreFileName = null;
    static ClhsdbLauncher test = null;

    private static void testFindPC(boolean withXcomp, boolean withCore) throws Exception {
        try {
            String linesep = System.getProperty("line.separator");
            String segvAddress = null;
            List<String> cmds = null;
            String cmdStr = null;
            Map<String, List<String>> expStrMap = null;

            test = new ClhsdbLauncher();

            theApp = new LingeredApp();
            theApp.setForceCrash(withCore);
            if (withXcomp) {
                LingeredApp.startApp(theApp, "-Xcomp", CoreUtils.getAlwaysPretouchArg(withCore));
            } else {
                LingeredApp.startApp(theApp, "-Xint", CoreUtils.getAlwaysPretouchArg(withCore));
            }
            System.out.print("Started LingeredApp ");
            if (withXcomp) {
                System.out.print("(-Xcomp) ");
            } else {
                System.out.print("(-Xint) ");
            }
            System.out.println("with pid " + theApp.getPid());

            if (withCore) {
                String crashOutput = theApp.getOutput().getStdout();
                coreFileName = CoreUtils.getCoreFileLocation(crashOutput, theApp.getPid());
                String[] parts = crashOutput.split(" pc=");
                String[] tokens = parts[1].split(",");
                segvAddress = tokens[0];

                cmds = new ArrayList<String>();
                cmdStr = "findpc " + segvAddress;
                cmds.add(cmdStr);
                expStrMap = new HashMap<>();
                if (Platform.isOSX()) {
                    expStrMap.put(cmdStr, List.of("In unknown location"));
                } else { 
                    expStrMap.put(cmdStr, List.of("Java_jdk_test_lib_apps_LingeredApp_crash"));
                }
                runTest(withCore, cmds, expStrMap);
            }

            cmds = List.of("jstack -v");
            String jStackOutput = runTest(withCore, cmds, null);

            String pcAddress = null;
            String[] parts = jStackOutput.split("LingeredApp.steadyState");
            String[] tokens = parts[1].split(" ");
            for (String token : tokens) {
                if (token.contains("pc")) {
                    String[] addresses = token.split("=");
                    pcAddress = addresses[1].replace(",","");
                    break;
                }
            }
            if (pcAddress == null) {
                throw new RuntimeException("Cannot find LingeredApp.steadyState pc in output");
            }

            cmds = new ArrayList<String>();
            cmdStr = "findpc " + pcAddress;
            cmds.add(cmdStr);
            expStrMap = new HashMap<>();
            if (withXcomp) {
                expStrMap.put(cmdStr, List.of(
                            "In code in NMethod for jdk/test/lib/apps/LingeredApp.steadyState",
                            "content:",
                            "oops:",
                            "frame size:"));
            } else {
                expStrMap.put(cmdStr, List.of(
                            "In interpreter codelet"));
            }
            runTest(withCore, cmds, expStrMap);

            parts = jStackOutput.split("LingeredApp.steadyState");
            parts = parts[1].split("Method\\*=");
            parts = parts[1].split(" ");
            String methodAddr = parts[0];
            cmdStr = "findpc " + methodAddr;
            cmds = List.of(cmdStr);
            expStrMap = new HashMap<>();
            expStrMap.put(cmdStr, List.of("Method ",
                                          "LingeredApp.steadyState",
                                          methodAddr));
            runTest(withCore, cmds, expStrMap);

            cmdStr = "whatis " + methodAddr;
            cmds = List.of(cmdStr);
            expStrMap = new HashMap<>();
            expStrMap.put(cmdStr, List.of("Method ",
                                          "LingeredApp.steadyState",
                                          methodAddr));
            runTest(withCore, cmds, expStrMap);

            cmdStr = "mem -v " + methodAddr + "/30";
            cmds = List.of(cmdStr);
            expStrMap = new HashMap<>();
            expStrMap.put(cmdStr, List.of("Method jdk/test/lib/apps/LingeredApp.steadyState",
                                          methodAddr,
                                          /* The following is from a field in the Method object. */
                                          "In interpreter codelet: method entry point"));
            runTest(withCore, cmds, expStrMap);

            parts = jStackOutput.split("tid=");
            parts = parts[1].split(" ");
            String tid = parts[0];  
            cmdStr = "findpc " + tid;
            cmds = List.of(cmdStr);
            expStrMap = new HashMap<>();
            expStrMap.put(cmdStr, List.of("Is of type JavaThread"));
            runTest(withCore, cmds, expStrMap);

            String badAddress = tid.substring(0, tid.length() - 3) + "ffc";
            cmdStr = "findpc " + badAddress;
            cmds = List.of(cmdStr);
            expStrMap = new HashMap<>();
            expStrMap.put(cmdStr, List.of("In unknown location"));
            runTest(withCore, cmds, expStrMap);

            parts = jStackOutput.split("tid=");
            parts = parts[1].split(" \\[");
            parts = parts[1].split("\\]");
            String stackAddress = parts[0];  
            if (Long.decode(stackAddress) == 0L) {
                System.out.println("Stack address is " + stackAddress + ". Skipping test.");
            } else {
                cmdStr = "findpc " + stackAddress;
                cmds = List.of(cmdStr);
                expStrMap = new HashMap<>();
                expStrMap.put(cmdStr, List.of("(In java stack)|(Is of type)"));
                runTest(withCore, cmds, expStrMap);
            }

            cmds = List.of("examine " + tid);
            String examineOutput = runTest(withCore, cmds, null);
            parts = examineOutput.split(tid + ": ");
            String value = parts[1].split(linesep)[0];
            cmdStr = "findpc " + value;
            cmds = List.of(cmdStr);
            expStrMap = new HashMap<>();
            if (Platform.isWindows()) {
                expStrMap.put(cmdStr, List.of("jvm.+JavaThread"));
            } else if (Platform.isOSX()) {
                if (withCore) {
                    expStrMap.put(cmdStr, List.of("__ZTV10JavaThread"));
                } else { 
                    expStrMap.put(cmdStr, List.of("In unknown location"));
                }
            } else {
                expStrMap.put(cmdStr, List.of("vtable for JavaThread"));
            }
            String findpcOutput = runTest(withCore, cmds, expStrMap);

            boolean hasSymbols = true;
            if (Platform.isWindows()) {
                if (findpcOutput.indexOf("jvm!JavaThread::`vftable'") == -1) {
                    hasSymbols = false;
                }
            }

            String symbol = "MaxJNILocalCapacity";
            cmds = List.of("findsym " + symbol);
            expStrMap = new HashMap<>();
            if (!hasSymbols) {
                expStrMap.put(cmdStr, List.of("Symbol not found"));
            }
            String findsymOutput = runTest(withCore, cmds, expStrMap);
            if (hasSymbols) {
                parts = findsymOutput.split("findsym " + symbol + linesep);
                parts = parts[1].split(":");
                String findsymAddress = parts[0].split(linesep)[0];
                cmdStr = "findpc " + findsymAddress;
                cmds = List.of(cmdStr);
                expStrMap = new HashMap<>();
                if (Platform.isOSX() && !withCore) {
                    expStrMap.put(cmdStr, List.of("Address " + findsymAddress + ": In unknown location"));
                } else {
                    expStrMap.put(cmdStr, List.of("Address " + findsymAddress + ": .*" + symbol));
                }
                runTest(withCore, cmds, expStrMap);
            }
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            if (!withCore) {
                LingeredApp.stopApp(theApp);
            }
        }
    }

    private static String runTest(boolean withCore, List<String> cmds, Map<String, List<String>> expStrMap)
        throws Exception
    {
        if (withCore) {
            return test.runOnCore(coreFileName, cmds, expStrMap, null);
        } else {
            return test.run(theApp.getPid(), cmds, expStrMap, null);
        }
    }

    public static void main(String[] args) throws Exception {
        boolean withXcomp = Boolean.parseBoolean(args[0]);
        boolean withCore = Boolean.parseBoolean(args[1]);
        System.out.println("Starting the ClhsdbFindPC test");
        testFindPC(withXcomp, withCore);
        System.out.println("Test PASSED");
    }
}
