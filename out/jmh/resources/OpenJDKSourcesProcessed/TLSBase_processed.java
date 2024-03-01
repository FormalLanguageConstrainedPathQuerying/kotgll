/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a base setup for creating a server and clients.  All clients will
 * connect to the server on construction.  The server constructor must be run
 * first.  The idea is for the test code to be minimal as possible without
 * this library class being complicated.
 *
 * Server.done() must be called or the server will never exit and hang the test.
 *
 * After construction, reading and writing are allowed from either side,
 * or a combination write/read from both sides for verifying text.
 *
 * The TLSBase.Server and TLSBase.Client classes are to allow full access to
 * the SSLSession for verifying data.
 *
 * See SSLSession/CheckSessionContext.java for an example
 *
 */

abstract public class TLSBase {
    static String pathToStores = "../etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    SSLContext sslContext;
    static int serverPort;
    String name;

    TLSBase() {
        String keyFilename =
                System.getProperty("test.src", "./") + "/" + pathToStores +
                        "/" + keyStoreFile;
        String trustFilename =
                System.getProperty("test.src", "./") + "/" + pathToStores +
                        "/" + trustStoreFile;
        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", passwd);
    }

    byte[] read(SSLSocket sock) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
            String s = reader.readLine();
            System.err.println("(read) " + name + ": " + s);
            return s.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void write(SSLSocket sock, byte[] data) {
        try {
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(sock.getOutputStream()));
            out.println(new String(data));
            out.flush();
            System.err.println("(write)" + name + ": " + new String(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Server constructor must be called before any client operation so the
     * tls server is ready.  There should be no timing problems as the
     */
    static class Server extends TLSBase {
        SSLServerSocketFactory fac;
        SSLServerSocket ssock;
        ConcurrentHashMap<Integer, SSLSocket> clientMap =
                new ConcurrentHashMap<>();
        boolean exit = false;
        Thread t;

        Server() {
            super();
            name = "server";
            try {
                sslContext = SSLContext.getDefault();
                fac = sslContext.getServerSocketFactory();
                ssock = (SSLServerSocket) fac.createServerSocket(0);
                serverPort = ssock.getLocalPort();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }

            t = new Thread(() -> {
                try {
                    while (true) {
                        System.err.println("Server ready on port " +
                                serverPort);
                        SSLSocket c = (SSLSocket)ssock.accept();
                        clientMap.put(c.getPort(), c);
                        try {
                            write(c, read(c));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Server Down");
                    ex.printStackTrace();
                }
            });
            t.start();
        }

        void done() {
            try {
                t.interrupt();
                ssock.close();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }

        byte[] read(Client client) {
            SSLSocket s = clientMap.get(Integer.valueOf(client.getPort()));
            if (s == null) {
                System.err.println("No socket found, port " + client.getPort());
            }
            return read(s);
        }

        void write(Client client, byte[] data) {
            write(clientMap.get(client.getPort()), data);
        }

        boolean writeRead(Client client, String s) {
            write(client, s.getBytes());
            return (Arrays.compare(s.getBytes(), client.read()) == 0);
        }

        SSLSession getSession(Client c) {
            SSLSocket s = clientMap.get(Integer.valueOf(c.getPort()));
            return s.getSession();
        }

        void close(Client c) throws IOException {
            SSLSocket s = clientMap.get(Integer.valueOf(c.getPort()));
            s.close();
        }
    }

    /**
     * Client side will establish a connection from the constructor and wait.
     * It must be run after the Server constructor is called.
     */
    static class Client extends TLSBase {
        SSLSocket sock;

        Client() {
            super();
            try {
                sslContext = SSLContext.getDefault();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
            connect();
        }

        public SSLSocket connect() {
            try {
                sslContext = SSLContext.getDefault();
                sock = (SSLSocket)sslContext.getSocketFactory().createSocket();
                sock.connect(new InetSocketAddress("localhost", serverPort));
                System.err.println("Client connected using port " +
                        sock.getLocalPort());
                name = "client(" + sock.toString() + ")";
                write("Hello");
                read();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return sock;
        }

        byte[] read() {
            return read(sock);
        }

        void write(byte[] data) {
            write(sock, data);
        }
        void write(String s) {
            write(sock, s.getBytes());
        }

        boolean writeRead(Server server, String s) {
            write(s.getBytes());
            return (Arrays.compare(s.getBytes(), server.read(this)) == 0);
        }

        int getPort() {
            return sock.getLocalPort();
        }

        void close() throws IOException {
            sock.close();
        }
    }
}
