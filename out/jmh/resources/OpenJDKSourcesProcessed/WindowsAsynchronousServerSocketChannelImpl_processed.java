/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import jdk.internal.misc.Unsafe;

/**
 * Windows implementation of AsynchronousServerSocketChannel using overlapped I/O.
 */

class WindowsAsynchronousServerSocketChannelImpl
    extends AsynchronousServerSocketChannelImpl implements Iocp.OverlappedChannel
{
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    private static final int DATA_BUFFER_SIZE = 88;

    private final long handle;
    private final int completionKey;
    private final Iocp iocp;

    private final PendingIoCache ioCache;

    private final long dataBuffer;

    private AtomicBoolean accepting = new AtomicBoolean();


    WindowsAsynchronousServerSocketChannelImpl(Iocp iocp) throws IOException {
        super(iocp);

        long h = IOUtil.fdVal(fd);
        int key;
        try {
            key = iocp.associate(this, h);
        } catch (IOException x) {
            closesocket0(h);   
            throw x;
        }

        this.handle = h;
        this.completionKey = key;
        this.iocp = iocp;
        this.ioCache = new PendingIoCache();
        this.dataBuffer = unsafe.allocateMemory(DATA_BUFFER_SIZE);
    }

    @Override
    public <V,A> PendingFuture<V,A> getByOverlapped(long overlapped) {
        return ioCache.remove(overlapped);
    }

    @Override
    void implClose() throws IOException {
        closesocket0(handle);

        ioCache.close();

        iocp.disassociate(completionKey);

        unsafe.freeMemory(dataBuffer);
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return iocp;
    }

    /**
     * Task to initiate accept operation and to handle result.
     */
    private class AcceptTask implements Runnable, Iocp.ResultHandler {
        private final WindowsAsynchronousSocketChannelImpl channel;
        @SuppressWarnings("removal")
        private final AccessControlContext acc;
        private final PendingFuture<AsynchronousSocketChannel,Object> result;

        AcceptTask(WindowsAsynchronousSocketChannelImpl channel,
                   @SuppressWarnings("removal") AccessControlContext acc,
                   PendingFuture<AsynchronousSocketChannel,Object> result)
        {
            this.channel = channel;
            this.acc = acc;
            this.result = result;
        }

        void enableAccept() {
            accepting.set(false);
        }

        void closeChildChannel() {
            try {
                channel.close();
            } catch (IOException ignore) { }
        }

        @SuppressWarnings("removal")
        void finishAccept() throws IOException {
            /**
             * Set local/remote addresses. This is currently very inefficient
             * in that it requires 2 calls to getsockname and 2 calls to getpeername.
             * (should change this to use GetAcceptExSockaddrs)
             */
            updateAcceptContext(handle, channel.handle());

            InetSocketAddress local = Net.localAddress(channel.fd);
            final InetSocketAddress remote = Net.remoteAddress(channel.fd);
            channel.setConnected(local, remote);

            if (acc != null) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        SecurityManager sm = System.getSecurityManager();
                        sm.checkAccept(remote.getAddress().getHostAddress(),
                                       remote.getPort());
                        return null;
                    }
                }, acc);
            }
        }

        /**
         * Initiates the accept operation.
         */
        @Override
        public void run() {
            long overlapped = 0L;

            try {
                begin();
                try {
                    channel.begin();

                    synchronized (result) {
                        overlapped = ioCache.add(result);

                        int n = accept0(handle, channel.handle(), overlapped, dataBuffer);
                        if (n == IOStatus.UNAVAILABLE) {
                            return;
                        }

                        finishAccept();

                        enableAccept();
                        result.setResult(channel);
                    }
                } finally {
                    channel.end();
                }
            } catch (Throwable x) {
                if (overlapped != 0L)
                    ioCache.remove(overlapped);
                closeChildChannel();
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException) && !(x instanceof SecurityException))
                    x = new IOException(x);
                enableAccept();
                result.setFailure(x);
            } finally {
                end();
            }

            if (result.isCancelled()) {
                closeChildChannel();
            }

            Invoker.invokeIndirectly(result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            try {
                if (iocp.isShutdown()) {
                    throw new IOException(new ShutdownChannelGroupException());
                }

                try {
                    begin();
                    try {
                        channel.begin();
                        finishAccept();
                    } finally {
                        channel.end();
                    }
                } finally {
                    end();
                }

                enableAccept();
                result.setResult(channel);
            } catch (Throwable x) {
                enableAccept();
                closeChildChannel();
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException) && !(x instanceof SecurityException))
                    x = new IOException(x);
                result.setFailure(x);
            }

            if (result.isCancelled()) {
                closeChildChannel();
            }

            Invoker.invokeIndirectly(result);
        }

        @Override
        public void failed(int error, IOException x) {
            enableAccept();
            closeChildChannel();

            if (isOpen()) {
                result.setFailure(x);
            } else {
                result.setFailure(new AsynchronousCloseException());
            }
            Invoker.invokeIndirectly(result);
        }
    }

    @Override
    Future<AsynchronousSocketChannel> implAccept(Object attachment,
        final CompletionHandler<AsynchronousSocketChannel,Object> handler)
    {
        if (!isOpen()) {
            Throwable exc = new ClosedChannelException();
            if (handler == null)
                return CompletedFuture.withFailure(exc);
            Invoker.invokeIndirectly(this, handler, attachment, null, exc);
            return null;
        }
        if (isAcceptKilled())
            throw new RuntimeException("Accept not allowed due to cancellation");

        if (localAddress == null)
            throw new NotYetBoundException();

        WindowsAsynchronousSocketChannelImpl ch = null;
        IOException ioe = null;
        try {
            begin();
            ch = new WindowsAsynchronousSocketChannelImpl(iocp, false);
        } catch (IOException x) {
            ioe = x;
        } finally {
            end();
        }
        if (ioe != null) {
            if (handler == null)
                return CompletedFuture.withFailure(ioe);
            Invoker.invokeIndirectly(this, handler, attachment, null, ioe);
            return null;
        }

        @SuppressWarnings("removal")
        AccessControlContext acc = (System.getSecurityManager() == null) ?
            null : AccessController.getContext();

        PendingFuture<AsynchronousSocketChannel,Object> result =
            new PendingFuture<AsynchronousSocketChannel,Object>(this, handler, attachment);
        AcceptTask task = new AcceptTask(ch, acc, result);
        result.setContext(task);

        if (!accepting.compareAndSet(false, true))
            throw new AcceptPendingException();

        task.run();
        return result;
    }


    private static native void initIDs();

    private static native int accept0(long listenSocket, long acceptSocket,
        long overlapped, long dataBuffer) throws IOException;

    private static native void updateAcceptContext(long listenSocket,
        long acceptSocket) throws IOException;

    private static native void closesocket0(long socket) throws IOException;

    static {
        IOUtil.load();
        initIDs();
    }
}
