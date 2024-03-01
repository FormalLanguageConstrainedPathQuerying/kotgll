/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.common;

import jdk.internal.net.http.common.SubscriberWrapper.SchedulingAction;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;

/**
 * Implements SSL using two {@link SubscriberWrapper}s.
 *
 * <p> Constructor takes two {@linkplain Flow.Subscriber subscribers} - {@code downReader}
 * and {@code downWriter}. {@code downReader} receives the application data (after it has
 * been decrypted by SSLFlowDelegate). {@code downWriter} receives the network data (after it has
 * been encrypted by SSLFlowDelegate).
 *
 * <p> Method {@link #upstreamWriter()} returns a {@linkplain Subscriber subscriber} which should
 * be subscribed with a {@linkplain Flow.Publisher publisher} which publishes application data
 * that can then be encrypted into network data by this SSLFlowDelegate and handed off to the
 * {@code downWriter}.
 *
 * <p> Method {@link #upstreamReader()} returns a {@link Subscriber subscriber} which should be
 * subscribed with a {@linkplain Flow.Publisher publisher} which publishes encrypted network data
 * that can then be decrypted into application data by this SSLFlowDelegate and handed off to the
 * {@code downReader}.
 *
 * <p> Errors are reported to the {@code downReader} subscriber.
 *
 * <p> The diagram below illustrates how the Flow.Subscribers are used in this class, and where
 * they come from:
 * <pre>
 * {@code
 *
 *
 *
 * --------->  data flow direction
 *
 *
 *                  |                                   ^
 *  upstreamWriter  |                                   | downReader
 *  obtained from   |                                   | supplied to
 * upstreamWriter() |                                   | constructor
 *                  v                                   |
 *      +-----------------------------------------------------------+
 *      *                                            decrypts       *
 *      *                       SSLFlowDelegate                     *
 *      *        encrypts                                           *
 *      +-----------------------------------------------------------+
 *                  |                                   ^
 *    downWriter    |                                   | upstreamReader
 *    supplied to   |                                   | obtained from
 *    constructor   |                                   | upstreamReader()
 *                  v                                   |
 *
 * }
 * </pre>
 */
public class SSLFlowDelegate {

    final Logger debug =
            Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

    private static final ByteBuffer SENTINEL = Utils.EMPTY_BYTEBUFFER;
    private static final ByteBuffer HS_TRIGGER = ByteBuffer.allocate(0);
    private static final ByteBuffer NOTHING = ByteBuffer.allocate(0);
    private static final String monProp = Utils.getProperty("jdk.internal.httpclient.monitorFlowDelegate");
    private static final boolean isMonitored =
            monProp != null && (monProp.isEmpty() || monProp.equalsIgnoreCase("true"));

    final Executor exec;
    final Reader reader;
    final Writer writer;
    final SSLEngine engine;
    final String tubeName; 
    final CompletableFuture<String> alpnCF; 
    final Monitorable monitor = isMonitored ? this::monitor : null; 
    volatile boolean close_notify_received;
    final CompletableFuture<Void> readerCF;
    final CompletableFuture<Void> writerCF;
    final CompletableFuture<Void> stopCF;
    final Consumer<ByteBuffer> recycler;
    static AtomicInteger scount = new AtomicInteger(1);
    final int id;

    /**
     * Creates an SSLFlowDelegate fed from two Flow.Subscribers. Each
     * Flow.Subscriber requires an associated {@link CompletableFuture}
     * for errors that need to be signaled from downstream to upstream.
     */
    public SSLFlowDelegate(SSLEngine engine,
                           Executor exec,
                           Subscriber<? super List<ByteBuffer>> downReader,
                           Subscriber<? super List<ByteBuffer>> downWriter)
    {
        this(engine, exec, null, downReader, downWriter);
    }

    /**
     * Creates an SSLFlowDelegate fed from two Flow.Subscribers. Each
     * Flow.Subscriber requires an associated {@link CompletableFuture}
     * for errors that need to be signaled from downstream to upstream.
     */
    public SSLFlowDelegate(SSLEngine engine,
            Executor exec,
            Consumer<ByteBuffer> recycler,
            Subscriber<? super List<ByteBuffer>> downReader,
            Subscriber<? super List<ByteBuffer>> downWriter)
        {
        this.id = scount.getAndIncrement();
        this.tubeName = String.valueOf(downWriter);
        this.recycler = recycler;
        this.reader = new Reader();
        this.writer = new Writer();
        this.engine = engine;
        this.exec = exec;
        this.handshakeState = new AtomicInteger(NOT_HANDSHAKING);
        this.readerCF = reader.completion();
        this.writerCF = reader.completion();
        readerCF.exceptionally(this::stopOnError);
        writerCF.exceptionally(this::stopOnError);
        this.stopCF = CompletableFuture.allOf(reader.completion(), writer.completion())
            .thenRun(this::normalStop);
        this.alpnCF = new MinimalFuture<>();

        connect(downReader, downWriter);

        if (isMonitored) Monitor.add(monitor);
    }

    /**
     * Returns true if the SSLFlowDelegate has detected a TLS
     * close_notify from the server.
     * @return true, if a close_notify was detected.
     */
    public boolean closeNotifyReceived() {
        return close_notify_received;
    }

    /**
     * Connects the read sink (downReader) to the SSLFlowDelegate Reader,
     * and the write sink (downWriter) to the SSLFlowDelegate Writer.
     * Called from within the constructor. Overwritten by SSLTube.
     *
     * @param downReader  The left hand side read sink (typically, the
     *                    HttpConnection read subscriber).
     * @param downWriter  The right hand side write sink (typically
     *                    the SocketTube write subscriber).
     */
    void connect(Subscriber<? super List<ByteBuffer>> downReader,
                 Subscriber<? super List<ByteBuffer>> downWriter) {
        this.reader.subscribe(downReader);
        this.writer.subscribe(downWriter);
    }

   /**
    * Returns a CompletableFuture<String> which completes after
    * the initial handshake completes, and which contains the negotiated
    * alpn.
    */
    public CompletableFuture<String> alpn() {
        return alpnCF;
    }

    private void setALPN() {
        if (alpnCF.isDone())
            return;
        String alpn = engine.getApplicationProtocol();
        if (debug.on()) debug.log("setALPN = %s", alpn);
        alpnCF.complete(alpn);
    }

    public String monitor() {
        StringBuilder sb = new StringBuilder();
        sb.append("SSL: id ").append(id);
        sb.append(" ").append(dbgString());
        sb.append(" HS state: " + states(handshakeState));
        sb.append(" Engine state: " + engine.getHandshakeStatus().toString());
        if (stateList != null) {
            sb.append(" LL : ");
            for (String s : stateList) {
                sb.append(s).append(" ");
            }
        }
        sb.append("\r\n");
        sb.append("Reader:: ").append(reader.toString());
        sb.append("\r\n");
        sb.append("Writer:: ").append(writer.toString());
        sb.append("\r\n===================================");
        return sb.toString();
    }

    protected SchedulingAction enterReadScheduling() {
        return SchedulingAction.CONTINUE;
    }

    protected Throwable checkForHandshake(Throwable t) {
        return t;
    }


    /**
     * Processing function for incoming data. Pass it thru SSLEngine.unwrap().
     * Any decrypted buffers returned to be passed downstream.
     * Status codes:
     *     NEED_UNWRAP: do nothing. Following incoming data will contain
     *                  any required handshake data
     *     NEED_WRAP: call writer.addData() with empty buffer
     *     NEED_TASK: delegate task to executor
     *     BUFFER_OVERFLOW: allocate larger output buffer. Repeat unwrap
     *     BUFFER_UNDERFLOW: keep buffer and wait for more data
     *     OK: return generated buffers.
     *
     * Upstream subscription strategy is to try and keep no more than
     * TARGET_BUFSIZE bytes in readBuf
     */
    final class Reader extends SubscriberWrapper implements FlowTube.TubeSubscriber {
        static final int TARGET_BUFSIZE = 16 * 1024;

        final SequentialScheduler scheduler;
        volatile ByteBuffer readBuf;
        volatile boolean completing;
        final ReentrantLock readBufferLock = new ReentrantLock();
        final Logger debugr = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

        private final class ReaderDownstreamPusher implements Runnable {
            @Override
            public void run() {
                processData();
            }
        }

        Reader() {
            super();
            scheduler = SequentialScheduler.lockingScheduler(
                    new ReaderDownstreamPusher());
            this.readBuf = ByteBuffer.allocate(1024);
            readBuf.limit(0); 
        }

        @Override
        public boolean supportsRecycling() {
            return recycler != null;
        }

        protected SchedulingAction enterScheduling() {
            return enterReadScheduling();
        }

        public final String dbgString() {
            return "SSL Reader(" + tubeName + ")";
        }

        /**
         * entry point for buffers delivered from upstream Subscriber
         */
        @Override
        public void incoming(List<ByteBuffer> buffers, boolean complete) {
            if (debugr.on())
                debugr.log("Adding %d bytes to read buffer",
                        Utils.remaining(buffers));
            addToReadBuf(buffers, complete);
            scheduler.runOrSchedule(exec);
        }

        @Override
        public String toString() {
            return "READER: " + super.toString() + ", readBuf: " + readBuf.toString()
                    + ", count: " + count.toString() + ", scheduler: "
                    + (scheduler.isStopped() ? "stopped" : "running")
                    + ", status: " + lastUnwrapStatus
                    + ", handshakeState: " + handshakeState.get()
                    + ", engine: " + engine.getHandshakeStatus();
        }

        private void reallocReadBuf() {
            int sz = readBuf.capacity();
            ByteBuffer newb = ByteBuffer.allocate(sz * 2);
            readBuf.flip();
            Utils.copy(readBuf, newb);
            readBuf = newb;
        }

        @Override
        protected long upstreamWindowUpdate(long currentWindow, long downstreamQsize) {
            if (needsMoreData()) {
                if (debugr.on()) {
                    int remaining = readBuf.remaining();
                    if (remaining > TARGET_BUFSIZE) {
                        debugr.log("readBuf has more than TARGET_BUFSIZE: %d",
                                remaining);
                    }
                }
                scheduler.runOrSchedule();
            }
            return 0; 
        }

        private void addToReadBuf(List<ByteBuffer> buffers, boolean complete) {
            assert Utils.remaining(buffers) > 0 || buffers.isEmpty();
            readBufferLock.lock();
            try {
                for (ByteBuffer buf : buffers) {
                    readBuf.compact();
                    while (readBuf.remaining() < buf.remaining())
                        reallocReadBuf();
                    readBuf.put(buf);
                    readBuf.flip();
                    if (recycler != null) recycler.accept(buf);
                }
                if (complete) {
                    this.completing = complete;
                    minBytesRequired = 0;
                }
            } finally {
                readBufferLock.unlock();
            }
        }

        @Override
        protected boolean errorCommon(Throwable throwable) {
            throwable = SSLFlowDelegate.this.checkForHandshake(throwable);
            return super.errorCommon(throwable);
        }

        void schedule() {
            scheduler.runOrSchedule(exec);
        }

        void stop() {
            if (debugr.on()) debugr.log("stop");
            scheduler.stop();
        }

        AtomicInteger count = new AtomicInteger();

        volatile int minBytesRequired;

        boolean needsMoreData() {
            if (upstreamSubscription != null && readBuf.remaining() <= minBytesRequired &&
                    (engine.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP
                            || !downstreamSubscription.demand.isFulfilled() && hasNoOutputData())) {
                return true;
            }
            return false;
        }

        void requestMoreDataIfNeeded() {
            if (needsMoreData()) {
                requestMore();
            }
        }

        final void processData() {
            try {
                if (debugr.on())
                    debugr.log("processData:"
                            + " readBuf remaining:" + readBuf.remaining()
                            + ", state:" + states(handshakeState)
                            + ", engine handshake status:" + engine.getHandshakeStatus());
                int len;
                boolean complete = false;
                while (readBuf.remaining() > (len = minBytesRequired)) {
                    boolean handshaking = false;
                    try {
                        EngineResult result;
                        readBufferLock.lock();
                        try {
                            complete = this.completing;
                            if (debugr.on()) debugr.log("Unwrapping: %s", readBuf.remaining());
                            len = len > 0 ? minBytesRequired = 0 : len;
                            result = unwrapBuffer(readBuf);
                            len = readBuf.remaining();
                            if (debugr.on()) {
                                debugr.log("Unwrapped: result: %s", result.result);
                                debugr.log("Unwrapped: consumed: %s", result.bytesConsumed());
                            }
                        } finally {
                            readBufferLock.unlock();
                        }
                        if (result.bytesProduced() > 0) {
                            if (debugr.on())
                                debugr.log("sending %d", result.bytesProduced());
                            count.addAndGet(result.bytesProduced());
                            outgoing(result.destBuffer, false);
                        }
                        if (result.status() == Status.BUFFER_UNDERFLOW) {
                            if (debugr.on()) debugr.log("BUFFER_UNDERFLOW");
                            readBufferLock.lock();
                            try {
                                minBytesRequired = len;
                                assert readBuf.remaining() >= len;
                                if (readBuf.remaining() > len) continue;
                                else if (this.completing) {
                                    if (debug.on()) {
                                        debugr.log("BUFFER_UNDERFLOW with EOF," +
                                                " %d bytes non decrypted.", len);
                                    }
                                    throw new IOException("BUFFER_UNDERFLOW with EOF, "
                                            + len + " bytes non decrypted.");
                                }
                            } finally {
                                readBufferLock.unlock();
                            }
                            requestMoreDataIfNeeded();
                            return;
                        }
                        if (complete && result.status() == Status.CLOSED) {
                            if (debugr.on()) debugr.log("Closed: completing");
                            outgoing(Utils.EMPTY_BB_LIST, true);
                            setALPN();
                            requestMoreDataIfNeeded();
                            return;
                        }
                        if (result.handshaking()) {
                            handshaking = true;
                            if (debugr.on()) debugr.log("handshaking");
                            if (doHandshake(result, READER)) continue; 
                            else break; 
                        } else {
                            if (trySetALPN()) {
                                resumeActivity();
                            }
                        }
                    } catch (IOException ex) {
                        Throwable cause = checkForHandshake(ex);
                        errorCommon(cause);
                        handleError(cause);
                        return;
                    }
                    if (handshaking && !complete) {
                        requestMoreDataIfNeeded();
                        return;
                    }
                }
                if (!complete) {
                    readBufferLock.lock();
                    try {
                        complete = this.completing && !readBuf.hasRemaining();
                    } finally {
                        readBufferLock.unlock();
                    }
                }
                if (complete) {
                    if (debugr.on()) debugr.log("completing");
                    setALPN();
                    outgoing(Utils.EMPTY_BB_LIST, true);
                } else {
                    requestMoreDataIfNeeded();
                }
            } catch (Throwable ex) {
                ex = checkForHandshake(ex);
                errorCommon(ex);
                handleError(ex);
            }
        }

        private volatile Status lastUnwrapStatus;
        EngineResult unwrapBuffer(ByteBuffer src) throws IOException {
            ByteBuffer dst = getAppBuffer();
            int len = src.remaining();
            while (true) {
                SSLEngineResult sslResult = engine.unwrap(src, dst);
                switch (lastUnwrapStatus = sslResult.getStatus()) {
                    case BUFFER_OVERFLOW:
                        int appSize = applicationBufferSize =
                                engine.getSession().getApplicationBufferSize();
                        ByteBuffer b = ByteBuffer.allocate(appSize + dst.position());
                        dst.flip();
                        b.put(dst);
                        dst = b;
                        break;
                    case CLOSED:
                        assert dst.position() == 0;
                        return doClosure(new EngineResult(sslResult));
                    case BUFFER_UNDERFLOW:
                        assert dst.position() == 0;
                        return new EngineResult(sslResult);
                    case OK:
                        int size = dst.position();
                        if (debug.on()) {
                            debugr.log("Decoded " + size + " bytes out of " + len
                                    + " into buffer of " + dst.capacity()
                                    + " remaining to decode: " + src.remaining());
                        }
                        if (size > adaptiveAppBufferSize) {
                            adaptiveAppBufferSize = ((size + 7) >>> 3) << 3;
                        }
                        dst.flip();
                        return new EngineResult(sslResult, dst);
                }
            }
        }
    }

    public interface Monitorable {
        public String getInfo();
    }

    public static class Monitor extends Thread {
        final List<WeakReference<Monitorable>> list;
        final List<FinalMonitorable> finalList;
        final ReferenceQueue<Monitorable> queue = new ReferenceQueue<>();
        static Monitor themon;

        static {
            themon = new Monitor();
            themon.start(); 
        }

        final class FinalMonitorable implements Monitorable {
            final String finalState;
            FinalMonitorable(Monitorable o) {
                finalState = o.getInfo();
                finalList.add(this);
            }
            @Override
            public String getInfo() {
                finalList.remove(this);
                return finalState;
            }
        }

        Monitor() {
            super("Monitor");
            setDaemon(true);
            list = Collections.synchronizedList(new LinkedList<>());
            finalList = new ArrayList<>(); 
        }

        void addTarget(Monitorable o) {
            list.add(new WeakReference<>(o, queue));
        }
        void removeTarget(Monitorable o) {
            synchronized (list) {
                Iterator<WeakReference<Monitorable>> it = list.iterator();
                while (it.hasNext()) {
                    Monitorable m = it.next().get();
                    if (m == null) it.remove();
                    if (o == m) {
                        it.remove();
                        break;
                    }
                }
                FinalMonitorable m = new FinalMonitorable(o);
                addTarget(m);
                Reference.reachabilityFence(m);
            }
        }

        public static void add(Monitorable o) {
            themon.addTarget(o);
        }
        public static void remove(Monitorable o) {
            themon.removeTarget(o);
        }

        @Override
        public void run() {
            System.out.println("Monitor starting");
            try {
                while (true) {
                    Thread.sleep(20 * 1000);
                    synchronized (list) {
                        Reference<? extends Monitorable> expired;
                        while ((expired = queue.poll()) != null) list.remove(expired);
                        for (WeakReference<Monitorable> ref : list) {
                            Monitorable o = ref.get();
                            if (o == null) continue;
                            if (o instanceof FinalMonitorable) {
                                ref.enqueue();
                            }
                            System.out.println(o.getInfo());
                            System.out.println("-------------------------");
                        }
                    }
                    System.out.println("--o-o-o-o-o-o-o-o-o-o-o-o-o-o-");
                }
            } catch (InterruptedException e) {
                System.out.println("Monitor exiting with " + e);
            }
        }
    }

    /**
     * Processing function for outgoing data. Pass it thru SSLEngine.wrap()
     * Any encrypted buffers generated are passed downstream to be written.
     * Status codes:
     *     NEED_UNWRAP: call reader.addData() with empty buffer
     *     NEED_WRAP: call addData() with empty buffer
     *     NEED_TASK: delegate task to executor
     *     BUFFER_OVERFLOW: allocate larger output buffer. Repeat wrap
     *     BUFFER_UNDERFLOW: shouldn't happen on writing side
     *     OK: return generated buffers
     */
    class Writer extends SubscriberWrapper {
        final SequentialScheduler scheduler;
        final List<ByteBuffer> writeList;
        final Logger debugw =  Utils.getDebugLogger(this::dbgString, Utils.DEBUG);
        volatile boolean completing;
        boolean completed; 

        class WriterDownstreamPusher extends SequentialScheduler.CompleteRestartableTask {
            @Override public void run() { processData(); }
        }

        Writer() {
            super();
            writeList = Collections.synchronizedList(new LinkedList<>());
            scheduler = new SequentialScheduler(new WriterDownstreamPusher());
        }

        @Override
        protected void incoming(List<ByteBuffer> buffers, boolean complete) {
            assert complete ? buffers == Utils.EMPTY_BB_LIST : true;
            assert buffers != Utils.EMPTY_BB_LIST ? complete == false : true;
            if (complete) {
                if (debugw.on()) debugw.log("adding SENTINEL");
                completing = true;
                writeList.add(SENTINEL);
            } else {
                writeList.addAll(buffers);
            }
            if (debugw.on())
                debugw.log("added " + buffers.size()
                           + " (" + Utils.remaining(buffers)
                           + " bytes) to the writeList");
            scheduler.runOrSchedule();
        }

        public final String dbgString() {
            return "SSL Writer(" + tubeName + ")";
        }

        protected void onSubscribe() {
            if (debugw.on()) debugw.log("onSubscribe initiating handshaking");
            addData(HS_TRIGGER);  
        }

        void schedule() {
            scheduler.runOrSchedule();
        }

        void stop() {
            if (debugw.on()) debugw.log("stop");
            scheduler.stop();
        }

        @Override
        public boolean closing() {
            return closeNotifyReceived();
        }

        private boolean isCompleting() {
            return completing;
        }

        @Override
        protected long upstreamWindowUpdate(long currentWindow, long downstreamQsize) {
            if (writeList.size() > 10)
                return 0;
            else
                return super.upstreamWindowUpdate(currentWindow, downstreamQsize);
        }

        private boolean hsTriggered() {
            synchronized (writeList) {
                for (ByteBuffer b : writeList)
                    if (b == HS_TRIGGER)
                        return true;
                return false;
            }
        }

        void triggerWrite() {
            synchronized (writeList) {
                if (writeList.isEmpty()) {
                    writeList.add(HS_TRIGGER);
                }
            }
            scheduler.runOrSchedule();
        }

        private void processData() {
            boolean completing = isCompleting();

            try {
                if (debugw.on())
                    debugw.log("processData, writeList remaining:"
                                + Utils.synchronizedRemaining(writeList) + ", hsTriggered:"
                                + hsTriggered() + ", needWrap:" + needWrap());

                while (Utils.synchronizedRemaining(writeList) > 0 || hsTriggered() || needWrap()) {
                    if (scheduler.isStopped()) return;
                    ByteBuffer[] outbufs = writeList.toArray(Utils.EMPTY_BB_ARRAY);
                    EngineResult result = wrapBuffers(outbufs);
                    if (debugw.on())
                        debugw.log("wrapBuffer returned %s", result.result);

                    if (result.status() == Status.CLOSED) {
                        if (!upstreamCompleted) {
                            upstreamCompleted = true;
                            upstreamSubscription.cancel();
                            setALPN();
                        }
                        if (result.bytesProduced() <= 0)
                            return;

                        if (!completing && !completed) {
                            completing = this.completing = true;
                            writeList.add(SENTINEL);
                        }
                    }

                    boolean handshaking = false;
                    if (result.handshaking()) {
                        if (debugw.on()) debugw.log("handshaking");
                        doHandshake(result, WRITER);  
                        handshaking = true;
                    } else {
                        if (trySetALPN()) {
                            resumeActivity();
                        }
                    }
                    cleanList(writeList); 
                    sendResultBytes(result);
                    if (handshaking) {
                        if (!completing && needWrap()) {
                            continue;
                        } else {
                            return;
                        }
                    }
                }
                if (completing && Utils.synchronizedRemaining(writeList) == 0) {
                    if (!completed) {
                        completed = true;
                        writeList.clear();
                        outgoing(Utils.EMPTY_BB_LIST, true);
                    }
                    return;
                }
                if (writeList.isEmpty() && needWrap()) {
                    writer.addData(HS_TRIGGER);
                }
            } catch (Throwable ex) {
                ex = checkForHandshake(ex);
                errorCommon(ex);
                handleError(ex);
            }
        }

        volatile ByteBuffer writeBuffer;
        private volatile Status lastWrappedStatus;
        @SuppressWarnings("fallthrough")
        EngineResult wrapBuffers(ByteBuffer[] src) throws SSLException {
            long len = Utils.remaining(src);
            if (debugw.on())
                debugw.log("wrapping " + len + " bytes");

            ByteBuffer dst = writeBuffer;
            if (dst == null) dst = writeBuffer = getNetBuffer();
            assert dst.position() == 0 : "buffer position is " + dst.position();
            assert dst.hasRemaining() : "buffer has no remaining space: capacity=" + dst.capacity();

            while (true) {
                SSLEngineResult sslResult = engine.wrap(src, dst);
                if (debugw.on()) debugw.log("SSLResult: " + sslResult);
                switch (lastWrappedStatus = sslResult.getStatus()) {
                    case BUFFER_OVERFLOW:
                        if (debugw.on()) debugw.log("BUFFER_OVERFLOW");
                        int netSize = packetBufferSize
                                = engine.getSession().getPacketBufferSize();
                        ByteBuffer b = writeBuffer = ByteBuffer.allocate(netSize + dst.position());
                        dst.flip();
                        b.put(dst);
                        dst = b;
                        break; 
                    case CLOSED:
                        if (debugw.on()) debugw.log("CLOSED");
                    case OK:
                        final ByteBuffer dest;
                        if (dst.position() == 0) {
                            dest = NOTHING; 
                        } else if (dst.position() < dst.capacity() / 2) {
                            dst.flip();
                            dest = Utils.copyAligned(dst);
                            dst.clear();
                        } else {
                            dst.flip();
                            dest = dst;
                            writeBuffer = null;
                        }
                        if (debugw.on())
                            debugw.log("OK => produced: %d bytes into %d, not wrapped: %d",
                                       dest.remaining(),  dest.capacity(), Utils.remaining(src));
                        return new EngineResult(sslResult, dest);
                    case BUFFER_UNDERFLOW:
                        if (debug.on()) debug.log("BUFFER_UNDERFLOW");
                        return new EngineResult(sslResult);
                    default:
                        if (debugw.on())
                            debugw.log("result: %s", sslResult.getStatus());
                        assert false : "result:" + sslResult.getStatus();
                }
            }
        }

        private boolean needWrap() {
            return engine.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;
        }

        private void sendResultBytes(EngineResult result) {
            if (result.bytesProduced() > 0) {
                if (debugw.on())
                    debugw.log("Sending %d bytes downstream",
                               result.bytesProduced());
                outgoing(result.destBuffer, false);
            }
        }

        @Override
        public String toString() {
            return "WRITER: " + super.toString()
                    + ", writeList size: " + writeList.size()
                    + ", scheduler: " + (scheduler.isStopped() ? "stopped" : "running")
                    + ", status: " + lastWrappedStatus;
        }
    }

    private void handleError(Throwable t) {
        if (debug.on()) debug.log("handleError", t);
        readerCF.completeExceptionally(t);
        writerCF.completeExceptionally(t);
        alpnCF.completeExceptionally(t);
        reader.stop();
        writer.stop();
    }

    boolean stopped;

    private void normalStop() {
        synchronized (this) {
            if (stopped)
                return;
            stopped = true;
        }
        reader.stop();
        writer.stop();
        if (!alpnCF.isDone()) {
            Throwable alpn = new SSLHandshakeException(
                    "Connection closed before successful ALPN negotiation");
            alpnCF.completeExceptionally(alpn);
        }
        if (isMonitored) Monitor.remove(monitor);
    }

    private Void stopOnError(Throwable error) {
        if (!alpnCF.isDone()) {
            alpnCF.completeExceptionally(error);
        }
        normalStop();
        return null;
    }

    private void cleanList(List<ByteBuffer> l) {
        synchronized (l) {
            Iterator<ByteBuffer> iter = l.iterator();
            while (iter.hasNext()) {
                ByteBuffer b = iter.next();
                if (!b.hasRemaining() && b != SENTINEL) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * States for handshake. We avoid races when accessing/updating the AtomicInt
     * because updates always schedule an additional call to both the read()
     * and write() functions.
     */
    private static final int NOT_HANDSHAKING = 0;
    private static final int HANDSHAKING = 1;

    private static final int DOING_TASKS = 4;
    private static final int REQUESTING_TASKS = 8;
    private static final int TASK_BITS = 12; 

    private static final int READER = 1;
    private static final int WRITER = 2;

    private static String states(AtomicInteger state) {
        int s = state.get();
        StringBuilder sb = new StringBuilder();
        int x = s & ~TASK_BITS;
        switch (x) {
            case NOT_HANDSHAKING    -> sb.append(" NOT_HANDSHAKING ");
            case HANDSHAKING        -> sb.append(" HANDSHAKING ");

            default -> throw new InternalError();
        }
        if ((s & DOING_TASKS) > 0)
            sb.append("|DOING_TASKS");
        if ((s & REQUESTING_TASKS) > 0)
            sb.append("|REQUESTING_TASKS");
        return sb.toString();
    }

    private void resumeActivity() {
        reader.schedule();
        writer.schedule();
    }

    final AtomicInteger handshakeState;
    final ConcurrentLinkedQueue<String> stateList =
            debug.on() ? new ConcurrentLinkedQueue<>() : null;

    private static final IntBinaryOperator REQUEST_OR_DO_TASKS = (current, ignored) -> {
        if ((current & DOING_TASKS) == 0)
            return DOING_TASKS | (current & HANDSHAKING);
        else
            return DOING_TASKS | REQUESTING_TASKS | (current & HANDSHAKING);
    };

    private static final IntBinaryOperator FINISH_OR_DO_TASKS = (current, ignored) -> {
        if ((current & REQUESTING_TASKS) != 0)
            return DOING_TASKS | (current & HANDSHAKING);
        return (current & HANDSHAKING);
    };

    private boolean doHandshake(EngineResult r, int caller) {
        handshakeState.getAndAccumulate(0, (current, unused) -> HANDSHAKING | (current & TASK_BITS));
        if (stateList != null && debug.on()) {
            stateList.add(r.handshakeStatus().toString());
            stateList.add(Integer.toString(caller));
        }
        switch (r.handshakeStatus()) {
            case NEED_TASK:
                int s = handshakeState.accumulateAndGet(0, REQUEST_OR_DO_TASKS);
                if ((s & REQUESTING_TASKS) > 0) { 
                    return false;
                }

                if (debug.on()) debug.log("obtaining and initiating task execution");
                List<Runnable> tasks = obtainTasks();
                executeTasks(tasks);
                return false;  
            case NEED_WRAP:
                if (caller == READER) {
                    writer.triggerWrite();
                    return false;
                }
                break;
            case NEED_UNWRAP:
            case NEED_UNWRAP_AGAIN:
                if (caller == WRITER) {
                    reader.schedule();
                    return false;
                }
                break;
            default:
                throw new InternalError("Unexpected handshake status:"
                                        + r.handshakeStatus());
        }
        return true;
    }

    private List<Runnable> obtainTasks() {
        List<Runnable> l = new ArrayList<>();
        Runnable r;
        while ((r = engine.getDelegatedTask()) != null) {
            l.add(r);
        }
        return l;
    }

    private void executeTasks(List<Runnable> tasks) {
        exec.execute(() -> {
            try {
                List<Runnable> nextTasks = tasks;
                if (debug.on()) debug.log("#tasks to execute: " + nextTasks.size());
                do {
                    nextTasks.forEach(Runnable::run);
                    if (engine.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                        nextTasks = obtainTasks();
                    } else {
                        int s = handshakeState.accumulateAndGet(0, FINISH_OR_DO_TASKS);
                        if ((s & DOING_TASKS) != 0) {
                            if (debug.on()) debug.log("re-running tasks (B)");
                            nextTasks = obtainTasks();
                            continue;
                        }
                        break;
                    }
                } while (true);
                if (debug.on()) debug.log("finished task execution");
                HandshakeStatus hs = engine.getHandshakeStatus();
                if (hs == HandshakeStatus.FINISHED || hs == HandshakeStatus.NOT_HANDSHAKING) {
                    trySetALPN();
                }
                resumeActivity();
            } catch (Throwable t) {
                handleError(checkForHandshake(t));
            }
        });
    }

    boolean trySetALPN() {
        if ((handshakeState.getAndSet(NOT_HANDSHAKING) & ~DOING_TASKS) == HANDSHAKING) {
            applicationBufferSize = engine.getSession().getApplicationBufferSize();
            packetBufferSize = engine.getSession().getPacketBufferSize();
            setALPN();
            return true;
        }
        return false;
    }

    EngineResult doClosure(EngineResult r) throws IOException {
        if (debug.on())
            debug.log("doClosure(%s): %s [isOutboundDone: %s, isInboundDone: %s]",
                      r.result, engine.getHandshakeStatus(),
                      engine.isOutboundDone(), engine.isInboundDone());
        if (engine.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) {
            if (engine.isInboundDone() && !engine.isOutboundDone()) {
                if (debug.on()) debug.log("doClosure: close_notify received");
                close_notify_received = true;
                if (!writer.scheduler.isStopped()) {
                    doHandshake(r, READER);
                } else {
                    var readerLock = reader.readBufferLock;
                    readerLock.lock();
                    try {
                        reader.completing = true;
                    } finally {
                        readerLock.unlock();
                    }
                }
            }
        }
        return r;
    }

    /**
     * Returns the upstream Flow.Subscriber of the reading (incoming) side.
     * This flow must be given the encrypted data read from upstream (eg socket)
     * before it is decrypted.
     */
    public Flow.Subscriber<List<ByteBuffer>> upstreamReader() {
        return reader;
    }

    /**
     * Returns the upstream Flow.Subscriber of the writing (outgoing) side.
     * This flow contains the plaintext data before it is encrypted.
     */
    public Flow.Subscriber<List<ByteBuffer>> upstreamWriter() {
        return writer;
    }

    public boolean resumeReader() {
        return reader.signalScheduling();
    }

    public void resetReaderDemand() {
        reader.resetDownstreamDemand();
    }

    static class EngineResult {
        final SSLEngineResult result;
        final ByteBuffer destBuffer;

        EngineResult(SSLEngineResult result) {
            this(result, null);
        }

        EngineResult(SSLEngineResult result, ByteBuffer destBuffer) {
            this.result = result;
            this.destBuffer = destBuffer;
        }

        boolean handshaking() {
            HandshakeStatus s = result.getHandshakeStatus();
            return s != HandshakeStatus.FINISHED
                   && s != HandshakeStatus.NOT_HANDSHAKING
                   && result.getStatus() != Status.CLOSED;
        }

        boolean needUnwrap() {
            HandshakeStatus s = result.getHandshakeStatus();
            return s == HandshakeStatus.NEED_UNWRAP;
        }


        int bytesConsumed() {
            return result.bytesConsumed();
        }

        int bytesProduced() {
            return result.bytesProduced();
        }

        SSLEngineResult.HandshakeStatus handshakeStatus() {
            return result.getHandshakeStatus();
        }

        SSLEngineResult.Status status() {
            return result.getStatus();
        }
    }

    volatile int packetBufferSize;
    final ByteBuffer getNetBuffer() {
        int netSize = packetBufferSize;
        if (netSize <= 0) {
            packetBufferSize = netSize = engine.getSession().getPacketBufferSize();
        }
        return ByteBuffer.allocate(netSize);
    }

    volatile int applicationBufferSize;
    volatile int adaptiveAppBufferSize;
    final ByteBuffer getAppBuffer() {
        int appSize = applicationBufferSize;
        if (appSize <= 0) {
            applicationBufferSize = appSize
                    = engine.getSession().getApplicationBufferSize();
        }
        int size = adaptiveAppBufferSize;
        if (size <= 0) {
            size = 512; 
        } else if (size > appSize) {
            size = appSize;
        }
        return ByteBuffer.allocate(size);
    }

    final String dbgString() {
        return "SSLFlowDelegate(" + tubeName + ")";
    }
}
