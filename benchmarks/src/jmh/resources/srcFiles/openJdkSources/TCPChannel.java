/*
 * Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package sun.rmi.transport.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.Socket;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.WeakHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import sun.rmi.runtime.Log;
import sun.rmi.runtime.NewThreadAction;
import sun.rmi.runtime.RuntimeUtil;
import sun.rmi.transport.Channel;
import sun.rmi.transport.Connection;
import sun.rmi.transport.Endpoint;
import sun.rmi.transport.TransportConstants;

/**
 * TCPChannel is the socket-based implementation of the RMI Channel
 * abstraction.
 *
 * @author Ann Wollrath
 */
public class TCPChannel implements Channel {
    /** endpoint for this channel */
    private final TCPEndpoint ep;
    /** transport for this channel */
    private final TCPTransport tr;
    /** list of cached connections */
    private final List<TCPConnection> freeList =
        new ArrayList<>();
    /** frees cached connections that have expired (guarded by freeList) */
    private Future<?> reaper = null;

    /** connection acceptor (should be in TCPTransport) */
    private ConnectionAcceptor acceptor;

    /** most recently authorized AccessControlContext */
    @SuppressWarnings("removal")
    private AccessControlContext okContext;

    /** cache of authorized AccessControlContexts */
    @SuppressWarnings("removal")
    private WeakHashMap<AccessControlContext,
                        Reference<AccessControlContext>> authcache;

    /** the SecurityManager which authorized okContext and authcache */
    @SuppressWarnings("removal")
    private SecurityManager cacheSecurityManager = null;

    /** client-side connection idle usage timeout */
    @SuppressWarnings("removal")
    private static final long idleTimeout =             
        AccessController.doPrivileged((PrivilegedAction<Long>) () ->
            Long.getLong("sun.rmi.transport.connectionTimeout", 15000));

    /** client-side connection handshake read timeout */
    @SuppressWarnings("removal")
    private static final int handshakeTimeout =         
        AccessController.doPrivileged((PrivilegedAction<Integer>) () ->
            Integer.getInteger("sun.rmi.transport.tcp.handshakeTimeout", 60000));

    /** client-side connection response read timeout (after handshake) */
    @SuppressWarnings("removal")
    private static final int responseTimeout =          
        AccessController.doPrivileged((PrivilegedAction<Integer>) () ->
            Integer.getInteger("sun.rmi.transport.tcp.responseTimeout", 0));

    /** thread pool for scheduling delayed tasks */
    @SuppressWarnings("removal")
    private static final ScheduledExecutorService scheduler =
        AccessController.doPrivileged(
            new RuntimeUtil.GetInstanceAction()).getScheduler();

    /**
     * Create channel for endpoint.
     */
    TCPChannel(TCPTransport tr, TCPEndpoint ep) {
        this.tr = tr;
        this.ep = ep;
    }

    /**
     * Return the endpoint for this channel.
     */
    public Endpoint getEndpoint() {
        return ep;
    }

    /**
     * Checks if the current caller has sufficient privilege to make
     * a connection to the remote endpoint.
     * @exception SecurityException if caller is not allowed to use this
     * Channel.
     */
    @SuppressWarnings("removal")
    private void checkConnectPermission() throws SecurityException {
        SecurityManager security = System.getSecurityManager();
        if (security == null)
            return;

        if (security != cacheSecurityManager) {
            okContext = null;
            authcache = new WeakHashMap<AccessControlContext,
                                        Reference<AccessControlContext>>();
            cacheSecurityManager = security;
        }

        AccessControlContext ctx = AccessController.getContext();

        if (okContext == null ||
            !(okContext.equals(ctx) || authcache.containsKey(ctx)))
        {
            security.checkConnect(ep.getHost(), ep.getPort());
            authcache.put(ctx, new SoftReference<AccessControlContext>(ctx));
        }
        okContext = ctx;
    }

    /**
     * Supplies a connection to the endpoint of the address space
     * for which this is a channel.  The returned connection may
     * be one retrieved from a cache of idle connections.
     */
    public Connection newConnection() throws RemoteException {
        TCPConnection conn;

        do {
            conn = null;
            synchronized (freeList) {
                int elementPos = freeList.size()-1;

                if (elementPos >= 0) {
                    checkConnectPermission();
                    conn = freeList.get(elementPos);
                    freeList.remove(elementPos);
                }
            }


            if (conn != null) {
                if (!conn.isDead()) {
                    TCPTransport.tcpLog.log(Log.BRIEF, "reuse connection");
                    return conn;
                }

                this.free(conn, false);
            }
        } while (conn != null);

        return (createConnection());
    }

    /**
     * Create a new connection to the remote endpoint of this channel.
     * The returned connection is new.  The caller must already have
     * passed a security checkConnect or equivalent.
     */
    private Connection createConnection() throws RemoteException {
        Connection conn;

        TCPTransport.tcpLog.log(Log.BRIEF, "create connection");

        Socket sock = ep.newSocket();
        conn = new TCPConnection(this, sock);

        try {
            /*
             * Set socket read timeout to configured value for JRMP
             * connection handshake; this also serves to guard against
             * non-JRMP servers that do not respond (see 4322806).
             */
            int originalSoTimeout = 0;
            try {
                originalSoTimeout = sock.getSoTimeout();
                sock.setSoTimeout(handshakeTimeout);
            } catch (Exception e) {
            }

            DataOutputStream out =
                new DataOutputStream(conn.getOutputStream());
            writeTransportHeader(out);

            if (!conn.isReusable()) {
                out.writeByte(TransportConstants.SingleOpProtocol);
            } else {
                out.writeByte(TransportConstants.StreamProtocol);
                out.flush();

                DataInputStream in =
                    new DataInputStream(conn.getInputStream());
                byte ack = in.readByte();
                if (ack != TransportConstants.ProtocolAck) {
                    throw new ConnectIOException(
                        ack == TransportConstants.ProtocolNack ?
                        "JRMP StreamProtocol not supported by server" :
                        "non-JRMP server at remote endpoint");
                }

                String suggestedHost = in.readUTF();
                int    suggestedPort = in.readInt();
                if (TCPTransport.tcpLog.isLoggable(Log.VERBOSE)) {
                    TCPTransport.tcpLog.log(Log.VERBOSE,
                        "server suggested " + suggestedHost + ":" +
                        suggestedPort);
                }

                TCPEndpoint.setLocalHost(suggestedHost);

                TCPEndpoint localEp =
                    TCPEndpoint.getLocalEndpoint(0, null, null);
                out.writeUTF(localEp.getHost());
                out.writeInt(localEp.getPort());
                if (TCPTransport.tcpLog.isLoggable(Log.VERBOSE)) {
                    TCPTransport.tcpLog.log(Log.VERBOSE, "using " +
                        localEp.getHost() + ":" + localEp.getPort());
                }

                /*
                 * After JRMP handshake, set socket read timeout to value
                 * configured for the rest of the lifetime of the
                 * connection.  NOTE: this timeout, if configured to a
                 * finite duration, places an upper bound on the time
                 * that a remote method call is permitted to execute.
                 */
                try {
                    /*
                     * If socket factory had set a non-zero timeout on its
                     * own, then restore it instead of using the property-
                     * configured value.
                     */
                    sock.setSoTimeout((originalSoTimeout != 0 ?
                                       originalSoTimeout :
                                       responseTimeout));
                } catch (Exception e) {
                }

                out.flush();
            }
        } catch (IOException e) {
            try {
                conn.close();
            } catch (Exception ex) {}
            if (e instanceof RemoteException) {
                throw (RemoteException) e;
            } else {
                throw new ConnectIOException(
                    "error during JRMP connection establishment", e);
            }
        }
        return conn;
    }

    /**
     * Free the connection generated by this channel.
     * @param conn The connection
     * @param reuse If true, the connection is in a state in which it
     *        can be reused for another method call.
     */
    public void free(Connection conn, boolean reuse) {
        if (conn == null) return;

        if (reuse && conn.isReusable()) {
            long lastuse = System.currentTimeMillis();
            TCPConnection tcpConnection = (TCPConnection) conn;

            TCPTransport.tcpLog.log(Log.BRIEF, "reuse connection");

            /*
             * Cache connection; if reaper task for expired
             * connections isn't scheduled, then schedule it.
             */
            synchronized (freeList) {
                freeList.add(tcpConnection);
                if (reaper == null) {
                    TCPTransport.tcpLog.log(Log.BRIEF, "create reaper");

                    reaper = scheduler.scheduleWithFixedDelay(
                        new Runnable() {
                            public void run() {
                                TCPTransport.tcpLog.log(Log.VERBOSE,
                                                        "wake up");
                                freeCachedConnections();
                            }
                        }, idleTimeout, idleTimeout, TimeUnit.MILLISECONDS);
                }
            }

            tcpConnection.setLastUseTime(lastuse);
            tcpConnection.setExpiration(lastuse + idleTimeout);
        } else {
            TCPTransport.tcpLog.log(Log.BRIEF, "close connection");

            try {
                conn.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Send transport header over stream.
     */
    private void writeTransportHeader(DataOutputStream out)
        throws RemoteException
    {
        try {
            DataOutputStream dataOut =
                new DataOutputStream(out);
            dataOut.writeInt(TransportConstants.Magic);
            dataOut.writeShort(TransportConstants.Version);
        } catch (IOException e) {
            throw new ConnectIOException(
                "error writing JRMP transport header", e);
        }
    }

    /**
     * Closes all the connections in the cache, whether timed out or not.
     */
    public void shedCache() {
        Connection[] conn;
        synchronized (freeList) {
            conn = freeList.toArray(new Connection[freeList.size()]);
            freeList.clear();
        }

        for (int i = conn.length; --i >= 0; ) {
            Connection c = conn[i];
            conn[i] = null; 
            try {
                c.close();
            } catch (java.io.IOException e) {
            }
        }
    }

    private void freeCachedConnections() {
        /*
         * Remove each connection whose time out has expired.
         */
        synchronized (freeList) {
            int size = freeList.size();

            if (size > 0) {
                long time = System.currentTimeMillis();
                ListIterator<TCPConnection> iter = freeList.listIterator(size);

                while (iter.hasPrevious()) {
                    TCPConnection conn = iter.previous();
                    if (conn.expired(time)) {
                        TCPTransport.tcpLog.log(Log.VERBOSE,
                            "connection timeout expired");

                        try {
                            conn.close();
                        } catch (java.io.IOException e) {
                        }
                        iter.remove();
                    }
                }
            }

            if (freeList.isEmpty()) {
                reaper.cancel(false);
                reaper = null;
            }
        }
    }
}

/**
 * ConnectionAcceptor manages accepting new connections and giving them
 * to TCPTransport's message handler on new threads.
 *
 * Since this object only needs to know which transport to give new
 * connections to, it doesn't need to be per-channel as currently
 * implemented.
 */
class ConnectionAcceptor implements Runnable {

    /** transport that will handle message on accepted connections */
    private TCPTransport transport;

    /** queue of connections to be accepted */
    private List<Connection> queue = new ArrayList<>();

    /** thread ID counter */
    private static int threadNum = 0;

    /**
     * Create a new ConnectionAcceptor that will give connections
     * to the specified transport on a new thread.
     */
    public ConnectionAcceptor(TCPTransport transport) {
        this.transport = transport;
    }

    /**
     * Start a new thread to accept connections.
     */
    public void startNewAcceptor() {
        @SuppressWarnings("removal")
        Thread t = AccessController.doPrivileged(
            new NewThreadAction(ConnectionAcceptor.this,
                                "TCPChannel Accept-" + ++ threadNum,
                                true));
        t.start();
    }

    /**
     * Add connection to queue of connections to be accepted.
     */
    public void accept(Connection conn) {
        synchronized (queue) {
            queue.add(conn);
            queue.notify();
        }
    }

    /**
     * Give transport next accepted connection, when available.
     */
    public void run() {
        Connection conn;

        synchronized (queue) {
            while (queue.size() == 0) {
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                }
            }
            startNewAcceptor();
            conn = queue.remove(0);
        }

        transport.handleMessages(conn, true);
    }
}
