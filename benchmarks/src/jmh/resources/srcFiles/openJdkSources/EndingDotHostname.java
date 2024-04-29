/*
 * Copyright (C) 2022 THL A29 Limited, a Tencent company. All rights reserved.
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
 * @bug 8065422
 * @summary Trailing dot in hostname causes TLS handshake to fail
 * @library /javax/net/ssl/templates
 * @run main/othervm --add-opens java.base/sun.security.ssl=ALL-UNNAMED
 *      -Djdk.net.hosts.file=hostsForExample EndingDotHostname
 */

import javax.net.ssl.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EndingDotHostname {
    public static void main(String[] args) throws Exception {
        System.setProperty("jdk.net.hosts.file", "hostsForExample");
        (new EndingDotHostname()).run();
    }

    public void run() throws Exception {
        bootUp();
    }

    private Thread serverThread = null;
    private volatile Exception serverException = null;
    private volatile Exception clientException = null;

    protected final CountDownLatch serverCondition = new CountDownLatch(1);

    protected final CountDownLatch clientCondition = new CountDownLatch(1);

    protected volatile int serverPort = 0;

    private void bootUp() throws Exception {
        Exception startException = null;
        try {
            startServer();
            startClient();
        } catch (Exception e) {
            startException = e;
        }

        if (serverThread != null) {
            serverThread.join();
        }

        Exception local = clientException;
        Exception remote = serverException;

        Exception exception = null;

        if ((local != null) && (remote != null)) {
            local.initCause(remote);
            exception = local;
        } else if (local != null) {
            exception = local;
        } else if (remote != null) {
            exception = remote;
        } else if (startException != null) {
            exception = startException;
        }

        if (exception != null) {
            if (exception != startException && startException != null) {
                exception.addSuppressed(startException);
            }
            throw exception;
        }

    }

    private void startServer() {
        serverThread = new Thread(() -> {
            try {
                doServerSide();
            } catch (Exception e) {
                serverException = e;
            }
        });

        serverThread.start();
    }

    private void startClient() {
        try {
            doClientSide();
        } catch (Exception e) {
            clientException = e;
        }
    }

    protected void doServerSide() throws Exception {
        SSLContext context = SSLExampleCert.createServerSSLContext();
        SSLServerSocketFactory sslssf = context.getServerSocketFactory();

        SSLServerSocket sslServerSocket =
                (SSLServerSocket)sslssf.createServerSocket();
        sslServerSocket.bind(new InetSocketAddress(
                InetAddress.getLoopbackAddress(), 0));
        serverPort = sslServerSocket.getLocalPort();

        serverCondition.countDown();

        SSLSocket sslSocket;
        try {
            sslServerSocket.setSoTimeout(30000);
            sslSocket = (SSLSocket)sslServerSocket.accept();
        } catch (SocketTimeoutException ste) {
            System.out.println(
                    "No incoming client connection in 30 seconds. " +
                            "Ignore in server side.");
            return;
        } finally {
            sslServerSocket.close();
        }

        try {
            boolean clientIsReady =
                    clientCondition.await(30L, TimeUnit.SECONDS);

            if (clientIsReady) {
                runServerApplication(sslSocket);
            } else {    
                System.out.println(
                        "The client is not the expected one or timeout. " +
                                "Ignore in server side.");
            }
        } finally {
            sslSocket.close();
        }
    }

    protected void runServerApplication(SSLSocket socket) throws Exception {
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslIS.read();
        sslOS.write(85);
        sslOS.flush();
    }

    protected void doClientSide() throws Exception {
        boolean serverIsReady =
                serverCondition.await(90L, TimeUnit.SECONDS);
        if (!serverIsReady) {
            System.out.println(
                    "The server is not ready yet in 90 seconds. " +
                            "Ignore in client side.");
            return;
        }

        SSLContext context = SSLExampleCert.createClientSSLContext();
        SSLSocketFactory sslsf = context.getSocketFactory();

        try (SSLSocket sslSocket = (SSLSocket)sslsf.createSocket(
                "www.example.com.", serverPort)) {
            SSLParameters sslParameters = sslSocket.getSSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            sslSocket.setSSLParameters(sslParameters);

            clientCondition.countDown();


            runClientApplication(sslSocket);
        }
    }

    protected void runClientApplication(SSLSocket socket) throws Exception {
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();
    }
}

