/*
 * Copyright (c) 2014, 2024 SAP SE. All rights reserved.
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check secondary error handling
 * @library /test/lib
 * @requires vm.flagless
 * @requires vm.debug
 * @requires os.family != "windows"
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver SecondaryErrorTest no_callstacks
 */

/*
 * @test
 * @summary Check secondary error handling
 * @library /test/lib
 * @requires vm.flagless
 * @requires vm.debug
 * @requires os.family != "windows"
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver SecondaryErrorTest with_callstacks
 */

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class SecondaryErrorTest {


  public static void main(String[] args) throws Exception {

    boolean with_callstacks = false;
    if (args.length != 1) {
      throw new IllegalArgumentException("Missing argument");
    } else if (args[0].equals("with_callstacks")) {
      with_callstacks = true;
    } else if (args[0].equals("no_callstacks")) {
      with_callstacks = false;
    } else {
      throw new IllegalArgumentException("unknown argument (" + args[0] + ")");
    }


    ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder(
        "-XX:+UnlockDiagnosticVMOptions",
        "-Xmx100M",
        "-XX:-CreateCoredumpOnCrash",
        "-XX:ErrorHandlerTest=15",
        "-XX:TestCrashInErrorHandler=14",
        "-XX:" + (with_callstacks ? "+" : "-") + "ErrorLogSecondaryErrorDetails",
        "-version");

    OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());

    output_detail.shouldMatch("# A fatal error has been detected by the Java Runtime Environment:.*");
    output_detail.shouldMatch("#.+SIGFPE.*");

    File hs_err_file = HsErrFileUtils.openHsErrFileFromOutput(output_detail);


    ArrayList<Pattern> patternlist = new ArrayList<>();
    patternlist.add(Pattern.compile("Will crash now \\(TestCrashInErrorHandler=14\\)..."));
    patternlist.add(Pattern.compile("\\[error occurred during error reporting \\(test secondary crash 1\\).*\\]"));
    if (with_callstacks) {
        patternlist.add(Pattern.compile("\\[siginfo:.*\\(SIGSEGV\\).*\\]"));
        patternlist.add(Pattern.compile("\\[stack: Native frames:.*"));
        patternlist.add(Pattern.compile(".*VMError::controlled_crash.*"));
    }
    patternlist.add(Pattern.compile("Will crash now \\(TestCrashInErrorHandler=14\\)..."));
    patternlist.add(Pattern.compile("\\[error occurred during error reporting \\(test secondary crash 2\\).*\\]"));
    if (with_callstacks) {
        patternlist.add(Pattern.compile("\\[siginfo:.*\\(SIGSEGV\\).*\\]"));
        patternlist.add(Pattern.compile("\\[stack: Native frames:.*"));
        patternlist.add(Pattern.compile(".*VMError::controlled_crash.*"));
    }
    Pattern[] pattern = patternlist.toArray(new Pattern[] {});

    HsErrFileUtils.checkHsErrFileContent(hs_err_file, pattern, false, true);

    System.out.println("OK.");

  }

}


