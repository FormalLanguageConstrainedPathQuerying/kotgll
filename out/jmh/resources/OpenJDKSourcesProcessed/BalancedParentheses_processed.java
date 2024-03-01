/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6449574
 * @library /test/lib
 * @summary Invalid ldap filter is accepted and processed
 */

import java.io.*;
import javax.naming.*;
import javax.naming.directory.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Hashtable;

import java.net.Socket;
import java.net.ServerSocket;

import jdk.test.lib.net.URIBuilder;

public class BalancedParentheses {
    static boolean separateServerThread = true;

    volatile int serverPort = 0;

    volatile static boolean serverReady = false;

    void doServerSide() throws Exception {
        ServerSocket serverSock = new ServerSocket();

        SocketAddress sockAddr = new InetSocketAddress(
                InetAddress.getLoopbackAddress(), 0);
        serverSock.bind(sockAddr);

        serverPort = serverSock.getLocalPort();
        serverReady = true;

        Socket socket = serverSock.accept();
        System.out.println("Server: Connection accepted");

        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();

        while (is.read() != -1) {
            is.skip(is.available());
            break;
        }

        byte[] bindResponse = {0x30, 0x0C, 0x02, 0x01, 0x01, 0x61, 0x07, 0x0A,
                               0x01, 0x00, 0x04, 0x00, 0x04, 0x00};
        os.write(bindResponse);
        os.flush();

        while (is.read() != -1) {
            is.skip(is.available());
        }

        is.close();
        os.close();
        socket.close();
        serverSock.close();
    }

    void doClientSide() throws Exception {
        while (!serverReady) {
            Thread.sleep(50);
        }

        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                                "com.sun.jndi.ldap.LdapCtxFactory");
        String providerURL = URIBuilder.newBuilder()
                .scheme("ldap")
                .loopback()
                .port(serverPort)
                .build().toString();
        env.put(Context.PROVIDER_URL, providerURL);
        env.put("com.sun.jndi.ldap.read.timeout", "1000");


        DirContext context = new InitialDirContext(env);

        SearchControls scs = new SearchControls();
        scs.setSearchScope(SearchControls.SUBTREE_SCOPE);

        try {
            NamingEnumeration<SearchResult> answer = context.search(
                                        "o=sun,c=us", "(&(cn=Bob)))", scs);
        } catch (InvalidSearchFilterException isfe) {
            System.out.println("Expected exception: " + isfe.getMessage());
        } catch (NamingException ne) {
            throw new Exception("Expect a InvalidSearchFilterException", ne);
        }

        try {
            NamingEnumeration<SearchResult> answer = context.search(
                                        "o=sun,c=us", ")(&(cn=Bob)", scs);
        } catch (InvalidSearchFilterException isfe) {
            System.out.println("Expected exception: " + isfe.getMessage());
        } catch (NamingException ne) {
            throw new Exception("Expect a InvalidSearchFilterException", ne);
        }

        try {
            NamingEnumeration<SearchResult> answer = context.search(
                                        "o=sun,c=us", "(&(cn=Bob))", scs);
        } catch (InvalidSearchFilterException isfe) {
            throw new Exception("Unexpected ISFE", isfe);
        } catch (NamingException ne) {
            System.out.println("Expected exception: " + ne.getMessage());
        }

        context.close();
    }

    /*
     * ============================================================
     * The remainder is just support stuff
     */

    Thread clientThread = null;
    Thread serverThread = null;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        System.err.println(e);
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            doServerSide();
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died...");
                        clientException = e;
                    }
                }
            };
            clientThread.start();
        } else {
            doClientSide();
        }
    }

    BalancedParentheses() throws Exception {
        if (separateServerThread) {
            startServer(true);
            startClient(false);
        } else {
            startClient(true);
            startServer(false);
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         *
         * If the main thread excepted, that propagates back
         * immediately.  If the other thread threw an exception, we
         * should report back.
         */
        if (serverException != null) {
            System.out.print("Server Exception:");
            throw serverException;
        }
        if (clientException != null) {
            System.out.print("Client Exception:");
            throw clientException;
        }
    }

    public static void main(String[] args) throws Exception {
        new BalancedParentheses();
    }

}
