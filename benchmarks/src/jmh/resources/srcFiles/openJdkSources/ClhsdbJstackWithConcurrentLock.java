/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8324066
 * @summary Test the clhsdb 'jstack -l' command for printing concurrent lock information
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbJstackWithConcurrentLock
 */

import java.util.List;
import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;

public class ClhsdbJstackWithConcurrentLock {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting the ClhsdbJstackWithConcurrentLock test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();

            theApp = new LingeredAppWithConcurrentLock();
            LingeredApp.startApp(theApp, "-Xmx4m");
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            List<String> cmds = List.of("jstack -l");
            String jstackOutput = test.run(theApp.getPid(), cmds, null, null);

            String key = ", (a java/util/concurrent/locks/ReentrantLock$NonfairSync)";
            String[] lines = jstackOutput.split("\\R");
            String addressString = null;
            for (String line : lines) {
                if (line.contains(key)) {
                    String[] words = line.split("[, ]");
                    for (String word : words) {
                        word = word.replace("<", "").replace(">", "");
                        if (word.startsWith("0x")) {
                            addressString = word;
                            break;
                        }
                    }
                    if (addressString != null)
                        break;
                }
            }
            if (addressString == null) {
                throw new RuntimeException("Token '" + key + "' not found in jstack output");
            }

            key = "- parking to wait for <" + addressString +
                "> (a java/util/concurrent/locks/ReentrantLock$NonfairSync)";
            boolean found = false;
            for (String line : lines) {
                if (line.contains(key)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Token '" + key + "' not found in jstack output");
            }
        } catch (SkippedException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
            System.out.println("OUTPUT: " + theApp.getOutput());
        }
        System.out.println("Test PASSED");
    }
}
