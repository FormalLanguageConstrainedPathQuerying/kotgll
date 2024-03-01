/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Tests how CDS works when critical library classes are replaced with JVMTI ClassFileLoadHook
 * @library /test/lib
 * @requires vm.cds
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar whitebox.jar jdk.test.whitebox.WhiteBox
 * @run main/othervm/native ReplaceCriticalClasses
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;
import jdk.test.whitebox.WhiteBox;

public class ReplaceCriticalClasses {
    public static void main(String args[]) throws Throwable {
        ReplaceCriticalClasses rcc = new ReplaceCriticalClasses();
        rcc.process(args);
    }

    public void process(String args[]) throws Throwable {
        if (args.length == 0) {
            String extraClasses[] = {"sun/util/resources/cldr/provider/CLDRLocaleDataMetaInfo"};

            CDSOptions opts = new CDSOptions()
                .setClassList(extraClasses)
                .setArchiveName(ReplaceCriticalClasses.class.getName() + ".jsa");
            CDSTestUtils.createArchiveAndCheck(opts);

            launchChildProcesses(getTests());
        } else if (args.length == 3 && args[0].equals("child")) {
            Class klass = Class.forName(args[2].replace("/", "."));
            if (args[1].equals("-shared")) {
                testInChild(true, klass);
            } else if (args[1].equals("-notshared")) {
                testInChild(false, klass);
            } else {
                throw new RuntimeException("Unknown child exec option " + args[1]);
            }
            return;
        } else {
            throw new RuntimeException("Usage: @run main/othervm/native ReplaceCriticalClasses");
        }
    }

    public String[] getTests() {
        String tests[] = {
            "-early -notshared java/lang/Object",
            "-early -notshared java/lang/String",
            "-early -notshared java/lang/Cloneable",
            "-early -notshared java/io/Serializable",
            "-early -notshared java/lang/Module",
            "-early -notshared java/lang/ModuleLayer",

            "java/lang/Object",
            "java/lang/String",
            "java/lang/Cloneable",
            "java/io/Serializable",
            "java/lang/Module",
            "java/lang/ModuleLayer",

            "-notshared java/util/Locale",
            "-notshared sun/util/locale/BaseLocale",
        };
        return tests;
    }

    static void launchChildProcesses(String tests[]) throws Throwable {
        int n = 0;
        for (String s : tests) {
            System.out.println("Test case[" + (n++) + "] = \"" + s + "\"");
            String args[] = s.split("\\s+"); 
            launchChild(args);
        }
    }

    static void launchChild(String args[]) throws Throwable {
        if (args.length < 1) {
            throw new RuntimeException("Invalid test case. Should be <-early> <-subgraph> <-notshared> <-nowhitebox> klassName subgraphKlass");
        }
        String klassName = null;
        String subgraphKlass = null;
        String early = "";
        boolean subgraph = false;
        boolean whitebox = true;
        String shared = "-shared";

        for (int i=0; i<args.length-1; i++) {
            String opt = args[i];
            if (opt.equals("-early")) {
                early = "-early,";
            } else if (opt.equals("-subgraph")) {
                subgraph = true;
            } else if (opt.equals("-nowhitebox")) {
                whitebox = false;
            } else if (opt.equals("-notshared")) {
                shared = opt;
            } else {
              if (!subgraph) {
                throw new RuntimeException("Unknown option: " + opt);
              }
            }
        }
        if (subgraph) {
          klassName = args[args.length-2];
          subgraphKlass = args[args.length-1];
        } else {
          klassName = args[args.length-1];
        }
        Class.forName(klassName.replace("/", ".")); 
        final String subgraphInit = "initialize_from_archived_subgraph " + subgraphKlass;

        String agent = "-agentlib:SimpleClassFileLoadHook=" + early + klassName + ",XXX,XXX";

        CDSOptions opts = (new CDSOptions())
            .setXShareMode("auto")
            .setArchiveName(ReplaceCriticalClasses.class.getName() + ".jsa")
            .setUseVersion(false)
            .addSuffix("-showversion",
                       "-Xlog:cds",
                       "-XX:+UnlockDiagnosticVMOptions",
                       agent);
        if (whitebox) {
            opts.addSuffix("-XX:+WhiteBoxAPI",
                           "-Xbootclasspath/a:" + ClassFileInstaller.getJarPath("whitebox.jar"));
        }
        opts.addSuffix("-Xlog:cds,cds+heap");
        opts.addSuffix("ReplaceCriticalClasses",
                       "child",
                       shared,
                       klassName);

        final boolean expectDisable = !early.equals("");
        final boolean checkSubgraph = subgraph;
        final boolean expectShared = shared.equals("-shared");
        CDSTestUtils.run(opts).assertNormalExit(out -> {
                if (expectDisable) {
                    out.shouldContain("CDS is disabled because early JVMTI ClassFileLoadHook is in use.");
                    System.out.println("CDS disabled as expected");
                }
                if (checkSubgraph) {
                    if (expectShared) {
                        if (!out.getOutput().contains("Unable to map at required address in java heap")) {
                            out.shouldContain(subgraphInit);
                            out.shouldNotContain("Rewriting done.");
                        }
                    } else {
                      out.shouldNotContain(subgraphInit);
                    }
                }
            });
    }

    static void testInChild(boolean shouldBeShared, Class klass) {
        try {
            WhiteBox wb = WhiteBox.getWhiteBox();

            if (shouldBeShared && !wb.isSharedClass(klass)) {
                throw new RuntimeException(klass + " should be shared but but actually is not.");
            }
            if (!shouldBeShared && wb.isSharedClass(klass)) {
                throw new RuntimeException(klass + " should not be shared but actually is.");
            }
            System.out.println("wb.isSharedClass(" + klass + "): " + wb.isSharedClass(klass) + " == " + shouldBeShared);
        } catch (UnsatisfiedLinkError e) {
            System.out.println("WhiteBox is disabled -- because test has -nowhitebox");
        }

        String strings[] = {
            "@",
            "nanosecond timeout value out of range",
            "timeoutMillis value is negative",

            "0",
            "0X",
            "0x",
            "int"
        };

        for (String s : strings) {
            String i = s.intern();
            if (s != i) {
                throw new RuntimeException("Interned string mismatch: \"" + s + "\" @ " + System.identityHashCode(s) +
                                           " vs \"" + i + "\" @ " + System.identityHashCode(i));
            }
        }
        System.out.println("If I can come to here without crashing, things should be OK");
    }
}
