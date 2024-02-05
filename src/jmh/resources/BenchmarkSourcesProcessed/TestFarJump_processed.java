/*
 * Copyright (c) 2024, BELLSOFT. All rights reserved.
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
package compiler.c2.aarch64;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

/*
 * @test
 * @bug 8280872
 * @summary Far call to runtime stub should be generated with single instruction for CodeHeap up to 250MB
 * @library /test/lib /
 *
 * @requires vm.flagless
 * @requires os.arch=="aarch64"
 * @requires vm.debug == false
 * @requires vm.compiler2.enabled
 *
 * @run driver compiler.c2.aarch64.TestFarJump
 */
public class TestFarJump {

    static boolean isADRP(int encoding) {
        final int mask = 0b1001_1111;
        final int val  = 0b1001_0000;
        return ((encoding >> 24) & mask) == val;
    }

    static boolean containsADRP(String input) {
        int index = input.indexOf(": ");
        if (index == -1) {
            return false;
        }
        input = input.substring(index + 1);
        if (input.contains("adrp")) {
            return true;
        }
        Pattern pattern = Pattern.compile("[0-9a-f ]*");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String match = matcher.group();
            match = match.replace(" " , "");
            if (match.length() != 8) {
                continue;
            }
            int dump = (int)Long.parseLong(match, 16);
            int encoding = Integer.reverseBytes(dump);
            return isADRP(encoding);
        }
        return false;
    }

    static void runVM(boolean bigCodeHeap) throws Exception {
        String className = TestFarJump.class.getName();
        String[] procArgs = {
            "-XX:-Inline",
            "-Xcomp",
            "-Xbatch",
            "-XX:+TieredCompilation",
            "-XX:+SegmentedCodeCache",
            "-XX:ReservedCodeCacheSize=" + (bigCodeHeap ? "256M" : "200M"),
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:CompileCommand=option," + className + "::main,bool,PrintAssembly,true",
            className};

        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder(procArgs);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        List<String> lines = output.asLines();

        ListIterator<String> itr = lines.listIterator();
        while (itr.hasNext()) {
            String line = itr.next();
            if (line.contains("[Exception Handler]")) {
                String next1 = itr.next();
                String next2 = itr.next();
                System.out.println(line);
                System.out.println(next1);
                System.out.println(next2);
                boolean containsADRP = containsADRP(next1) || containsADRP(next2);
                if (bigCodeHeap && !containsADRP) {
                    throw new RuntimeException("ADRP instruction is expected on far jump");
                }
                if (!bigCodeHeap && containsADRP) {
                    throw new RuntimeException("for CodeHeap < 250MB the far jump is expected to be encoded with a single branch instruction");
                }
                return;
            }
        }
        throw new RuntimeException("Assembly output: exception Handler is not found");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            runVM(true);
            runVM(false);
            return;
        }
        if (args.length > 0) {
            System.out.println("Ok");
        }
    }
}

