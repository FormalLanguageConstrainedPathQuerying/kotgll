/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Exercise the java.lang.instrument.Instrumentation APIs on classes archived
 *          using CDS/AppCDSv1/AppCDSv2.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @requires vm.cds
 * @requires vm.jvmti
 * @build jdk.test.whitebox.WhiteBox
 *        InstrumentationApp
 *        InstrumentationClassFileTransformer
 *        InstrumentationRegisterClassFileTransformer
 * @run main/othervm InstrumentationTest
 */


import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;
import jdk.test.lib.Asserts;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class InstrumentationTest {
    public static String bootClasses[] = {
        "InstrumentationApp$Intf",
        "InstrumentationApp$Bar",
        "jdk.test.whitebox.WhiteBox",
    };
    public static String appClasses[] = {
        "InstrumentationApp",
        "InstrumentationApp$Foo",
        "InstrumentationApp$MyLoader",
    };
    public static String custClasses[] = {
        "InstrumentationApp$Coo",
    };
    public static String sharedClasses[] = TestCommon.concat(bootClasses, appClasses);

    public static String agentClasses[] = {
        "InstrumentationClassFileTransformer",
        "InstrumentationRegisterClassFileTransformer",
        "Util",
    };

    public static void main(String[] args) throws Throwable {
        runTest(false);
        runTest(true);
    }

    public static void runTest(boolean attachAgent) throws Throwable {
        String bootJar =
            ClassFileInstaller.writeJar("InstrumentationBoot.jar", bootClasses);
        String appJar =
            ClassFileInstaller.writeJar("InstrumentationApp.jar",
                                        TestCommon.concat(appClasses,
                                                          "InstrumentationApp$ArchivedIfAppCDSv2Enabled"));
        String custJar =
            ClassFileInstaller.writeJar("InstrumentationCust.jar", custClasses);
        String agentJar =
            ClassFileInstaller.writeJar("InstrumentationAgent.jar",
                                        ClassFileInstaller.Manifest.fromSourceFile("InstrumentationAgent.mf"),
                                        agentClasses);

        String bootCP = "-Xbootclasspath/a:" + bootJar;

        System.out.println("");
        System.out.println("============================================================");
        System.out.println("CDS: NO, attachAgent: " + (attachAgent ? "YES" : "NO"));
        System.out.println("============================================================");
        System.out.println("");

        String agentCmdArg, flagFile;
        if (attachAgent) {
            agentCmdArg = "-showversion";
        } else {
            agentCmdArg = "-javaagent:" + agentJar;
        }

        flagFile = getFlagFile(attachAgent);
        AgentAttachThread t = doAttach(attachAgent, flagFile, agentJar);
        CDSOptions opts = (new CDSOptions())
            .setUseVersion(false)
            .setXShareMode("off")
            .addSuffix(bootCP,
                       "-cp", appJar,
                       "-XX:+UnlockDiagnosticVMOptions",
                       "-XX:+WhiteBoxAPI",
                       "-Xshare:off",
                       agentCmdArg,
                       "InstrumentationApp", flagFile, bootJar, appJar, custJar);
        CDSTestUtils.run(opts)
                    .assertNormalExit();
        checkAttach(t);

        String[] v2Classes = {
            "InstrumentationApp$ArchivedIfAppCDSv2Enabled",
            "java/lang/Object id: 0",
            "InstrumentationApp$Intf id: 1",
            "InstrumentationApp$Coo  id: 2 super: 0 interfaces: 1 source: " + custJar,
        };
        String[] sharedClassesWithV2 = TestCommon.concat(v2Classes, sharedClasses);
        OutputAnalyzer out = TestCommon.dump(appJar, sharedClassesWithV2, bootCP);
        if (out.getExitValue() != 0) {
            System.out.println("Redumping with AppCDSv2 disabled");
                TestCommon.testDump(appJar, sharedClasses, bootCP);
        }

        System.out.println("");
        System.out.println("============================================================");
        System.out.println("CDS: YES, attachAgent: " + (attachAgent ? "YES" : "NO"));
        System.out.println("============================================================");
        System.out.println("");

        flagFile = getFlagFile(attachAgent);
        t = doAttach(attachAgent, flagFile, agentJar);
        out = TestCommon.execAuto("-cp", appJar,
                bootCP,
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                agentCmdArg,
               "InstrumentationApp", flagFile, bootJar, appJar, custJar);

        opts = (new CDSOptions()).setXShareMode("auto");
        TestCommon.checkExec(out, opts);
        checkAttach(t);
    }

    static int flagFileSerial = 1;
    static private String getFlagFile(boolean attachAgent) {
        if (attachAgent) {
            return "attach.flag." + ProcessHandle.current().pid() +
                    "." + (flagFileSerial++) + "." + System.currentTimeMillis();
        } else {
            return "noattach";
        }
    }

    static AgentAttachThread doAttach(boolean attachAgent, String flagFile, String agentJar) throws Throwable {
        if (!attachAgent) {
            return null;
        }


        File f = new File(flagFile);
        f.delete();
        if (f.exists()) {
            throw new RuntimeException("Flag file should not exist: " + f);
        }

        AgentAttachThread t = new AgentAttachThread(flagFile, agentJar);
        t.start();
        return t;
    }

    static void checkAttach(AgentAttachThread thread) throws Throwable {
        if (thread != null) {
            thread.check();
        }
    }

    static class AgentAttachThread extends Thread {
        String flagFile;
        String agentJar;
        volatile boolean succeeded;

        AgentAttachThread(String flagFile, String agentJar) {
            this.flagFile = flagFile;
            this.agentJar = agentJar;
            this.succeeded = false;
        }

        static String getPid(String flagFile) throws Throwable {
            while (true) {
                Thread.sleep(100);
                File f = new File(flagFile);
                if (f.exists() && f.length() > 100) {
                    try (FileInputStream in = new FileInputStream(f)) {
                        Scanner scanner = new Scanner(in);
                        return Long.toString(scanner.nextLong());
                    } catch (Throwable t) {
                        System.out.println("Ignored: " + t);
                        t.printStackTrace(System.out);
                        continue;
                    }
                }
            }
        }

        public void run() {
            try {
                String pid = getPid(flagFile);
                System.out.println("child pid = " + pid);
                VirtualMachine vm = VirtualMachine.attach(pid);
                System.out.println(agentJar);
                vm.loadAgent(agentJar);
            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            }

            File f = new File(flagFile);
            for (int i=0; i<5; i++) {
                f.delete();
                try {
                    Thread.sleep(10);
                } catch (Throwable t) {;}
            }
            if (f.exists()) {
                throw new RuntimeException("Failed to delete " + f);
            }
            System.out.println("Attach succeeded (parent)");
            succeeded = true;
        }

        void check() throws Throwable {
            super.join();
            if (!succeeded) {
                throw new RuntimeException("Attaching agent to child VM failed");
            }
        }
    }
}

