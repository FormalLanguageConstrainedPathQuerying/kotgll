/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @run driver ReattemptErrorTest
 */

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class ReattemptErrorTest {

    public static final int ERROR_LOG_TIMEOUT = 16;

    public static void main(String[] args) throws Exception {


        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder(
            "-XX:+UnlockDiagnosticVMOptions",
            "-Xmx100M",
            "-XX:-CreateCoredumpOnCrash",
            "-XX:ErrorHandlerTest=15",
            "-XX:TestCrashInErrorHandler=15",
            "-XX:ErrorLogTimeout=" + ERROR_LOG_TIMEOUT,
            "-version");

        OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());

        output_detail.shouldMatch("# A fatal error has been detected by the Java Runtime Environment:.*");
        output_detail.shouldMatch("#.+SIGFPE.*");

        File hs_err_file = HsErrFileUtils.openHsErrFileFromOutput(output_detail);

        ArrayList<Pattern> positivePatternlist = new ArrayList<>();
        ArrayList<Pattern> negativePatternlist = new ArrayList<>();

        positivePatternlist.add(Pattern.compile("Will crash now \\(TestCrashInErrorHandler=15\\)..."));
        positivePatternlist.add(Pattern.compile("\\[error occurred during error reporting \\(test reattempt secondary crash\\).*\\]"));
        positivePatternlist.add(Pattern.compile("test reattempt secondary crash. attempt 2"));
        negativePatternlist.add(Pattern.compile("test reattempt secondary crash. attempt 3"));

        positivePatternlist.add(Pattern.compile("test reattempt timeout"));
        positivePatternlist.add(Pattern.compile(".*timeout occurred during error reporting in step \"test reattempt timeout\".*"));
        negativePatternlist.add(Pattern.compile("test reattempt secondary crash, attempt 2"));
        positivePatternlist.add(Pattern.compile(".*stop reattempt \\(test reattempt timeout, attempt 2\\) reason: Step time limit reached.*"));

        positivePatternlist.add(Pattern.compile("test reattempt stack headroom"));
        positivePatternlist.add(Pattern.compile("\\[error occurred during error reporting \\(test reattempt stack headroom\\).*\\]"));
        negativePatternlist.add(Pattern.compile("test reattempt stack headroom, attempt 2"));
        positivePatternlist.add(Pattern.compile(".*stop reattempt \\(test reattempt stack headroom, attempt 2\\) reason: Stack headroom limit reached.*"));

        Pattern[] positivePatterns = positivePatternlist.toArray(new Pattern[] {});
        Pattern[] negativePatterns = negativePatternlist.toArray(new Pattern[] {});

        HsErrFileUtils.checkHsErrFileContent(hs_err_file, positivePatterns, negativePatterns, true, true);

        System.out.println("OK.");
    }
}
