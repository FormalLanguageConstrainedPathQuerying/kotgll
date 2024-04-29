/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8002091
 * @summary Test options patterns for javac,javap and javadoc using
 * javac as a test launcher. Create a dummy javac and intercept options to check
 * reception of options as passed through the launcher without having to launch
 * javac. Only -J and -cp ./* options should be consumed by the launcher.
 * @modules jdk.compiler
 *          jdk.zipfs
 * @run main ToolsOpts
 * @author ssides
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ToolsOpts extends TestHelper {
    static String[][] optionPatterns = {
        {"-J-Xmx128m"},
        {"-J-version"},
        {"-J-XshowSettings:vm"},
        {"-J-Xdiag"},
        {"-J-showversion"},
        {"-J-version", "-option"},
        {"-option"},
        {"-option:sub"},
        {"-option:sub-"},
        {"-option:sub1,sub2"}, 
        {"-option:{sub1,sub2,sub3}"}, 
        {"-option:{{sub1,sub2,sub3}}"},
        {"-option/c:/export/date/tmp"},
        {"-option=value"},
        {"-Dpk1.pk2.pk3"}, 
        {"-Dpk1.pk2=value"}, 
        {"@<filename>"},
        {"-option", "http:
        {"-option", "name", "p1:p2.."},
        {"-All these non-options show launchers pass options as is to tool."},
        {"-option"},
        {"-option:sub"},
        {"-option:sub-"},
        {"-option", "<path>"},
        {"-option", "<file>"},
        {"-option", "<dir>"},
        {"-option", "http:
        {"-option", "<html code>"},
        {"-option", "name1:name2"},
        {"-option", "3"},
        {"option1", "-J-version", "option2"},
        {"option1", "-J-version", "-J-XshowSettings:vm", "option2"},};

    static void init() throws IOException {

        final String mainJava = "Main" + JAVA_FILE_EXT;
        List<String> contents = new ArrayList<>();
        contents.add("package com.sun.tools.javac;");
        contents.add("public class Main {");
        contents.add("    public static void main(String... args) {\n");
        contents.add("       for (String x : args) {\n");
        contents.add("           if(x.compareTo(\" \")!=0)\n");
        contents.add("               System.out.println(x);\n");
        contents.add("       }\n");
        contents.add("    }\n");
        contents.add("}\n");
        String mainJavaPath = "patch-src/com/sun/tools/javac/" + mainJava;
        File mainJavaFile = new File(mainJavaPath.replace('/', File.separatorChar));
        mainJavaFile.getParentFile().mkdirs();
        createFile(mainJavaFile, contents);

        new File("jdk.compiler").mkdir();
        compile("--patch-module", "jdk.compiler=patch-src",
                "-d", "jdk.compiler",
                mainJavaFile.toString());
    }

    static void pass(String msg) {
        System.out.println("pass: " + msg);
    }

    static void errout(String msg) {
        System.err.println(msg);
    }

    static int indexOfJoption(String[] opts) {
        for (int i = 0; i < opts.length; i++) {
            if (opts[i].startsWith("-J")) {
                return i;
            }
        }
        return -1;
    }

    /*
     * Check that J options a) are not passed to tool, and b) do the right thing,
     * that is, they should be passed to java launcher and work as expected.
     */
    static void checkJoptionOutput(TestResult tr, String[] opts) throws IOException {
        String jopts = "";
        for (String pat : opts) {
            jopts = jopts.concat(pat + " ");
            if (tr.contains("-J")) {
                throw new RuntimeException(
                        "failed: output should not contain option " + pat);
            }
            if (pat.compareTo("-J-version") == 0 ||
                    pat.compareTo("-J-showversion") == 0) {
                if (!tr.contains("java version") &&
                        !tr.contains("openjdk version")) {
                    throw new RuntimeException("failed: " + pat +
                            " should display a version string.");
                }
            } else if (pat.compareTo("-J-XshowSettings:VM") == 0) {
                if (!tr.contains("VM settings")) {
                    throw new RuntimeException("failed: " + pat +
                            " should have display VM settings.");
                }
            }
        }
        pass("Joption check: " + jopts);
    }

    /*
     * Feed each option pattern in optionPatterns array to javac launcher with
     * checking program preempting javac. Check that option received by 'dummy'
     * javac is the one passed on the command line.
     */
    static void runTestOptions() throws IOException {
        init();
        TestResult tr;
        int jpos = -1;
        String xPatch = "-J--patch-module=jdk.compiler=jdk.compiler";
        for (String arg[] : optionPatterns) {
            jpos = indexOfJoption(arg);
            String cmdString = javacCmd + " " + xPatch;
            for (String opt : arg) {
                cmdString = cmdString.concat(" " + opt);
            }
            switch (arg.length) {
                case 1:
                    tr = doExec(javacCmd, xPatch, arg[0]);
                    break;
                case 2:
                    tr = doExec(javacCmd, xPatch, arg[0], arg[1]);
                    break;
                case 3:
                    tr = doExec(javacCmd, xPatch, arg[0], arg[1], arg[2]);
                    break;
                case 4:
                    tr = doExec(javacCmd, xPatch, arg[0], arg[1], arg[2], arg[3]);
                    break;
                default:
                    tr = null;
                    break;
            }

            if (jpos > -1) {
                checkJoptionOutput(tr, arg);
                if (tr.contains(arg[jpos])) {
                    throw new RuntimeException(
                            "failed! Should not have passed -J option to tool.\n"
                            + "CMD: " + cmdString);
                }
            } else {
                int j = 0;
                List<String> output = tr.testOutput;
                for (int i = 0; i < arg.length; i++) {
                    boolean found = false;
                    for (; j < output.size(); j++) {
                        if (output.get(j).equals(arg[i])) {
                            pass("check " + output.get(j) + " == " + arg[i]);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new RuntimeException(
                                "failed! Should have passed non -J option [" + arg[i] + "] to tool.\n"
                                + "CMD: " + cmdString);
                    }
                }
            }
            pass(cmdString);
        }
    }

    public static void main(String... args) throws IOException {
        runTestOptions();
    }
}
