/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Smoke test for JDWP hardening
 * @library /test/lib
 * @run driver JdwpAllowTest
 */

import java.io.IOException;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import jdk.test.lib.JDWP;
import jdk.test.lib.Utils;
import jdk.test.lib.apps.LingeredApp;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class JdwpAllowTest {

    public static int handshake(int port) throws IOException {
        int res = -1;
        Socket s = null;
        try {
            s = new Socket(localAddr, port);
            s.getOutputStream().write("JDWP-Handshake".getBytes("UTF-8"));
            byte[] buffer = new byte[24];
            res = s.getInputStream().read(buffer);
        }
        catch (SocketException ex) {
            ex.printStackTrace(System.out);
        } finally {
            if (s != null) {
                s.close();
            }
        }
        return res;
    }

    public static String[] prepareCmd(String allowOpt) {
         String jdwpArgs = "-agentlib:jdwp=transport=dt_socket,server=y," +
                           "suspend=n,address=*:0"
                            + (allowOpt == null ? "" : ",allow=" + allowOpt);
         return new String[] { jdwpArgs };
    }

    private static int detectPort(LingeredApp app) {
        long maxWaitTime = System.currentTimeMillis()
                + Utils.adjustTimeout(10000);  
        while (true) {
            String s = app.getProcessStdout();
            JDWP.ListenAddress addr = JDWP.parseListenAddress(s);
            if (addr != null) {
                return Integer.parseInt(addr.address());
            }
            if (System.currentTimeMillis() > maxWaitTime) {
                throw new RuntimeException("Could not detect port from '" + s + "' (timeout)");
            }
            try {
                if (app.getProcess().waitFor(500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Could not detect port from '" + s + "' (debuggee is terminated)");
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public static void positiveTest(String testName, String allowOpt)
        throws InterruptedException, IOException {
        System.out.println("\nStarting " + testName);
        String[] cmd = prepareCmd(allowOpt);

        LingeredApp a = LingeredApp.startApp(cmd);
        int res;
        try {
            res = handshake(detectPort(a));
        } finally {
            a.stopApp();
        }
        if (res < 0) {
            throw new RuntimeException(testName + " FAILED");
        }
        System.out.println(testName + " PASSED");
    }

    public static void negativeTest(String testName, String allowOpt)
        throws InterruptedException, IOException {
        System.out.println("\nStarting " + testName);
        String[] cmd = prepareCmd(allowOpt);

        LingeredApp a = LingeredApp.startApp(cmd);
        int res;
        try {
            res = handshake(detectPort(a));
        } finally {
            a.stopApp();
        }
        if (res > 0) {
            System.out.println(testName + ": res=" + res);
            throw new RuntimeException(testName + " FAILED");
        }
        System.out.println(testName + ": returned a negative code as expected: " + res);
        System.out.println(testName + " PASSED");
    }

    public static void badAllowOptionTest(String testName, String allowOpt)
        throws InterruptedException, IOException {
        System.out.println("\nStarting " + testName);
        String[] cmd = prepareCmd(allowOpt);

        LingeredApp a;
        try {
            a = LingeredApp.startApp(cmd);
        } catch (IOException ex) {
            System.out.println(testName + ": caught expected IOException");
            System.out.println(testName + " PASSED");
            return;
        }
        a.stopApp();
        throw new RuntimeException(testName + " FAILED");
    }

    /*
     * Generate allow address by changing random bit in the local address
     * and calculate 2 masks (prefix length) - one matches original local address
     * and another doesn't.
     */
    private static class MaskTest {
        public final String localAddress;
        public final String allowAddress;
        public final int prefixLengthGood;
        public final int prefixLengthBad;

        public MaskTest(InetAddress addr) throws Exception {
            localAddress = addr.getHostAddress();
            byte[] bytes = addr.getAddress();
            Random r = new Random();
            int bitToChange = r.nextInt(bytes.length * 8 - 2) + 1;
            setBit(bytes, bitToChange, !getBit(bytes, bitToChange));
            for (int i = bitToChange + 1; i < bytes.length * 8; i++) {
                setBit(bytes, i, false);
            }
            allowAddress = InetAddress.getByAddress(bytes).getHostAddress();

            prefixLengthGood = bitToChange;
            prefixLengthBad = bitToChange + 1;
        }

        private static boolean getBit(byte[] bytes, int pos) {
            return (bytes[pos / 8] & (1 << (7 - (pos % 8)))) != 0;
        }

        private static void setBit(byte[] bytes, int pos, boolean value) {
            byte byteValue = (byte)(1 << (7 - (pos % 8)));
            if (value) {
                bytes[pos / 8] = (byte)(bytes[pos / 8] | byteValue);
            } else {
                bytes[pos / 8] &= (~byteValue);
            }
        }
    }

    private static String localAddr;
    private static List<MaskTest> maskTests = new LinkedList<>();

    private static void init() throws Exception {
        InetAddress addrs[] = InetAddress.getAllByName("localhost");
        if (addrs.length == 0) {
            throw new RuntimeException("No addresses is returned for 'localhost'");
        }
        localAddr = addrs[0].getHostAddress();
        System.out.println("localhost address: " + localAddr);

        for (int i =  0; i < addrs.length; i++) {
            maskTests.add(new MaskTest(addrs[i]));
        }
    }

    public static void main(String[] args) throws Exception {
        init();

        positiveTest("DefaultTest", null);

        positiveTest("ExplicitDefaultTest", "*");

        positiveTest("AllowTest", localAddr);

        positiveTest("MultiAllowTest", localAddr + "+10.0.0.0/8+172.16.0.0/12+192.168.0.0/24");

        negativeTest("DenyTest", "0.0.0.0");

        badAllowOptionTest("MultiDenyTest", localAddr + ";192.168.0.0/24");

        badAllowOptionTest("EmptyAllowOptionTest", "");

        badAllowOptionTest("ExplicitMultiDefault1Test", "*+" + localAddr);

        badAllowOptionTest("ExplicitMultiDefault2Test", localAddr + "+*");

        for (MaskTest test: maskTests) {
            localAddr = test.localAddress;
            positiveTest("PositiveMaskTest(" + test.localAddress + ")",
                         test.allowAddress + "/" + test.prefixLengthGood);
            negativeTest("NegativeMaskTest(" + test.localAddress + ")",
                         test.allowAddress + "/" + test.prefixLengthBad);
        }

        System.out.println("\nTest PASSED");
    }

}
