/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4208804
 *
 * @summary Incoming connections should be subject to timeout
 * @author Adrian Colley
 *
 * @build TestIface TestImpl TestImpl_Stub
 * @run main/othervm/policy=security.policy/timeout=60
 *     -Dsun.rmi.transport.tcp.readTimeout=5000 ReadTimeoutTest
 */

/* This test sets a very short read timeout, exports an object, and then
 * connects to the port and does nothing.  The server should close the
 * connection after the timeout.  If that doesn't happen, the test fails.
 *
 * A background thread does the read.  The foreground waits for DELAY*4
 * and then aborts.  This should give sufficient margin for timing slop.
 */

import java.rmi.*;
import java.rmi.server.RMISocketFactory;
import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReadTimeoutTest
{
    private static final int DELAY = 5000;      

    public static void main(String[] args)
        throws Exception
    {
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());

        System.setProperty("sun.rmi.transport.tcp.readTimeout", Integer.toString(DELAY));

        System.err.println("(installing socket factory)");
        SomeFactory fac = new SomeFactory();
        RMISocketFactory.setSocketFactory(fac);

        TestImpl impl = new TestImpl();

        System.err.println("(exporting remote object)");
        TestIface stub = impl.export();
        Socket DoS = null;
        try {
            int port = fac.whichPort();

            if (port == 0)
                throw new Error("TEST FAILED: export didn't reserve a port(?)");

            System.err.println("(connecting to listening port on localhost:" +
                               port + ")");
            DoS = new Socket(InetAddress.getLoopbackAddress(), port);
            InputStream stream = DoS.getInputStream();

            CountDownLatch done = new CountDownLatch(1);
            (new SomeReader(stream, done)).start();

            if (done.await(DELAY * 4, TimeUnit.SECONDS)) {
                System.err.println("TEST PASSED.");
            } else {
                throw new Error("TEST FAILED.");
            }

        } catch (InterruptedException ie) {
            throw new Error("Unexpected interrupt", ie);
        } finally {
            try {
                if (DoS != null)
                    DoS.close();        
                impl.unexport();
            } catch (Throwable unmatter) {
            }
        }

    }

    private static class SomeFactory
        extends RMISocketFactory
    {
        private int servport = 0;

        @Override
        public Socket createSocket(String h, int p)
            throws IOException
        {
            return (new Socket(h, p));
        }

        /** Create a server socket and remember which port it's on.
         * Aborts if createServerSocket(0) is called twice, because then
         * it doesn't know whether to remember the first or second port.
         */
        @Override
        public ServerSocket createServerSocket(int p)
            throws IOException
        {
            ServerSocket ss;
            ss = new ServerSocket(p);
            if (p == 0) {
                if (servport != 0) {
                    System.err.println("TEST FAILED: " +
                                       "Duplicate createServerSocket(0)");
                    throw new Error("Test aborted (createServerSocket)");
                }
                servport = ss.getLocalPort();
            }
            return (ss);
        }

        /** Return which port was reserved by createServerSocket(0).
         * If the return value was 0, createServerSocket(0) wasn't called.
         */
        public int whichPort() {
            return (servport);
        }
    } 

    protected static class SomeReader extends Thread {
        private final InputStream readon;
        private final CountDownLatch done;

        public SomeReader(InputStream s, CountDownLatch done) {
            super();
            this.setDaemon(true);
            this.readon = s;
            this.done = done;
        }

        @Override
        public void run() {
            try {
                int c = this.readon.read();
                if (c != -1)
                    throw new Error ("Server returned " + c);
                done.countDown();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } 
}
