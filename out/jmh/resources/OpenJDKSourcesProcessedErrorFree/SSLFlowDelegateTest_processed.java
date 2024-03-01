/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.SSLFlowDelegate;
import jdk.internal.net.http.common.SubscriberWrapper;
import jdk.internal.net.http.common.Utils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SSLFlowDelegateTest {
    private static final String ALPN = "foobar";
    private static final String debugTag = SSLFlowDelegateTest.class.getSimpleName();
    private static final Random random = new Random();
    private static final byte DATA_BYTE = (byte) random.nextInt();

    private ExecutorService executor;
    private SSLContext sslContext;
    private SSLParameters sslParams;
    private SSLServerSocket sslServerSocket;
    private SSLEngine clientEngine;
    private CompletableFuture<Void> testCompletion;

    @BeforeTest
    public void beforeTest() throws Exception {
        this.executor = Executors.newCachedThreadPool();
        this.sslContext = new jdk.internal.net.http.SimpleSSLContext().get();
        this.testCompletion = new CompletableFuture<>();

        final SSLParameters sp = new SSLParameters();
        sp.setApplicationProtocols(new String[]{ALPN});
        this.sslParams = sp;

        this.sslServerSocket = startServer(this.sslContext);
        println(debugTag, "Server started at " + this.sslServerSocket.getInetAddress() + ":"
                + this.sslServerSocket.getLocalPort());

        this.clientEngine = createClientEngine(this.sslContext);
    }

    @AfterTest
    public void afterTest() throws Exception {
        if (this.sslServerSocket != null) {
            println(debugTag, "Closing server socket " + this.sslServerSocket);
            this.sslServerSocket.close();
        }
        if (this.executor != null) {
            println(debugTag, "Shutting down the executor " + this.executor);
            this.executor.shutdownNow();
        }
    }

    private static void println(final String debugTag, final String msg) {
        println(debugTag, msg, null);
    }

    private static void println(final String debugTag, final String msg, final Throwable t) {
        final Logger logger = Utils.getDebugLogger(() -> debugTag);
        logger.log(msg);
        if (t != null) {
            t.printStackTrace();
        }
    }

    private SSLServerSocket createSSLServerSocket(
            final SSLContext ctx, final InetSocketAddress bindAddr) throws IOException {
        final SSLServerSocketFactory fac = ctx.getServerSocketFactory();
        final SSLServerSocket sslServerSocket = (SSLServerSocket) fac.createServerSocket();
        sslServerSocket.setReuseAddress(false);
        sslServerSocket.setSSLParameters(this.sslParams);
        sslServerSocket.bind(bindAddr);
        return sslServerSocket;
    }

    private SSLServerSocket startServer(final SSLContext ctx) throws Exception {
        final SSLServerSocket sslServerSocket = createSSLServerSocket(ctx,
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        final Runnable serverResponsePusher = new ServerResponsePusher(sslServerSocket,
                this.testCompletion);
        final Thread serverThread = new Thread(serverResponsePusher, "serverResponsePusher");
        serverThread.start();
        return sslServerSocket;
    }

    private SSLEngine createClientEngine(final SSLContext ctx) {
        final SSLEngine clientEngine = ctx.createSSLEngine();
        clientEngine.setSSLParameters(this.sslParams);
        clientEngine.setUseClientMode(true);
        return clientEngine;
    }

    /**
     * Constructs a {@code SSLFlowDelegate} with a {@code downReader} which only requests one
     * item and then never requests any more. After that one item has been received by the
     * {@code downReader}, this test feeds the
     * {@linkplain SSLFlowDelegate#upstreamReader() upstreamReader} with (network SSL) data in
     * such a manner that while
     * {@link SSLEngine#unwrap(ByteBuffer, ByteBuffer) unwrapping} it in the internal implementation
     * of {@code SSLFlowDelegate}, it will often trigger {@code BUFFER_UNDERFLOW} state.
     * This test then verifies that the {@code SSLFlowDelegate} when it reaches such a state will
     * not keep asking for more (network SSL) data and decrypting it to application data and
     * accumulating it (since the {@code downReader} won't be using any of this accumulated data).
     */
    @Test
    public void testUnsolicitedBytes() throws Exception {
        try (final Socket socket = new Socket(sslServerSocket.getInetAddress(),
                sslServerSocket.getLocalPort())) {
            println(debugTag, "connected to server, local socket: " + socket);
            final CompletableFuture<Long> soleExpectedAppData = new CompletableFuture<>();
            this.testCompletion.whenComplete((r, t) -> {
                if (soleExpectedAppData.isDone()) {
                    return;
                }
                if (t == null) {
                    soleExpectedAppData.complete(-1L); 
                    return;
                }
                soleExpectedAppData.completeExceptionally(t);
            });
            final AppResponseReceiver appResponseReceiver = new AppResponseReceiver(
                    this.testCompletion, soleExpectedAppData);
            final SocketWriter networkDataWriter = new SocketWriter(socket, this.testCompletion);
            final SSLFlowDelegate sslFlowDelegate = new SSLFlowDelegate(clientEngine, executor,
                    appResponseReceiver, networkDataWriter);
            final SocketReader socketReader = new SocketReader(socket, executor,
                    sslFlowDelegate.upstreamReader(), this.testCompletion);
            new Thread(socketReader, "socketReader").start();

            final Flow.Publisher<List<ByteBuffer>> publisher = new ZeroDataPublisher<>();
            println(debugTag, "Subscribing the upstreamWriter() to trigger SSL handshake");
            publisher.subscribe(sslFlowDelegate.upstreamWriter());
            println(debugTag, "Waiting for handshake to complete");
            final String negotiatedALPN = sslFlowDelegate.alpn().join();
            println(debugTag, "handshake completed, with negotiated ALPN: " + negotiatedALPN);
            Assert.assertEquals(negotiatedALPN, ALPN, "unexpected ALPN negotiated");
            try {
                println(debugTag, "waiting for the sole expected chunk of application data to" +
                        " become available to " + appResponseReceiver);
                final long numAppDataBytesReceived = soleExpectedAppData.join();
                println(debugTag, "Received " + numAppDataBytesReceived + " app data bytes," +
                        " no more app data expected");
                this.testCompletion.join();
            } catch (CompletionException ce) {
                failTest(ce);
            }
            println(debugTag, "now checking if any unsolicited bytes accumulated");
            assertNoUnsolicitedBytes(sslFlowDelegate);
            println(debugTag, "testing completed successfully, no unsolicited bytes accumulated");
        }
    }

    private void failTest(final CompletionException ce) {
        final Throwable cause = ce.getCause();
        Assert.fail(cause.getMessage() == null ? "test failed" : cause.getMessage(), cause);
    }

    private void assertNoUnsolicitedBytes(final SSLFlowDelegate sslFlowDelegate) throws Exception {
        final Field readerField = SSLFlowDelegate.class.getDeclaredField("reader");
        readerField.setAccessible(true);

        final Field readerOutputQField = SubscriberWrapper.class.getDeclaredField("outputQ");
        readerOutputQField.setAccessible(true);

        final Object reader = readerField.get(sslFlowDelegate);
        final ConcurrentLinkedQueue<List<ByteBuffer>> outputQ =
                ConcurrentLinkedQueue.class.cast(readerOutputQField.get(reader));
        long numUnsolicitated = 0;
        List<ByteBuffer> accumulations;
        while ((accumulations = outputQ.poll()) != null) {
            println(debugTag, "found some items in outputQ");
            for (final ByteBuffer buf : accumulations) {
                if (!buf.hasRemaining()) {
                    continue;
                }
                try {
                    numUnsolicitated = Math.addExact(numUnsolicitated, buf.remaining());
                } catch (ArithmeticException ame) {
                    numUnsolicitated = Long.MAX_VALUE;
                    break;
                }
            }
            println(debugTag, "num unsolicited bytes so far = " + numUnsolicitated);
        }
        Assert.assertEquals(numUnsolicitated, 0,
                "SSLFlowDelegate has accumulated " + numUnsolicitated + " unsolicited bytes");
    }

    private static final class ZeroDataPublisher<T> implements Flow.Publisher<T> {
        private final AtomicBoolean hasSubscriber = new AtomicBoolean();

        @Override
        public void subscribe(final Subscriber<? super T> subscriber) {
            if (!hasSubscriber.compareAndSet(false, true)) {
                throw new IllegalStateException("Cannot subscribe more than once");
            }
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
        }
    }

    private static final class SocketWriter implements Subscriber<List<ByteBuffer>> {
        private static final String debugTag = SocketWriter.class.getSimpleName();

        private final Socket socket;
        private final CompletableFuture<Void> completion;
        private volatile Flow.Subscription subscription;
        private final AtomicLong numBytesWritten = new AtomicLong();

        private SocketWriter(final Socket socket, final CompletableFuture<Void> completion) {
            this.socket = socket;
            this.completion = completion;
        }

        @Override
        public void onSubscribe(final Flow.Subscription subscription) {
            this.subscription = subscription;
            println(debugTag, "onSubscribe invoked, requesting for data to write to socket");
            subscription.request(1);
        }

        @Override
        public void onNext(final List<ByteBuffer> bufs) {
            try {
                final OutputStream os =
                        new BufferedOutputStream(this.socket.getOutputStream());

                for (final ByteBuffer buf : bufs) {
                    int len = buf.remaining();
                    int written = writeToStream(os, buf);
                    assert len == written;
                    this.numBytesWritten.addAndGet(len);
                    assert !buf.hasRemaining()
                            : "buffer has " + buf.remaining() + " bytes left";
                    this.subscription.request(1); 
                }
            } catch (Throwable e) {
                println(debugTag, "failed: " + e, e);
                completion.completeExceptionally(e);
            }
        }

        @Override
        public void onError(final Throwable throwable) {
            println(debugTag, "error: " + throwable, throwable);
            completion.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            println(debugTag, "onComplete(), total bytes written: " + this.numBytesWritten.get());
        }

        private int writeToStream(final OutputStream os, final ByteBuffer buf) throws IOException {
            final byte[] b = buf.array();
            final int offset = buf.arrayOffset() + buf.position();
            final int n = buf.limit() - buf.position();
            os.write(b, offset, n);
            buf.position(buf.limit());
            os.flush();
            return n;
        }
    }

    private static final class SocketReader implements Runnable {
        private static final String debugTag = SocketReader.class.getSimpleName();

        private static final int VERY_SMALL_DATA_SIZE = 123;

        private final Socket socket;
        private final SubmissionPublisher<List<ByteBuffer>> publisher;
        private final CompletableFuture<Void> completion;

        private SocketReader(final Socket socket, final Executor executor,
                             final Subscriber<List<ByteBuffer>> incomingNetworkDataSubscriber,
                             final CompletableFuture<Void> completion) {
            this.socket = socket;
            this.completion = completion;
            this.publisher = new SubmissionPublisher<>(executor, Flow.defaultBufferSize(),
                    (s, t) -> completion.completeExceptionally(t));
            this.publisher.subscribe(incomingNetworkDataSubscriber);
        }

        @Override
        public void run() {
            try {
                final InputStream is = socket.getInputStream();
                long numBytesRead = 0;
                long numBytesPublished = 0;
                while (true) {
                    final byte[] buf = new byte[10240]; 
                    final int n = is.read(buf);
                    if (n == -1) {
                        println(debugTag, "got EOF, now closing resources; total read "
                                + numBytesRead + " bytes, total published " + numBytesPublished
                                + " bytes");
                        closeAndComplete(is);
                        return;
                    }
                    println(debugTag, "read " + n + " bytes from socket");
                    numBytesRead = Math.addExact(numBytesRead, n);
                    int remaining = n;
                    int index = 0;
                    while (remaining > 0) {
                        final int chunkSize = Math.min(remaining, VERY_SMALL_DATA_SIZE);
                        final byte[] chunk = Arrays.copyOfRange(buf, index, index + chunkSize);
                        index += chunkSize;
                        remaining -= chunkSize;
                        final int lagOrDrops = publisher.offer(
                                List.of(ByteBuffer.wrap(chunk)), 2, TimeUnit.SECONDS, null);
                        if (lagOrDrops < 0) {
                            println(debugTag, "dropped a chunk, re-offering");
                            final int newLagOrDrops = publisher.offer(
                                    List.of(ByteBuffer.wrap(chunk)), 2, TimeUnit.SECONDS, null);
                            if (newLagOrDrops < 0) {
                                println(debugTag, "dropped the re-offered chunk; closing resources," +
                                        " total bytes read: " + numBytesRead +
                                        " total bytes published: " + numBytesPublished);
                                closeAndComplete(is);
                                return;
                            }
                        }
                        numBytesPublished += chunkSize;
                        println(debugTag, "published " + numBytesPublished + " bytes of total "
                                + numBytesRead + " bytes read");
                    }
                }
            } catch (Throwable e) {
                println(debugTag, "failed: " + e, e);
                completion.completeExceptionally(e);
            }
        }

        private void closeAndComplete(final InputStream is) {
            publisher.close();
            completion.complete(null);
            Utils.close(is);
        }
    }

    private static final class ServerResponsePusher implements Runnable {
        private static final String debugTag = ServerResponsePusher.class.getSimpleName();
        private final SSLServerSocket sslServerSocket;
        private final CompletableFuture<Void> completion;

        private ServerResponsePusher(final SSLServerSocket sslServerSocket,
                                     final CompletableFuture<Void> completion) {
            this.sslServerSocket = sslServerSocket;
            this.completion = completion;
        }

        @Override
        public void run() {
            try {
                try (final Socket socket = this.sslServerSocket.accept()) {
                    println(debugTag, "Accepted connection from " + socket);
                    try (final OutputStream os = socket.getOutputStream()) {
                        final byte[] resp = new byte[10240]; 
                        Arrays.fill(resp, DATA_BYTE);
                        long numWritten = 0;
                        final int numTimes = 50;
                        for (int i = 0; i < numTimes; i++) {
                            println(debugTag, "writing " + resp.length + " bytes, "
                                    + numWritten + " written so far");
                            os.write(resp);
                            numWritten += resp.length;
                            os.flush();
                        }
                        println(debugTag, "stopped writing, total bytes written: " + numWritten);
                    }
                }
            } catch (Throwable e) {
                println(debugTag, "error: " + e, e);
                this.completion.completeExceptionally(e);
            }
        }
    }

    private static final class AppResponseReceiver implements Subscriber<List<ByteBuffer>> {
        private static final String debugTag = AppResponseReceiver.class.getSimpleName();

        private final byte[] expectedData = new byte[1024]; 

        private final AtomicLong numBytesReceived;
        private volatile Flow.Subscription subscription;
        private final CompletableFuture<Void> completion;
        private final CompletableFuture<Long> soleExpectedAppData;
        private boolean receivedOneItem;

        private AppResponseReceiver(final CompletableFuture<Void> completion,
                                    final CompletableFuture<Long> soleExpectedAppData) {
            this.numBytesReceived = new AtomicLong(0);
            this.soleExpectedAppData = soleExpectedAppData;
            this.completion = completion;
            Arrays.fill(expectedData, DATA_BYTE);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            println(debugTag, "onSubscribe invoked");
            this.subscription = subscription;
            subscription.request(1); 
        }

        @Override
        public void onNext(final List<ByteBuffer> buffers) {
            if (receivedOneItem) {
                completion.completeExceptionally(new AssertionError("onNext() called more than" +
                        " once, even though no request was made"));
                return;
            }
            receivedOneItem = true;
            for (final ByteBuffer buf : buffers) {
                final int numBytes = buf.remaining();
                while (buf.hasRemaining()) {
                    final int size = Math.min(buf.remaining(), expectedData.length);
                    final byte[] actual = new byte[size];
                    buf.get(actual);
                    final int index = Arrays.mismatch(expectedData, 0, size, actual, 0, size);
                    if (index != -1) {
                        final String errMsg = "Unexpected byte received: " + actual[index];
                        println(debugTag, "Cancelling subscription due to error: " + errMsg);
                        subscription.cancel();
                        completion.completeExceptionally(new AssertionError(errMsg));
                        return;
                    }
                }
                numBytesReceived.addAndGet(numBytes);
            }
            println(debugTag, "Received " + numBytesReceived.get() + " bytes," +
                    " will not request any more data");
            soleExpectedAppData.complete(numBytesReceived.get());
        }

        @Override
        public void onError(final Throwable throwable) {
            completion.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            final long n = numBytesReceived.get();
            println(debugTag, "Completed: received " + n + " bytes");
        }
    }
}
