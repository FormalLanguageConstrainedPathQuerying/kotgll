/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2020 SAP SE. All rights reserved.
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
 * @summary Validate and test -?, -h and --help flags. All tools in the jdk
 *          should take the same flags to display the help message. These
 *          flags should be documented in the printed help message. The
 *          tool should quit without error code after displaying the
 *          help message (if there  is no other problem with the command
 *          line).
 *          Also check that tools that used to accept -help still do
 *          so. Test that tools that never accepted -help don't do so
 *          in future. I.e., check that the tool returns with the same
 *          return code as called with an invalid flag, and does not
 *          print anything containing '-help' in that case.
 * @compile HelpFlagsTest.java
 * @run main HelpFlagsTest
 */

import java.io.File;

public class HelpFlagsTest extends TestHelper {

    static final String[] TOOLS_NOT_TO_TEST = {
        "appletviewer",     
        "jaccessinspector", 
        "jaccessinspector-32", 
        "jaccesswalker",    
        "jaccesswalker-32", 
        "jconsole",         
        "servertool",       
        "javaw",            
        "kinit",
        "klist",
        "ktab",
        "javacpl",
        "jmc",
        "jweblauncher",
        "jcontrol",
        "ssvagent"
    };

    private static class ToolHelpSpec {
        String toolname;

        boolean supportsQuestionMark;
        boolean supportsH;
        boolean supportsHelp;

        int exitcodeOfHelp;

        boolean supportsLegacyHelp;

        boolean documentsLegacyHelp;

        int exitcodeOfWrongFlag;

        ToolHelpSpec(String n, int q, int h, int hp, int ex1, int l, int dl, int ex2) {
            toolname = n;
            supportsQuestionMark = ( q  == 1 ? true : false );
            supportsH            = ( h  == 1 ? true : false );
            supportsHelp         = ( hp == 1 ? true : false );
            exitcodeOfHelp       = ex1;

            supportsLegacyHelp   = (  l == 1 ? true : false );
            documentsLegacyHelp  = ( dl == 1 ? true : false );
            exitcodeOfWrongFlag  = ex2;
        }
    }

    static ToolHelpSpec[] jdkTools = {
        new ToolHelpSpec("jabswitch",   0,   0,   0,   0,         0,    0,     0),     
        new ToolHelpSpec("jar",         1,   1,   1,   0,         0,    0,     1),     
        new ToolHelpSpec("jarsigner",   1,   1,   1,   0,         1,    0,     1),     
        new ToolHelpSpec("java",        1,   1,   1,   0,         1,    1,     1),     
        new ToolHelpSpec("javac",       1,   0,   1,   0,         1,    1,     2),     
        new ToolHelpSpec("javadoc",     1,   1,   1,   0,         1,    1,     1),     
        new ToolHelpSpec("javap",       1,   1,   1,   0,         1,    1,     2),     
        new ToolHelpSpec("javaw",       1,   1,   1,   0,         1,    1,     1),     
        new ToolHelpSpec("jcmd",        1,   1,   1,   0,         1,    0,     1),     
        new ToolHelpSpec("jdb",         1,   1,   1,   0,         1,    1,     0),     
        new ToolHelpSpec("jdeprscan",   1,   1,   1,   0,         0,    0,     1),     
        new ToolHelpSpec("jdeps",       1,   1,   1,   0,         1,    0,     2),     
        new ToolHelpSpec("jfr",         1,   1,   1,   0,         0,    0,     2),     
        new ToolHelpSpec("jhsdb",       0,   0,   0,   0,         0,    0,     0),     
        new ToolHelpSpec("jimage",      1,   1,   1,   0,         0,    0,     2),     
        new ToolHelpSpec("jinfo",       1,   1,   1,   0,         1,    1,     1),     
        new ToolHelpSpec("jjs",         0,   1,   1, 100,         0,    0,   100),     
        new ToolHelpSpec("jlink",       1,   1,   1,   0,         0,    0,     2),     
        new ToolHelpSpec("jmap",        1,   1,   1,   0,         1,    0,     1),     
        new ToolHelpSpec("jmod",        1,   1,   1,   0,         1,    0,     2),     
        new ToolHelpSpec("jps",         1,   1,   1,   0,         1,    1,     1),     
        new ToolHelpSpec("jrunscript",  1,   1,   1,   0,         1,    1,     7),     
        new ToolHelpSpec("jshell",      1,   1,   1,   0,         1,    0,     1),     
        new ToolHelpSpec("jstack",      1,   1,   1,   0,         1,    1,     1),     
        new ToolHelpSpec("jstat",       1,   1,   1,   0,         1,    1,     1),     
        new ToolHelpSpec("jstatd",      1,   1,   1,   0,         0,    0,     1),     
        new ToolHelpSpec("keytool",     1,   1,   1,   0,         1,    0,     1),     
        new ToolHelpSpec("rmiregistry", 0,   0,   0,   0,         0,    0,     1),     
        new ToolHelpSpec("serialver",   0,   0,   0,   0,         0,    0,     1),     
        new ToolHelpSpec("jpackage",    0,   1,   1,   0,         0,    1,     1),     
        new ToolHelpSpec("jwebserver",  1,   1,   1,   0,         0,    1,     1),     
    };

    static ToolHelpSpec getToolHelpSpec(String tool) {
        for (ToolHelpSpec x : jdkTools) {
            if (tool.toLowerCase().equals(x.toolname) ||
                tool.toLowerCase().equals(x.toolname + ".exe"))
                return x;
        }
        return null;
    }

    static boolean findFlagInLine(String line, String flag) {
        if (line.contains(flag) &&
            !line.contains("nknown") &&                       
            !line.contains("invalid flag") &&                 
            !line.contains("invalid option") &&               
            !line.contains("FileNotFoundException: -help") && 
            !line.contains("-h requires an argument") &&      
            !line.contains("port argument,")) {               
            int flagLen = flag.length();
            int lineLen = line.length();
            for (int i = line.indexOf(flag); i >= 0; i = line.indexOf(flag, i+1)) {
                if (i > 0 &&
                    line.charAt(i-1) != ' ' &&
                    line.charAt(i-1) != '[' &&  
                    line.charAt(i-1) != '|' &&  
                    line.charAt(i-1) != '\t') { 
                    continue;
                }
                int posAfter = i + flagLen;
                if (posAfter < lineLen &&
                    line.charAt(posAfter) != ' ' &&
                    line.charAt(posAfter) != ',' &&
                    line.charAt(posAfter) != '[' && 
                    line.charAt(posAfter) != ']' && 
                    line.charAt(posAfter) != ')' && 
                    line.charAt(posAfter) != '|' && 
                    line.charAt(posAfter) != ':' && 
                    line.charAt(posAfter) != '"') { 
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    static TestResult runToolWithFlag(File f, String flag) {
        String x = f.getAbsolutePath();
        TestResult tr = doExec(x, flag);
        System.out.println("Testing " + f.getName());
        System.out.println("#> " + x + " " + flag);
        tr.testOutput.forEach(System.out::println);
        System.out.println("#> echo $?");
        System.out.println(tr.exitValue);

        return tr;
    }

    static String testTool(File f, String flag, int exitcode) {
        String result = "";
        TestResult tr = runToolWithFlag(f, flag);

        if (exitcode == 0 && !tr.isOK()) {
            System.out.println("failed");
            result = "failed: " + f.getName() + " " + flag + " has exit code " + tr.exitValue + ".\n";
        }

        boolean foundFlag = false;
        for (String y : tr.testOutput) {
            if (!foundFlag && findFlagInLine(y, flag)) { 
                foundFlag = true;
                System.out.println("Found documentation of '" + flag + "': '" + y.trim() +"'");
            }
        }
        if (!foundFlag) {
            result += "failed: " + f.getName() + " does not document " +
                flag + " in help message.\n";
        }

        if (!result.isEmpty())
            System.out.println(result);

        return result;
    }

    static String testLegacyFlag(File f, int exitcode) {
        String result = "";
        TestResult tr = runToolWithFlag(f, "-help");

        if (exitcode == 0 && !tr.isOK()) {
            System.out.println("failed");
            result = "failed: " + f.getName() + " -help has exit code " + tr.exitValue + ".\n";
        }

        boolean foundFlag = false;
        for (String y : tr.testOutput) {
            if (!foundFlag && findFlagInLine(y, "-help")) {  
                foundFlag = true;
                System.out.println("Found documentation of '-help': '" + y.trim() +"'");
            }
        }
        if (foundFlag) {
            result += "failed: " + f.getName() + " does document -help " +
                "in help message. This legacy flag should not be documented.\n";
        }

        if (!result.isEmpty())
            System.out.println(result);

        return result;
    }

    static String testInvalidFlag(File f, String flag, int exitcode, boolean documentsLegacyHelp) {
        String result = "";
        TestResult tr = runToolWithFlag(f, flag);

        if (!((exitcode == tr.exitValue) ||
              (tr.exitValue < 0 && exitcode == tr.exitValue + 256))) {
            System.out.println("failed");
            result = "failed: " + f.getName() + " " + flag + " should not be " +
                     "accepted. But it has exit code " + tr.exitValue + ".\n";
        }

        if (!documentsLegacyHelp) {
            boolean foundFlag = false;
            for (String y : tr.testOutput) {
                if (!foundFlag && findFlagInLine(y, "-help")) {  
                    foundFlag = true;
                    System.out.println("Found documentation of '-help': '" + y.trim() +"'");
                }
            }
            if (foundFlag) {
                result += "failed: " + f.getName() + " does document -help " +
                    "in error message. This legacy flag should not be documented.\n";
            }
        }

        if (!result.isEmpty())
            System.out.println(result);

        return result;
    }

    public static void main(String[] args) {
        String errorMessage = "";

        if (!isEnglishLocale()) { return; }

        for (File f : new File(JAVA_BIN).listFiles(new ToolFilter(TOOLS_NOT_TO_TEST))) {
            String toolName = f.getName();

            ToolHelpSpec tool = getToolHelpSpec(toolName);
            if (tool == null) {
                errorMessage += "Tool " + toolName + " not covered by this test. " +
                    "Add specification to jdkTools array!\n";
                continue;
            }

            if (tool.supportsQuestionMark == true) {
                errorMessage += testTool(f, "-?", tool.exitcodeOfHelp);
            } else {
                System.out.println("Skip " + tool.toolname + ". It does not support -?.");
            }
            if (tool.supportsH == true) {
                errorMessage += testTool(f, "-h", tool.exitcodeOfHelp);
            } else {
                System.out.println("Skip " + tool.toolname + ". It does not support -h.");
            }
            if (tool.supportsHelp == true) {
                errorMessage += testTool(f, "--help", tool.exitcodeOfHelp);
            } else {
                System.out.println("Skip " + tool.toolname + ". It does not support --help.");
            }

            errorMessage += testInvalidFlag(f, "-asdfxgr", tool.exitcodeOfWrongFlag, tool.documentsLegacyHelp);

            if (!tool.documentsLegacyHelp) {
                if (tool.supportsLegacyHelp == true) {
                    errorMessage += testLegacyFlag(f, tool.exitcodeOfHelp);
                } else {
                    errorMessage += testInvalidFlag(f, "-help", tool.exitcodeOfWrongFlag, false);
                }
            }
        }

        if (errorMessage.isEmpty()) {
            System.out.println("All help string tests: PASS");
        } else {
            throw new AssertionError("HelpFlagsTest failed:\n" + errorMessage);
        }
    }
}
