/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.Hashtable;
import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import jdk.test.lib.net.URIBuilder;

/**
 * @test
 * @bug 8290367
 * @summary Check if com.sun.jndi.ldap.object.trustSerialData covers the creation
 *          of RMI remote objects from the 'javaRemoteLocation' LDAP attribute.
 * @modules java.naming/com.sun.jndi.ldap
 * @library /test/lib ../lib /javax/naming/module/src/test/test/
 * @build LDAPServer LDAPTestUtils
 *
 * @run main/othervm RemoteLocationAttributeTest
 * @run main/othervm -Dcom.sun.jndi.ldap.object.trustSerialData
 *                   RemoteLocationAttributeTest
 * @run main/othervm -Dcom.sun.jndi.ldap.object.trustSerialData=false
 *                   RemoteLocationAttributeTest
 * @run main/othervm -Dcom.sun.jndi.ldap.object.trustSerialData=true
 *                   RemoteLocationAttributeTest
 * @run main/othervm -Dcom.sun.jndi.ldap.object.trustSerialData=TrUe
 *                   RemoteLocationAttributeTest
 */

public class RemoteLocationAttributeTest {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket();

        SocketAddress sockAddr = new InetSocketAddress(
                InetAddress.getLoopbackAddress(), 0);
        serverSocket.bind(sockAddr);

        String providerURL = URIBuilder.newBuilder()
                .scheme("ldap")
                .loopback()
                .port(serverSocket.getLocalPort())
                .buildUnchecked().toString();

        Hashtable<Object, Object> env;

        env = LDAPTestUtils.initEnv(serverSocket, providerURL,
                RemoteLocationAttributeTest.class.getName(), args, false);

        DirContext ctx = null;
        try (serverSocket) {
            System.err.println(env);
            ctx = new InitialDirContext(env);
            Object lookupResult = ctx.lookup("Test");
            System.err.println("Lookup result:" + lookupResult);
            if (!RECONSTRUCTION_ALLOWED) {
                throw new AssertionError("Unexpected successful lookup");
            }
        } catch (ServiceUnavailableException | CommunicationException connectionException) {
            System.err.println("Got one of connection exceptions:" + connectionException);
            if (!RECONSTRUCTION_ALLOWED) {
                throw new AssertionError("Reconstruction not blocked, as expected");
            }
        } catch (NamingException ne) {
            String message = ne.getMessage();
            System.err.printf("Got NamingException with message: '%s'%n", message);
            if (RECONSTRUCTION_ALLOWED && EXPECTED_NAMING_EXCEPTION_MESSAGE.equals(message)) {
                throw new AssertionError("Reconstruction unexpectedly blocked");
            }
            if (!RECONSTRUCTION_ALLOWED && !EXPECTED_NAMING_EXCEPTION_MESSAGE.equals(message)) {
                throw new AssertionError("Reconstruction not blocked");
            }
        } finally {
            LDAPTestUtils.cleanup(ctx);
        }
    }

    private static final boolean RECONSTRUCTION_ALLOWED =
            Boolean.getBoolean("com.sun.jndi.ldap.object.trustSerialData");

    private static final String EXPECTED_NAMING_EXCEPTION_MESSAGE = "Object deserialization is not allowed";
}
