/*
 * Copyright (c) 2008, 2023, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.net.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import jdk.internal.misc.Unsafe;
import sun.net.util.SocketExceptions;

/**
 * Windows implementation of AsynchronousSocketChannel using overlapped I/O.
 */

class WindowsAsynchronousSocketChannelImpl
    extends AsynchronousSocketChannelImpl implements Iocp.OverlappedChannel
{
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    private static int dependsArch(int value32, int value64) {
        return (unsafe.addressSize() == 4) ? value32 : value64;
    }

    /*
     * typedef struct _WSABUF {
     *     u_long      len;
     *     char FAR *  buf;
     * } WSABUF;
     */
    private static final int SIZEOF_WSABUF  = dependsArch(8, 16);
    private static final int OFFSETOF_LEN   = 0;
    private static final int OFFSETOF_BUF   = dependsArch(4, 8);

    private static final int MAX_WSABUF     = 16;

    private static final int SIZEOF_WSABUFARRAY = MAX_WSABUF * SIZEOF_WSABUF;


    final long handle;

    private final Iocp iocp;

    private final int completionKey;

    private final PendingIoCache ioCache;

    private final long readBufferArray;
    private final long writeBufferArray;


    WindowsAsynchronousSocketChannelImpl(Iocp iocp, boolean failIfGroupShutdown)
        throws IOException
    {
        super(iocp);

        long h = IOUtil.fdVal(fd);
        int key = 0;
        try {
            key = iocp.associate(this, h);
        } catch (ShutdownChannelGroupException x) {
            if (failIfGroupShutdown) {
                closesocket0(h);
                throw x;
            }
        } catch (IOException x) {
            closesocket0(h);
            throw x;
        }

        this.handle = h;
        this.iocp = iocp;
        this.completionKey = key;
        this.ioCache = new PendingIoCache();

        this.readBufferArray = unsafe.allocateMemory(SIZEOF_WSABUFARRAY);
        this.writeBufferArray = unsafe.allocateMemory(SIZEOF_WSABUFARRAY);
    }

    WindowsAsynchronousSocketChannelImpl(Iocp iocp) throws IOException {
        this(iocp, true);
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return iocp;
    }

    /**
     * Invoked by Iocp when an I/O operation competes.
     */
    @Override
    public <V,A> PendingFuture<V,A> getByOverlapped(long overlapped) {
        return ioCache.remove(overlapped);
    }

    long handle() {
        return handle;
    }

    void setConnected(InetSocketAddress localAddress,
                      InetSocketAddress remoteAddress)
    {
        synchronized (stateLock) {
            state = ST_CONNECTED;
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
        }
    }

    @Override
    void implClose() throws IOException {
        closesocket0(handle);

        ioCache.close();

        unsafe.freeMemory(readBufferArray);
        unsafe.freeMemory(writeBufferArray);

        if (completionKey != 0)
            iocp.disassociate(completionKey);
    }

    @Override
    public void onCancel(PendingFuture<?,?> task) {
        if (task.getContext() instanceof ConnectTask)
            killConnect();
        if (task.getContext() instanceof ReadTask)
            killReading();
        if (task.getContext() instanceof WriteTask)
            killWriting();
    }

    /**
     * Implements the task to initiate a connection and the handler to
     * consume the result when the connection is established (or fails).
     */
    private class ConnectTask<A> implements Runnable, Iocp.ResultHandler {
        private final InetSocketAddress remote;
        private final PendingFuture<Void,A> result;

        ConnectTask(InetSocketAddress remote, PendingFuture<Void,A> result) {
            this.remote = remote;
            this.result = result;
        }

        private void closeChannel() {
            try {
                close();
            } catch (IOException ignore) { }
        }

        private IOException toIOException(Throwable x) {
            if (x instanceof IOException) {
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                return (IOException)x;
            }
            return new IOException(x);
        }

        /**
         * Invoke after a connection is successfully established.
         */
        private void afterConnect() throws IOException {
            updateConnectContext(handle);
            synchronized (stateLock) {
                state = ST_CONNECTED;
                remoteAddress = remote;
            }
        }

        /**
         * Task to initiate a connection.
         */
        @Override
        public void run() {
            long overlapped = 0L;
            Throwable exc = null;
            try {
                begin();

                synchronized (result) {
                    overlapped = ioCache.add(result);
                    int n = connect0(handle, Net.isIPv6Available(), remote.getAddress(),
                                     remote.getPort(), overlapped);
                    if (n == IOStatus.UNAVAILABLE) {
                        return;
                    }

                    afterConnect();
                    result.setResult(null);
                }
            } catch (Throwable x) {
                if (overlapped != 0L)
                    ioCache.remove(overlapped);
                exc = x;
            } finally {
                end();
            }

            if (exc != null) {
                closeChannel();
                exc = SocketExceptions.of(toIOException(exc), remote);
                result.setFailure(exc);
            }
            Invoker.invoke(result);
        }

        /**
         * Invoked by handler thread when connection established.
         */
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            Throwable exc = null;
            try {
                begin();
                afterConnect();
                result.setResult(null);
            } catch (Throwable x) {
                exc = x;
            } finally {
                end();
            }

            if (exc != null) {
                closeChannel();
                IOException ee = toIOException(exc);
                ee = SocketExceptions.of(ee, remote);
                result.setFailure(ee);
            }

            if (canInvokeDirect) {
                Invoker.invokeUnchecked(result);
            } else {
                Invoker.invoke(result);
            }
        }

        /**
         * Invoked by handler thread when failed to establish connection.
         */
        @Override
        public void failed(int error, IOException x) {
            x = SocketExceptions.of(x, remote);
            if (isOpen()) {
                closeChannel();
                result.setFailure(x);
            } else {
                x = SocketExceptions.of(new AsynchronousCloseException(), remote);
                result.setFailure(x);
            }
            Invoker.invoke(result);
        }
    }

    @SuppressWarnings("removal")
    private void doPrivilegedBind(final SocketAddress sa) throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    bind(sa);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

    @Override
    <A> Future<Void> implConnect(SocketAddress remote,
                                 A attachment,
                                 CompletionHandler<Void,? super A> handler)
    {
        if (!isOpen()) {
            Throwable exc = new ClosedChannelException();
            if (handler == null)
                return CompletedFuture.withFailure(exc);
            Invoker.invoke(this, handler, attachment, null, exc);
            return null;
        }

        InetSocketAddress isa = Net.checkAddress(remote);

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());

        IOException bindException = null;
        synchronized (stateLock) {
            if (state == ST_CONNECTED)
                throw new AlreadyConnectedException();
            if (state == ST_PENDING)
                throw new ConnectionPendingException();
            if (localAddress == null) {
                try {
                    SocketAddress any = new InetSocketAddress(0);
                    if (sm == null) {
                        bind(any);
                    } else {
                        doPrivilegedBind(any);
                    }
                } catch (IOException x) {
                    bindException = x;
                }
            }
            if (bindException == null)
                state = ST_PENDING;
        }

        if (bindException != null) {
            try {
                close();
            } catch (IOException ignore) { }
            if (handler == null)
                return CompletedFuture.withFailure(bindException);
            Invoker.invoke(this, handler, attachment, null, bindException);
            return null;
        }

        PendingFuture<Void,A> result =
            new PendingFuture<Void,A>(this, handler, attachment);
        ConnectTask<A> task = new ConnectTask<A>(isa, result);
        result.setContext(task);

        task.run();
        return result;
    }

    /**
     * Implements the task to initiate a read and the handler to consume the
     * result when the read completes.
     */
    private class ReadTask<V,A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer[] bufs;
        private final int numBufs;
        private final boolean scatteringRead;
        private final PendingFuture<V,A> result;

        private ByteBuffer[] shadow;
        private Runnable scopeHandleReleasers;

        ReadTask(ByteBuffer[] bufs,
                 boolean scatteringRead,
                 PendingFuture<V,A> result)
        {
            this.bufs = bufs;
            this.numBufs = (bufs.length > MAX_WSABUF) ? MAX_WSABUF : bufs.length;
            this.scatteringRead = scatteringRead;
            this.result = result;
        }

        /**
         * Invoked prior to read to prepare the WSABUF array. Where necessary,
         * it substitutes non-direct buffers with direct buffers.
         */
        void prepareBuffers() {
            scopeHandleReleasers = IOUtil.acquireScopes(bufs);
            shadow = new ByteBuffer[numBufs];
            long address = readBufferArray;
            for (int i=0; i<numBufs; i++) {
                ByteBuffer dst = bufs[i];
                int pos = dst.position();
                int lim = dst.limit();
                assert (pos <= lim);
                int rem = (pos <= lim ? lim - pos : 0);
                long a;
                if (!(dst instanceof DirectBuffer)) {
                    ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
                    shadow[i] = bb;
                    a = IOUtil.bufferAddress(bb);
                } else {
                    shadow[i] = dst;
                    a = IOUtil.bufferAddress(dst) + pos;
                }
                unsafe.putAddress(address + OFFSETOF_BUF, a);
                unsafe.putInt(address + OFFSETOF_LEN, rem);
                address += SIZEOF_WSABUF;
            }
        }

        /**
         * Invoked after a read has completed to update the buffer positions
         * and release any substituted buffers.
         */
        void updateBuffers(int bytesRead) {
            for (int i=0; i<numBufs; i++) {
                ByteBuffer nextBuffer = shadow[i];
                int pos = nextBuffer.position();
                int len = nextBuffer.remaining();
                if (bytesRead >= len) {
                    bytesRead -= len;
                    int newPosition = pos + len;
                    try {
                        nextBuffer.position(newPosition);
                    } catch (IllegalArgumentException x) {
                    }
                } else { 
                    if (bytesRead > 0) {
                        assert(pos + bytesRead < (long)Integer.MAX_VALUE);
                        int newPosition = pos + bytesRead;
                        try {
                            nextBuffer.position(newPosition);
                        } catch (IllegalArgumentException x) {
                        }
                    }
                    break;
                }
            }

            for (int i=0; i<numBufs; i++) {
                if (!(bufs[i] instanceof DirectBuffer)) {
                    shadow[i].flip();
                    try {
                        bufs[i].put(shadow[i]);
                    } catch (BufferOverflowException x) {
                    }
                }
            }
        }

        void releaseBuffers() {
            for (int i=0; i<numBufs; i++) {
                if (!(bufs[i] instanceof DirectBuffer)) {
                    Util.releaseTemporaryDirectBuffer(shadow[i]);
                }
            }
            IOUtil.releaseScopes(scopeHandleReleasers);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            long overlapped = 0L;
            boolean prepared = false;
            boolean pending = false;

            try {
                begin();

                prepareBuffers();
                prepared = true;

                overlapped = ioCache.add(result);

                int n = read0(handle, numBufs, readBufferArray, overlapped);
                if (n == IOStatus.UNAVAILABLE) {
                    pending = true;
                    return;
                }
                if (n == IOStatus.EOF) {
                    enableReading();
                    if (scatteringRead) {
                        result.setResult((V)Long.valueOf(-1L));
                    } else {
                        result.setResult((V)Integer.valueOf(-1));
                    }
                } else {
                    throw new InternalError("Read completed immediately");
                }
            } catch (Throwable x) {
                enableReading();
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException))
                    x = new IOException(x);
                result.setFailure(x);
            } finally {
                if (!pending) {
                    if (overlapped != 0L)
                        ioCache.remove(overlapped);
                    if (prepared)
                        releaseBuffers();
                }
                end();
            }

            Invoker.invoke(result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        @SuppressWarnings("unchecked")
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            if (bytesTransferred == 0) {
                bytesTransferred = -1;  
            } else {
                updateBuffers(bytesTransferred);
            }

            releaseBuffers();

            synchronized (result) {
                if (result.isDone())
                    return;
                enableReading();
                if (scatteringRead) {
                    result.setResult((V)Long.valueOf(bytesTransferred));
                } else {
                    result.setResult((V)Integer.valueOf(bytesTransferred));
                }
            }
            if (canInvokeDirect) {
                Invoker.invokeUnchecked(result);
            } else {
                Invoker.invoke(result);
            }
        }

        @Override
        public void failed(int error, IOException x) {
            releaseBuffers();

            if (!isOpen())
                x = new AsynchronousCloseException();

            synchronized (result) {
                if (result.isDone())
                    return;
                enableReading();
                result.setFailure(x);
            }
            Invoker.invoke(result);
        }

        /**
         * Invoked if timeout expires before it is cancelled
         */
        void timeout() {
            synchronized (result) {
                if (result.isDone())
                    return;

                enableReading(true);
                result.setFailure(new InterruptedByTimeoutException());
            }

            Invoker.invoke(result);
        }
    }

    @Override
    <V extends Number,A> Future<V> implRead(boolean isScatteringRead,
                                            ByteBuffer dst,
                                            ByteBuffer[] dsts,
                                            long timeout,
                                            TimeUnit unit,
                                            A attachment,
                                            CompletionHandler<V,? super A> handler)
    {
        PendingFuture<V,A> result =
            new PendingFuture<V,A>(this, handler, attachment);
        ByteBuffer[] bufs;
        if (isScatteringRead) {
            bufs = dsts;
        } else {
            bufs = new ByteBuffer[1];
            bufs[0] = dst;
        }
        final ReadTask<V,A> readTask =
                new ReadTask<V,A>(bufs, isScatteringRead, result);
        result.setContext(readTask);

        if (timeout > 0L) {
            Future<?> timeoutTask = iocp.schedule(new Runnable() {
                public void run() {
                    readTask.timeout();
                }
            }, timeout, unit);
            result.setTimeoutTask(timeoutTask);
        }

        readTask.run();
        return result;
    }

    /**
     * Implements the task to initiate a write and the handler to consume the
     * result when the write completes.
     */
    private class WriteTask<V,A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer[] bufs;
        private final int numBufs;
        private final boolean gatheringWrite;
        private final PendingFuture<V,A> result;

        private ByteBuffer[] shadow;
        private Runnable scopeHandleReleasers;

        WriteTask(ByteBuffer[] bufs,
                  boolean gatheringWrite,
                  PendingFuture<V,A> result)
        {
            this.bufs = bufs;
            this.numBufs = (bufs.length > MAX_WSABUF) ? MAX_WSABUF : bufs.length;
            this.gatheringWrite = gatheringWrite;
            this.result = result;
        }

        /**
         * Invoked prior to write to prepare the WSABUF array. Where necessary,
         * it substitutes non-direct buffers with direct buffers.
         */
        void prepareBuffers() {
            scopeHandleReleasers = IOUtil.acquireScopes(bufs);
            shadow = new ByteBuffer[numBufs];
            long address = writeBufferArray;
            for (int i=0; i<numBufs; i++) {
                ByteBuffer src = bufs[i];
                int pos = src.position();
                int lim = src.limit();
                assert (pos <= lim);
                int rem = (pos <= lim ? lim - pos : 0);
                long a;
                if (!(src instanceof DirectBuffer)) {
                    ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
                    bb.put(src);
                    bb.flip();
                    src.position(pos);  
                    shadow[i] = bb;
                    a = IOUtil.bufferAddress(bb);
                } else {
                    shadow[i] = src;
                    a = IOUtil.bufferAddress(src) + pos;
                }
                unsafe.putAddress(address + OFFSETOF_BUF, a);
                unsafe.putInt(address + OFFSETOF_LEN, rem);
                address += SIZEOF_WSABUF;
            }
        }

        /**
         * Invoked after a write has completed to update the buffer positions
         * and release any substituted buffers.
         */
        void updateBuffers(int bytesWritten) {
            for (int i=0; i<numBufs; i++) {
                ByteBuffer nextBuffer = bufs[i];
                int pos = nextBuffer.position();
                int lim = nextBuffer.limit();
                int len = (pos <= lim ? lim - pos : lim);
                if (bytesWritten >= len) {
                    bytesWritten -= len;
                    int newPosition = pos + len;
                    try {
                        nextBuffer.position(newPosition);
                    } catch (IllegalArgumentException x) {
                    }
                } else { 
                    if (bytesWritten > 0) {
                        assert(pos + bytesWritten < (long)Integer.MAX_VALUE);
                        int newPosition = pos + bytesWritten;
                        try {
                            nextBuffer.position(newPosition);
                        } catch (IllegalArgumentException x) {
                        }
                    }
                    break;
                }
            }
        }

        void releaseBuffers() {
            for (int i=0; i<numBufs; i++) {
                if (!(bufs[i] instanceof DirectBuffer)) {
                    Util.releaseTemporaryDirectBuffer(shadow[i]);
                }
            }
            IOUtil.releaseScopes(scopeHandleReleasers);
        }

        @Override
        public void run() {
            long overlapped = 0L;
            boolean prepared = false;
            boolean pending = false;
            boolean shutdown = false;

            try {
                begin();

                prepareBuffers();
                prepared = true;

                overlapped = ioCache.add(result);
                int n = write0(handle, numBufs, writeBufferArray, overlapped);
                if (n == IOStatus.UNAVAILABLE) {
                    pending = true;
                    return;
                }
                if (n == IOStatus.EOF) {
                    shutdown = true;
                    throw new ClosedChannelException();
                }
                throw new InternalError("Write completed immediately");
            } catch (Throwable x) {
                enableWriting();
                if (!shutdown && (x instanceof ClosedChannelException))
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException))
                    x = new IOException(x);
                result.setFailure(x);
            } finally {
                if (!pending) {
                    if (overlapped != 0L)
                        ioCache.remove(overlapped);
                    if (prepared)
                        releaseBuffers();
                }
                end();
            }

            Invoker.invoke(result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        @SuppressWarnings("unchecked")
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            updateBuffers(bytesTransferred);

            releaseBuffers();

            synchronized (result) {
                if (result.isDone())
                    return;
                enableWriting();
                if (gatheringWrite) {
                    result.setResult((V)Long.valueOf(bytesTransferred));
                } else {
                    result.setResult((V)Integer.valueOf(bytesTransferred));
                }
            }
            if (canInvokeDirect) {
                Invoker.invokeUnchecked(result);
            } else {
                Invoker.invoke(result);
            }
        }

        @Override
        public void failed(int error, IOException x) {
            releaseBuffers();

            if (!isOpen())
                x = new AsynchronousCloseException();

            synchronized (result) {
                if (result.isDone())
                    return;
                enableWriting();
                result.setFailure(x);
            }
            Invoker.invoke(result);
        }

        /**
         * Invoked if timeout expires before it is cancelled
         */
        void timeout() {
            synchronized (result) {
                if (result.isDone())
                    return;

                enableWriting(true);
                result.setFailure(new InterruptedByTimeoutException());
            }

            Invoker.invoke(result);
        }
    }

    @Override
    <V extends Number,A> Future<V> implWrite(boolean gatheringWrite,
                                             ByteBuffer src,
                                             ByteBuffer[] srcs,
                                             long timeout,
                                             TimeUnit unit,
                                             A attachment,
                                             CompletionHandler<V,? super A> handler)
    {
        PendingFuture<V,A> result =
            new PendingFuture<V,A>(this, handler, attachment);
        ByteBuffer[] bufs;
        if (gatheringWrite) {
            bufs = srcs;
        } else {
            bufs = new ByteBuffer[1];
            bufs[0] = src;
        }
        final WriteTask<V,A> writeTask =
                new WriteTask<V,A>(bufs, gatheringWrite, result);
        result.setContext(writeTask);

        if (timeout > 0L) {
            Future<?> timeoutTask = iocp.schedule(new Runnable() {
                public void run() {
                    writeTask.timeout();
                }
            }, timeout, unit);
            result.setTimeoutTask(timeoutTask);
        }

        writeTask.run();
        return result;
    }


    private static native void initIDs();

    private static native int connect0(long socket, boolean preferIPv6,
        InetAddress remote, int remotePort, long overlapped) throws IOException;

    private static native void updateConnectContext(long socket) throws IOException;

    private static native int read0(long socket, int count, long address, long overlapped)
        throws IOException;

    private static native int write0(long socket, int count, long address,
        long overlapped) throws IOException;

    private static native void shutdown0(long socket, int how) throws IOException;

    private static native void closesocket0(long socket) throws IOException;

    static {
        IOUtil.load();
        initIDs();
    }
}
