/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.stream.Stream;
import static java.lang.System.err;

/*
 * @test
 * @summary An example on how to read a response body with InputStream.
 * @run main/othervm/manual -Dtest.debug=true HttpInputStreamTest
 * @author daniel fuchs
 */
public class HttpInputStreamTest {

    public static boolean DEBUG = Boolean.getBoolean("test.debug");

    /**
     * A simple HttpResponse.BodyHandler that creates a live
     * InputStream to read the response body from the underlying ByteBuffer
     * Flow.
     * The InputStream is made immediately available for consumption, before
     * the response body is fully received.
     */
    public static class HttpInputStreamHandler
        implements HttpResponse.BodyHandler<InputStream>    {

        public static final int MAX_BUFFERS_IN_QUEUE = 1;  

        private final int maxBuffers;

        public HttpInputStreamHandler() {
            this(MAX_BUFFERS_IN_QUEUE);
        }

        public HttpInputStreamHandler(int maxBuffers) {
            this.maxBuffers = maxBuffers <= 0 ? MAX_BUFFERS_IN_QUEUE : maxBuffers;
        }

        @Override
        public HttpResponse.BodySubscriber<InputStream>
                apply(HttpResponse.ResponseInfo rinfo) {
            return new HttpResponseInputStream(maxBuffers);
        }

        /**
         * An InputStream built on top of the Flow API.
         */
        private static class HttpResponseInputStream extends InputStream
                    implements HttpResponse.BodySubscriber<InputStream> {

            private static final ByteBuffer LAST_BUFFER = ByteBuffer.wrap(new byte[0]);
            private static final List<ByteBuffer> LAST_LIST = List.of(LAST_BUFFER);

            private final BlockingQueue<List<ByteBuffer>> buffers;
            private volatile Flow.Subscription subscription;
            private volatile boolean closed;
            private volatile Throwable failed;
            private volatile Iterator<ByteBuffer> currentListItr;
            private volatile ByteBuffer currentBuffer;

            HttpResponseInputStream() {
                this(MAX_BUFFERS_IN_QUEUE);
            }

            HttpResponseInputStream(int maxBuffers) {
                int capacity = maxBuffers <= 0 ? MAX_BUFFERS_IN_QUEUE : maxBuffers;
                this.buffers = new ArrayBlockingQueue<>(capacity + 1);
            }

            @Override
            public CompletionStage<InputStream> getBody() {
                return CompletableFuture.completedStage(this);
            }

            private ByteBuffer current() throws IOException {
                while (currentBuffer == null || !currentBuffer.hasRemaining()) {
                    if (closed || failed != null) {
                        throw new IOException("closed", failed);
                    }
                    if (currentBuffer == LAST_BUFFER) break;

                    try {
                        if (currentListItr == null || !currentListItr.hasNext()) {

                            if (DEBUG) err.println("Taking list of Buffers");
                            List<ByteBuffer> lb = buffers.take();
                            currentListItr = lb.iterator();
                            if (DEBUG) err.println("List of Buffers Taken");

                            if (closed || failed != null)
                                throw new IOException("closed", failed);

                            if (lb == LAST_LIST) {
                                currentListItr = null;
                                currentBuffer = LAST_BUFFER;
                                break;
                            }

                            Flow.Subscription s = subscription;
                            if (s != null)
                                s.request(1);
                        }
                        assert currentListItr != null;
                        assert currentListItr.hasNext();
                        if (DEBUG) err.println("Next Buffer");
                        currentBuffer = currentListItr.next();
                    } catch (InterruptedException ex) {
                    }
                }
                assert currentBuffer == LAST_BUFFER || currentBuffer.hasRemaining();
                return currentBuffer;
            }

            @Override
            public int read(byte[] bytes, int off, int len) throws IOException {
                ByteBuffer buffer;
                if ((buffer = current()) == LAST_BUFFER) return -1;

                int read = Math.min(buffer.remaining(), len);
                assert read > 0 && read <= buffer.remaining();

                buffer.get(bytes, off, read);
                return read;
            }

            @Override
            public int read() throws IOException {
                ByteBuffer buffer;
                if ((buffer = current()) == LAST_BUFFER) return -1;
                return buffer.get() & 0xFF;
            }

            @Override
            public void onSubscribe(Flow.Subscription s) {
                if (this.subscription != null) {
                    s.cancel();
                    return;
                }
                this.subscription = s;
                assert buffers.remainingCapacity() > 1; 
                if (DEBUG) err.println("onSubscribe: requesting "
                     + Math.max(1, buffers.remainingCapacity() - 1));
                s.request(Math.max(1, buffers.remainingCapacity() - 1));
            }

            @Override
            public void onNext(List<ByteBuffer> t) {
                try {
                    if (DEBUG) err.println("next item received");
                    if (!buffers.offer(t)) {
                        throw new IllegalStateException("queue is full");
                    }
                    if (DEBUG) err.println("item offered");
                } catch (Exception ex) {
                    failed = ex;
                    try {
                        close();
                    } catch (IOException ex1) {
                    }
                }
            }

            @Override
            public void onError(Throwable thrwbl) {
                subscription = null;
                failed = thrwbl;
            }

            @Override
            public void onComplete() {
                subscription = null;
                onNext(LAST_LIST);
            }

            @Override
            public void close() throws IOException {
                synchronized (this) {
                    if (closed) return;
                    closed = true;
                }
                Flow.Subscription s = subscription;
                subscription = null;
                if (s != null) {
                     s.cancel();
                }
                super.close();
            }

        }
    }

    /**
     * Examine the response headers to figure out the charset used to
     * encode the body content.
     * If the content type is not textual, returns an empty Optional.
     * Otherwise, returns the body content's charset, defaulting to
     * ISO-8859-1 if none is explicitly specified.
     * @param headers The response headers.
     * @return The charset to use for decoding the response body, if
     *         the response body content is text/...
     */
    public static Optional<Charset> getCharset(HttpHeaders headers) {
        Optional<String> contentType = headers.firstValue("Content-Type");
        Optional<Charset> charset = Optional.empty();
        if (contentType.isPresent()) {
            final String[] values = contentType.get().split(";");
            if (values[0].startsWith("text/")) {
                charset = Optional.of(Stream.of(values)
                    .map(x -> x.toLowerCase(Locale.ROOT))
                    .map(String::trim)
                    .filter(x -> x.startsWith("charset="))
                    .map(x -> x.substring("charset=".length()))
                    .findFirst()
                    .orElse("ISO-8859-1"))
                    .map(Charset::forName);
            }
        }
        return charset;
    }

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest
            .newBuilder(new URI("https:
            .GET()
            .build();


        CompletableFuture<HttpResponse<InputStream>> handle =
            client.sendAsync(request, new HttpInputStreamHandler(3));
        if (DEBUG) err.println("Request sent");

        HttpResponse<InputStream> pending = handle.get();

        HttpHeaders responseHeaders = pending.headers();

        Optional<Charset> charset = getCharset(responseHeaders);

        try (InputStream is = pending.body();
            Reader r = new InputStreamReader(is, charset.get())) {

            char[] buff = new char[32];
            int off=0, n=0;
            if (DEBUG) err.println("Start receiving response body");
            if (DEBUG) err.println("Charset: " + charset.get());

            while ((n = r.read(buff, off, buff.length - off)) > 0) {
                assert (buff.length - off) > 0;
                assert n <= (buff.length - off);
                if (n == (buff.length - off)) {
                    System.out.print(buff);
                    off = 0;
                } else {
                    off += n;
                }
                assert off < buff.length;
            }

            assert off >= 0 && off < buff.length;
            for (int i=0; i < off; i++) {
                System.out.print(buff[i]);
            }

            System.out.println("Done!");
        }
    }

}
