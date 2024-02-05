/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8179559 8225239
 * @library /test/lib
 * @modules java.base/java.net:open
 */

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.lang.reflect.Method;
import jdk.test.lib.Platform;

public class NetworkInterfaceRetrievalTests {
    public static void main(String[] args) throws Exception {
        int checkFailureCount = 0;

        Method isBound = NetworkInterface.class.getDeclaredMethod("isBoundInetAddress", InetAddress.class);
        isBound.setAccessible(true);

        try {
            Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();

                String dName = ni.getDisplayName();
                if (Platform.isWindows() && dName != null && dName.contains("Teredo"))
                    continue;

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                System.out.println("############ Checking network interface + "
                        + ni + " #############");
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    System.out.println("************ Checking address  + "
                            + addr + " *************");
                    NetworkInterface addrNetIf = NetworkInterface
                            .getByInetAddress(addr);
                    if (addrNetIf.equals(ni)) {
                        System.out.println("Retreived net if " + addrNetIf
                                + " equal to owning net if " + ni);
                    } else {
                        System.out.println("Retreived net if " + addrNetIf
                                + "NOT  equal to owning net if " + ni
                                + "***********");
                        checkFailureCount++;
                    }

                    if (!((boolean)isBound.invoke(null, addr))) {
                        System.out.println("Retreived net if bound addr " + addr
                                + "NOT shown as bound using NetworkInterface.isBoundAddress "
                                + "***********");
                        checkFailureCount++;
                    }
                }
            }

        } catch (Exception ex) {

        }

        if (checkFailureCount > 0) {
            throw new RuntimeException(
                    "NetworkInterface lookup by address didn't match owner network interface");
        }
    }
}
