/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6227124
 * @summary Test that setting an MBeanServerForwarder on an already
 *          started RMI connector server has the expected behavior.
 * @author Luis-Miguel Alventosa
 *
 * @run clean MBSFPreStartPostStartTest
 * @run build MBSFPreStartPostStartTest
 * @run main MBSFPreStartPostStartTest
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;

public class MBSFPreStartPostStartTest {

    public static class MBSFInvocationHandler implements InvocationHandler {

        public static MBeanServerForwarder newProxyInstance() {

            final InvocationHandler handler = new MBSFInvocationHandler();

            final Class[] interfaces =
                new Class[] {MBeanServerForwarder.class};

            Object proxy = Proxy.newProxyInstance(
                                 MBeanServerForwarder.class.getClassLoader(),
                                 interfaces,
                                 handler);

            return MBeanServerForwarder.class.cast(proxy);
        }

        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

            final String methodName = method.getName();

            if (methodName.equals("getMBeanServer")) {
                return mbs;
            }

            if (methodName.equals("setMBeanServer")) {
                if (args[0] == null)
                    throw new IllegalArgumentException("Null MBeanServer");
                if (mbs != null)
                    throw new IllegalArgumentException("MBeanServer object " +
                                                       "already initialized");
                mbs = (MBeanServer) args[0];
                return null;
            }

            flag = true;

            return method.invoke(mbs, args);
        }

        public boolean getFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        private boolean flag;
        private MBeanServer mbs;
    }

    /**
     * Run test
     */
    public int runTest(boolean setBeforeStart) throws Exception {

        echo("=-=-= MBSFPreStartPostStartTest: Set MBSF " +
             (setBeforeStart ? "before" : "after") +
             " starting the connector server =-=-=");

        JMXConnectorServer server = null;
        JMXConnector client = null;

        final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        try {
            final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:

            server = JMXConnectorServerFactory.newJMXConnectorServer(url,
                                                                     null,
                                                                     mbs);

            MBeanServerForwarder mbsf =
                MBSFInvocationHandler.newProxyInstance();

            if (setBeforeStart)
                server.setMBeanServerForwarder(mbsf);

            server.start();

            if (!setBeforeStart)
                server.setMBeanServerForwarder(mbsf);

            client = server.toJMXConnector(null);

            client.connect(null);

            final MBeanServerConnection mbsc =
                client.getMBeanServerConnection();

            mbsc.getDefaultDomain();

            MBSFInvocationHandler mbsfih =
                (MBSFInvocationHandler) Proxy.getInvocationHandler(mbsf);
            if (mbsfih.getFlag() == true) {
                echo("OK: Did go into MBeanServerForwarder!");
            } else {
                echo("KO: Didn't go into MBeanServerForwarder!");
                return 1;
            }
        } catch (Exception e) {
            echo("Failed to perform operation: " + e);
            return 1;
        } finally {
            if (client != null)
                client.close();

            if (server != null)
                server.stop();

            if (mbs != null)
                MBeanServerFactory.releaseMBeanServer(mbs);
        }

        return 0;
    }

    /*
     * Print message
     */
    private static void echo(String message) {
        System.out.println(message);
    }

    /*
     * Standalone entry point.
     *
     * Run the test and report to stdout.
     */
    public static void main (String args[]) throws Exception {

        int error = 0;

        MBSFPreStartPostStartTest test = new MBSFPreStartPostStartTest();

        error += test.runTest(true);
        error += test.runTest(false);

        if (error > 0) {
            echo(">>> Unhappy Bye, Bye!");
            throw new IllegalStateException(
                "Test FAILED: Unexpected error!");
        } else {
            echo(">>> Happy Bye, Bye!");
        }
    }
}
