/*
 * Copyright (c) 2005, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6261831
 * @summary Tests the use of the subject delegation feature on the authenticated
 *          principals within the RMI connector server's creator codebase.
 * @author Luis-Miguel Alventosa
 * @modules java.management.rmi
 *          java.management/com.sun.jmx.remote.security
 * @run clean SubjectDelegation2Test SimpleStandard SimpleStandardMBean
 * @run build SubjectDelegation2Test SimpleStandard SimpleStandardMBean
 * @run main/othervm -Djava.security.manager=allow SubjectDelegation2Test policy21 ok
 * @run main/othervm -Djava.security.manager=allow SubjectDelegation2Test policy22 ko
 * @run main/othervm -Djava.security.manager=allow SubjectDelegation2Test policy23 ko
 * @run main/othervm -Djava.security.manager=allow SubjectDelegation2Test policy24 ok
 * @run main/othervm -Djava.security.manager=allow SubjectDelegation2Test policy25 ko
 */

import com.sun.jmx.remote.security.JMXPluggableAuthenticator;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Properties;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class SubjectDelegation2Test {

    public static void main(String[] args) throws Exception {
        String policyFile = args[0];
        String testResult = args[1];
        System.out.println("Policy file = " + policyFile);
        System.out.println("Expected test result = " + testResult);
        JMXConnectorServer jmxcs = null;
        JMXConnector jmxc = null;
        try {
            System.out.println("Start RMI registry...");
            Registry reg = null;
            int port = 5880;
            while (port++ < 5900) {
                try {
                    reg = LocateRegistry.createRegistry(port);
                    System.out.println("RMI registry running on port " + port);
                    break;
                } catch (RemoteException e) {
                    System.out.println("Failed to create RMI registry " +
                                       "on port " + port);
                }
            }
            if (reg == null) {
                System.exit(1);
            }
            final String passwordFile = System.getProperty("test.src") +
                File.separator + "jmxremote.password";
            System.out.println("Password file = " + passwordFile);
            final String policy = System.getProperty("test.src") +
                File.separator + policyFile;
            System.out.println("PolicyFile = " + policy);
            System.setProperty("java.security.policy", policy);
            System.out.println("Create the MBean server");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            System.out.println("Create SimpleStandard MBean");
            SimpleStandard s = new SimpleStandard("monitorRole");
            mbs.registerMBean(s, new ObjectName("MBeans:type=SimpleStandard"));
            Properties props = new Properties();
            props.setProperty("jmx.remote.x.password.file", passwordFile);
            System.out.println("Initialize environment map");
            HashMap env = new HashMap();
            env.put("jmx.remote.authenticator",
                    new JMXPluggableAuthenticator(props));
            System.setSecurityManager(new SecurityManager());
            System.out.println("Create an RMI connector server");
            JMXServiceURL url = new JMXServiceURL("rmi", null, 0);

            jmxcs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
            jmxcs.start();
            System.out.println("Create an RMI connector client");
            HashMap cli_env = new HashMap();
            String[] credentials = new String[] { "monitorRole" , "QED" };
            cli_env.put("jmx.remote.credentials", credentials);
            jmxc = JMXConnectorFactory.connect(jmxcs.getAddress(), cli_env);
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
            System.out.println("Domains:");
            String domains[] = mbsc.getDomains();
            for (int i = 0; i < domains.length; i++) {
                System.out.println("\tDomain[" + i + "] = " + domains[i]);
            }
            System.out.println("MBean count = " + mbsc.getMBeanCount());
            String oldState =
                (String) mbsc.getAttribute(
                              new ObjectName("MBeans:type=SimpleStandard"),
                              "State");
            System.out.println("Old State = \"" + oldState + "\"");
            System.out.println("Set State to \"changed state\"");
            mbsc.setAttribute(new ObjectName("MBeans:type=SimpleStandard"),
                              new Attribute("State", "changed state"));
            String newState =
                (String) mbsc.getAttribute(
                              new ObjectName("MBeans:type=SimpleStandard"),
                              "State");
            System.out.println("New State = \"" + newState + "\"");
            if (!newState.equals("changed state")) {
                System.out.println("Invalid State = \"" + newState + "\"");
                System.exit(1);
            }
            System.out.println("Add notification listener...");
            mbsc.addNotificationListener(
                 new ObjectName("MBeans:type=SimpleStandard"),
                 new NotificationListener() {
                     public void handleNotification(Notification notification,
                                                    Object handback) {
                         System.out.println("Received notification: " +
                                            notification);
                     }
                 },
                 null,
                 null);
            System.out.println("Unregister SimpleStandard MBean...");
            mbsc.unregisterMBean(new ObjectName("MBeans:type=SimpleStandard"));
        } catch (SecurityException e) {
            if (testResult.equals("ko")) {
                System.out.println("Got expected security exception = " + e);
            } else {
                System.out.println("Got unexpected security exception = " + e);
                e.printStackTrace();
                throw e;
            }
        } catch (Exception e) {
            System.out.println("Unexpected exception caught = " + e);
            e.printStackTrace();
            throw e;
        } finally {
            if (jmxc != null)
                jmxc.close();
            if (jmxcs != null)
                jmxcs.stop();
            System.out.println("Bye! Bye!");
        }
    }
}
