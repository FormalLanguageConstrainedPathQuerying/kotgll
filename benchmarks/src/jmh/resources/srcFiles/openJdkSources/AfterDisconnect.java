/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8231880 8231258
 * @library /test/lib
 * @summary Test DatagramChannel bound to specific address/ephemeral port after disconnect
 * @run testng/othervm AfterDisconnect
 * @run testng/othervm -Djava.net.preferIPv4Stack=true AfterDisconnect
 * @run testng/othervm -Djava.net.preferIPv6Addresses=true AfterDisconnect
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

import jdk.test.lib.net.IPSupport;

public class AfterDisconnect {

    @Test
    public void execute() throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();
        boolean preferIPv6 = Boolean.getBoolean("java.net.preferIPv6Addresses");
        InetAddress lb = InetAddress.getLoopbackAddress();

        try (DatagramChannel dc = DatagramChannel.open()) {
            System.out.println("Test with default");
            dc.bind(new InetSocketAddress(lb, 0));
            test(dc);
            test(dc);
        }

        if (IPSupport.hasIPv6()) {
            System.out.println("Test with IPv6 socket");
            try (DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET6)) {
                dc.bind(new InetSocketAddress(lb, 0));
                test(dc);
                test(dc);
            }
        }

        if (IPSupport.hasIPv4() && !preferIPv6) {
            System.out.println("Test with IPv4 socket");
            try (DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET)) {
                dc.bind(new InetSocketAddress(lb, 0));
                test(dc);
                test(dc);
            }
        }
    }

    void test(DatagramChannel dc) throws IOException {
        testLocalAddress(dc);
        testSocketOptions(dc);
        testSelectorRegistration(dc);
        testMulticastGroups(dc);
    }

    /**
     * Test that disconnect restores local address
     */
    void testLocalAddress(DatagramChannel dc) throws IOException {
        try (DatagramChannel server = DatagramChannel.open()) {
            server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

            SocketAddress local = dc.getLocalAddress();
            SocketAddress remote = server.getLocalAddress();

            dc.connect(remote);
            assertTrue(dc.isConnected());
            assertEquals(dc.getLocalAddress(), local);
            assertEquals(dc.getRemoteAddress(), remote);

            dc.disconnect();
            assertFalse(dc.isConnected());
            assertEquals(dc.getLocalAddress(), local);
            assertTrue(dc.getRemoteAddress() == null);
        }
    }

    /**
     * Test that disconnect does not change socket options
     */
    void testSocketOptions(DatagramChannel dc) throws IOException {
        dc.setOption(StandardSocketOptions.SO_SNDBUF, 32*1024);
        dc.setOption(StandardSocketOptions.SO_RCVBUF, 64*1024);
        InetAddress ia = dc.socket().getLocalAddress();
        NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
        if (ni != null && ni.supportsMulticast())
            dc.setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);

        Map<SocketOption<?>, Object> map = options(dc);

        dc.connect(dc.getLocalAddress());
        dc.disconnect();

        assertEquals(map, options(dc));
    }

    /**
     * Returns a map of the given channel's socket options and values.
     */
    private Map<SocketOption<?>, Object> options(DatagramChannel dc) throws IOException {
        Map<SocketOption<?>, Object> map = new HashMap<>();
        for (SocketOption<?> option : dc.supportedOptions()) {
            try {
                Object value = dc.getOption(option);
                if (value != null) {
                    map.put(option, value);
                }
            } catch (IOException ignore) { }
        }
        return map;
    }

    /**
     * Test that disconnect does not interfere with Selector registrations
     */
    void testSelectorRegistration(DatagramChannel dc) throws IOException {
        try (Selector sel = Selector.open()) {
            dc.configureBlocking(false);
            SelectionKey key = dc.register(sel, SelectionKey.OP_READ);

            sel.selectNow();

            dc.connect(dc.getLocalAddress());
            dc.disconnect();

            assertTrue(key.isValid());

            ByteBuffer bb = ByteBuffer.allocate(100);
            SocketAddress sender = dc.receive(bb);
            assertTrue(sender == null);

            dc.send(ByteBuffer.wrap("Hello".getBytes("UTF-8")), dc.getLocalAddress());
            assertFalse(key.isReadable());
            while (sel.select() == 0);
            assertTrue(key.isReadable());
            sender = dc.receive(bb);
            assertEquals(sender, dc.getLocalAddress());

            key.cancel();
            sel.selectNow();
            dc.configureBlocking(true);
        }
    }

    /**
     * Test that disconnect does not interfere with multicast group membership
     */
    void testMulticastGroups(DatagramChannel dc) throws IOException {
        InetAddress localAddress = dc.socket().getLocalAddress();
        InetAddress group;
        if (localAddress instanceof Inet6Address) {
            group = InetAddress.getByName("ff02::a");
        } else {
            group = InetAddress.getByName("225.4.5.6");
        }
        NetworkInterface ni = NetworkInterface.getByInetAddress(localAddress);
        if (ni != null && ni.supportsMulticast()) {
            MembershipKey key = dc.join(group, ni);

            dc.connect(dc.getLocalAddress());
            dc.disconnect();

            assertTrue(key.isValid());

            dc.send(ByteBuffer.wrap("Hello".getBytes("UTF-8")), dc.getLocalAddress());
            ByteBuffer bb = ByteBuffer.allocate(100);
            SocketAddress sender = dc.receive(bb);
            assertEquals(sender, dc.getLocalAddress());

            key.drop();
        }
    }
}
