/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5039210
 * @summary test on a client notification deadlock.
 * @author Shanliang JIANG
 *
 * @run clean DeadLockTest
 * @run build DeadLockTest
 * @run main DeadLockTest
 */

import java.net.MalformedURLException;
import java.io.IOException;
import java.util.HashMap;

import javax.management.*;
import javax.management.remote.*;

public class DeadLockTest {
    private static final String[] protocols = {"rmi", "iiop", "jmxmp"};
    private static final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

    public static void main(String[] args) {
        System.out.println(">>> test on a client notification deadlock.");

        boolean ok = true;
        for (int i = 0; i < protocols.length; i++) {
            try {
                test(protocols[i]);
            } catch (Exception e) {
                System.out.println(">>> Test failed for " + protocols[i]);
                e.printStackTrace(System.out);
            }
        }

        System.out.println(">>> Test passed");
    }

    private static void test(String proto)
            throws Exception {
        System.out.println(">>> Test for protocol " + proto);

        JMXServiceURL u = null;
        JMXConnectorServer server = null;

        HashMap env = new HashMap(2);
        env.put("jmx.remote.x.server.connection.timeout", "1000");

        env.put("jmx.remote.x.client.connection.check.period", "0");

        try {
            u = new JMXServiceURL(proto, null, 0);
            server = JMXConnectorServerFactory.newJMXConnectorServer(u, env, mbs);
        } catch (MalformedURLException e) {
            System.out.println(">>> Skipping unsupported URL " + proto);
        }

        server.start();

        JMXServiceURL addr = server.getAddress();

        long st = 2000;
        MyListener myListener;

        do {
            JMXConnector client = JMXConnectorFactory.connect(addr, env);
            MBeanServerConnection conn = client.getMBeanServerConnection();
            myListener = new MyListener(conn);
            client.addConnectionNotificationListener(myListener, null, null);

            Thread.sleep(st);

            conn.getDefaultDomain();

            Thread.sleep(100);

            client.close();
            Thread.sleep(100);

            st += 2000;

        } while(!myListener.isDone());

        server.stop();
    }

    private static class MyListener implements NotificationListener {
        public MyListener(MBeanServerConnection conn) {
            this.conn = conn;
        }

        public void handleNotification(Notification n, Object h) {
            if (n instanceof JMXConnectionNotification) {
                JMXConnectionNotification jcn = (JMXConnectionNotification)n;
                final String type = jcn.getType();
                System.out.println(">>> The listener receives notif with the type:"+type);

                if (JMXConnectionNotification.CLOSED.equals(type) ||
                    JMXConnectionNotification.FAILED.equals(type)) {

                    synchronized(this) {
                        done = false;
                    }

                    try {
                        conn.getDefaultDomain();
                    } catch (IOException ioe) {
                    }

                    synchronized(this) {
                        done = true;
                    }

                    System.out.println(">>> The listener is not blocked!");
                }
            }
        }

        public boolean isDone() {
            synchronized(this) {
                return done;
            }
        }

        private boolean done = false;
        private MBeanServerConnection conn;
    }
}
