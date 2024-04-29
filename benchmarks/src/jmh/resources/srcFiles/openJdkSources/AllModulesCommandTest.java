/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.Set;
import java.util.HashSet;
import static jdk.test.lib.Asserts.assertTrue;

/**
 * @test
 * @summary Tests the modules-related JDWP commands
 * @library /test/lib
 * @modules jdk.jdwp.agent
 * @modules java.base/jdk.internal.misc
 * @requires vm.jvmti
 * @compile AllModulesCommandTestDebuggee.java
 * @run main/othervm AllModulesCommandTest
 */
public class AllModulesCommandTest implements DebuggeeLauncher.Listener {

    private DebuggeeLauncher launcher;
    private JdwpChannel channel;
    private CountDownLatch jdwpLatch = new CountDownLatch(1);
    private Set<String> jdwpModuleNames = new HashSet<>();
    private Set<String> javaModuleNames = new HashSet<>();

    public static void main(String[] args) throws Throwable {
        new AllModulesCommandTest().doTest();
    }

    private void doTest() throws Throwable {
        launcher = new DebuggeeLauncher(this);
        launcher.launchDebuggee();
        jdwpLatch.await();
        doJdwp();
    }

    @Override
    public void onDebuggeeModuleInfo(String modName) {
        javaModuleNames.add(modName);
    }

    @Override
    public void onDebuggeeSendingCompleted() {
        jdwpLatch.countDown();
    }

    private void doJdwp() throws Exception {
        try {
            channel = new JdwpChannel();
            channel.connect(launcher.getJdwpPort());
            JdwpAllModulesReply reply = new JdwpAllModulesCmd().send(channel);
            assertReply(reply);
            for (int i = 0; i < reply.getModulesCount(); ++i) {
                long modId = reply.getModuleId(i);
                String modName = getModuleName(modId);
                System.out.println("i=" + i + ", modId=" + modId + ", modName=" + modName);
                if (modName != null) { 
                    jdwpModuleNames.add(modName);
                }
                assertClassLoader(modId, modName);
            }

            System.out.println("Module names reported by JDWP: " + Arrays.toString(jdwpModuleNames.toArray()));
            System.out.println("Module names reported by Java: " + Arrays.toString(javaModuleNames.toArray()));

            if (!jdwpModuleNames.equals(javaModuleNames)) {
                throw new RuntimeException("Modules info reported by Java API differs from that reported by JDWP.");
            } else {
                System.out.println("Test passed!");
            }

        } finally {
            launcher.terminateDebuggee();
            try {
                new JdwpExitCmd(0).send(channel);
                channel.disconnect();
            } catch (Exception x) {
            }
        }
    }

    private String getModuleName(long modId) throws IOException {
        JdwpModNameReply reply = new JdwpModNameCmd(modId).send(channel);
        assertReply(reply);
        return reply.getModuleName();
    }

    private void assertReply(JdwpReply reply) {
        if (reply.getErrorCode() != 0) {
            throw new RuntimeException("Unexpected reply error code " + reply.getErrorCode() + " for reply " + reply);
        }
    }

    private void assertClassLoader(long modId, String modName) throws IOException {
        JdwpClassLoaderReply reply = new JdwpClassLoaderCmd(modId).send(channel);
        assertReply(reply);
        long moduleClassLoader = reply.getClassLoaderId();
        assertTrue(moduleClassLoader >= 0, "bad classloader refId " + moduleClassLoader + " for module '" + modName + "', moduleId=" + modId);

        String clsModName = getModuleName(modId);
        if ("java.base".equals(clsModName)) {
            assertGetModule(moduleClassLoader, modId);
        }
    }

    private void assertGetModule(long moduleClassLoader, long modId) throws IOException {
        JdwpVisibleClassesReply visibleClasses = new JdwpVisibleClassesCmd(moduleClassLoader).send(channel);
        assertReply(visibleClasses);

        boolean moduleFound = false;
        for (long clsId : visibleClasses.getVisibleClasses()) {
            JdwpModuleReply modReply = new JdwpModuleCmd(clsId).send(channel);
            assertReply(modReply);
            long clsModId = modReply.getModuleId();

            if (modId == clsModId) {
                moduleFound = true;
                break;
            }
        }
        assertTrue(moduleFound, "None of the visible classes for the classloader of the module " + getModuleName(modId) + " reports the module as its own");
    }

}
