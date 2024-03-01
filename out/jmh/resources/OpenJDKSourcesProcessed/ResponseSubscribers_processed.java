/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.io.BufferedReader;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.net.http.HttpResponse.BodySubscriber;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.HttpClientImpl.DelegatingExecutor;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ResponseSubscribers {

    /**
     * This interface is used by our BodySubscriber implementations to
     * declare whether calling getBody() inline is safe, or whether
     * it needs to be called asynchronously in an executor thread.
     * Calling getBody() inline is usually safe except when it
     * might block - which can be the case if the BodySubscriber
     * is provided by custom code, or if it uses a finisher that
     * might be called and might block before the last bit is
     * received (for instance, if a mapping subscriber is used with
     * a mapper function that maps an InputStream to a GZIPInputStream,
     * as the constructor of GZIPInputStream calls read()).
     * @param <T> The response type.
     */
    public interface TrustedSubscriber<T> extends BodySubscriber<T> {
        /**
         * Returns true if getBody() should be called asynchronously.
         * @implSpec The default implementation of this method returns
         *           false.
         * @return true if getBody() should be called asynchronously.
         */
        default boolean needsExecutor() { return false;}

        /**
         * Returns true if calling {@code bs::getBody} might block
         * and requires an executor.
         *
         * @implNote
         * In particular this method returns
         * true if {@code bs} is not a {@code TrustedSubscriber}.
         * If it is a {@code TrustedSubscriber}, it returns
         * {@code ((TrustedSubscriber) bs).needsExecutor()}.
         *
         * @param bs A BodySubscriber.
         * @return true if calling {@code bs::getBody} requires using
         *         an executor.
         */
        static boolean needsExecutor(BodySubscriber<?> bs) {
            if (bs instanceof TrustedSubscriber) {
                return ((TrustedSubscriber) bs).needsExecutor();
            } else return true;
        }
    }

    public static class ConsumerSubscriber implements TrustedSubscriber<Void> {
        private final Consumer<Optional<byte[]>> consumer;
        private Flow.Subscription subscription;
        private final CompletableFuture<Void> result = new MinimalFuture<>();
        private final AtomicBoolean subscribed = new AtomicBoolean();

        public ConsumerSubscriber(Consumer<Optional<byte[]>> consumer) {
            this.consumer = Objects.requireNonNull(consumer);
        }

        @Override
        public CompletionStage<Void> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (!subscribed.compareAndSet(false, true)) {
                subscription.cancel();
            } else {
                this.subscription = subscription;
                subscription.request(1);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            Objects.requireNonNull(items);
            for (ByteBuffer item : items) {
                byte[] buf = new byte[item.remaining()];
                item.get(buf);
                consumer.accept(Optional.of(buf));
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            Objects.requireNonNull(throwable);
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            consumer.accept(Optional.empty());
            result.complete(null);
        }

    }

    /**
     * A Subscriber that writes the flow of data to a given file.
     *
     * Privileged actions are performed within a limited doPrivileged that only
     * asserts the specific, write, file permissions that were checked during
     * the construction of this PathSubscriber.
     */
    public static class PathSubscriber implements TrustedSubscriber<Path> {

        private static final FilePermission[] EMPTY_FILE_PERMISSIONS = new FilePermission[0];

        private final Path file;
        private final OpenOption[] options;
        @SuppressWarnings("removal")
        private final AccessControlContext acc;
        private final FilePermission[] filePermissions;
        private final boolean isDefaultFS;
        private final CompletableFuture<Path> result = new MinimalFuture<>();

        private final AtomicBoolean subscribed = new AtomicBoolean();
        private volatile Flow.Subscription subscription;
        private volatile FileChannel out;

        private static final String pathForSecurityCheck(Path path) {
            return path.toFile().getPath();
        }

        /**
         * Factory for creating PathSubscriber.
         *
         * Permission checks are performed here before construction of the
         * PathSubscriber. Permission checking and construction are deliberately
         * and tightly co-located.
         */
        public static PathSubscriber create(Path file,
                                            List<OpenOption> options) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            FilePermission filePermission = null;
            if (sm != null) {
                try {
                    String fn = pathForSecurityCheck(file);
                    FilePermission writePermission = new FilePermission(fn, "write");
                    sm.checkPermission(writePermission);
                    filePermission = writePermission;
                } catch (UnsupportedOperationException ignored) {
                }
            }

            assert filePermission == null || filePermission.getActions().equals("write");
            @SuppressWarnings("removal")
            AccessControlContext acc = sm != null ? AccessController.getContext() : null;
            return new PathSubscriber(file, options, acc, filePermission);
        }

        /*package-private*/ PathSubscriber(Path file,
                                           List<OpenOption> options,
                                           @SuppressWarnings("removal") AccessControlContext acc,
                                           FilePermission... filePermissions) {
            this.file = file;
            this.options = options.stream().toArray(OpenOption[]::new);
            this.acc = acc;
            this.filePermissions = filePermissions == null || filePermissions[0] == null
                            ? EMPTY_FILE_PERMISSIONS : filePermissions;
            this.isDefaultFS = isDefaultFS(file);
        }

        private static boolean isDefaultFS(Path file) {
            try {
                file.toFile();
                return true;
            } catch (UnsupportedOperationException uoe) {
                return false;
            }
        }

        @SuppressWarnings("removal")
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (!subscribed.compareAndSet(false, true)) {
                subscription.cancel();
                return;
            }

            this.subscription = subscription;
            if (acc == null) {
                try {
                    out = FileChannel.open(file, options);
                } catch (IOException ioe) {
                    result.completeExceptionally(ioe);
                    subscription.cancel();
                    return;
                }
            } else {
                try {
                    PrivilegedExceptionAction<FileChannel> pa =
                            () -> FileChannel.open(file, options);
                    out = isDefaultFS
                            ? AccessController.doPrivileged(pa, acc, filePermissions)
                            : AccessController.doPrivileged(pa, acc);
                } catch (PrivilegedActionException pae) {
                    Throwable t = pae.getCause() != null ? pae.getCause() : pae;
                    result.completeExceptionally(t);
                    subscription.cancel();
                    return;
                } catch (Exception e) {
                    result.completeExceptionally(e);
                    subscription.cancel();
                    return;
                }
            }
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            try {
                ByteBuffer[] buffers = items.toArray(Utils.EMPTY_BB_ARRAY);
                do {
                    out.write(buffers);
                } while (Utils.hasRemaining(buffers));
            } catch (IOException ex) {
                close();
                subscription.cancel();
                result.completeExceptionally(ex);
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable e) {
            result.completeExceptionally(e);
            close();
        }

        @Override
        public void onComplete() {
            close();
            result.complete(file);
        }

        @Override
        public CompletionStage<Path> getBody() {
            return result;
        }

        @SuppressWarnings("removal")
        private void close() {
            if (acc == null) {
                Utils.close(out);
            } else {
                PrivilegedAction<Void> pa = () -> {
                    Utils.close(out);
                    return null;
                };
                if (isDefaultFS) {
                    AccessController.doPrivileged(pa, acc, filePermissions);
                } else {
                    AccessController.doPrivileged(pa, acc);
                }
            }
        }
    }

    public static class ByteArraySubscriber<T> implements TrustedSubscriber<T> {
        private final Function<byte[], T> finisher;
        private final CompletableFuture<T> result = new MinimalFuture<>();
        private final List<ByteBuffer> received = new ArrayList<>();

        private volatile Flow.Subscription subscription;

        public ByteArraySubscriber(Function<byte[],T> finisher) {
            this.finisher = finisher;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            assert Utils.hasRemaining(items);
            received.addAll(items);
        }

        @Override
        public void onError(Throwable throwable) {
            received.clear();
            result.completeExceptionally(throwable);
        }

        private static byte[] join(List<ByteBuffer> bytes) {
            int size = Utils.remaining(bytes, Integer.MAX_VALUE);
            byte[] res = new byte[size];
            int from = 0;
            for (ByteBuffer b : bytes) {
                int l = b.remaining();
                b.get(res, from, l);
                from += l;
            }
            return res;
        }

        @Override
        public void onComplete() {
            try {
                result.complete(finisher.apply(join(received)));
                received.clear();
            } catch (IllegalArgumentException e) {
                result.completeExceptionally(e);
            }
        }

        @Override
        public CompletionStage<T> getBody() {
            return result;
        }
    }

    /**
     * An InputStream built on top of the Flow API.
     */
    public static class HttpResponseInputStream extends InputStream
        implements TrustedSubscriber<InputStream>
    {
        static final int MAX_BUFFERS_IN_QUEUE = 1;  

        private static final ByteBuffer LAST_BUFFER = ByteBuffer.wrap(new byte[0]);
        private static final List<ByteBuffer> LAST_LIST = List.of(LAST_BUFFER);
        private static final Logger debug =
                Utils.getDebugLogger("HttpResponseInputStream"::toString, Utils.DEBUG);

        private final BlockingQueue<List<ByteBuffer>> buffers;
        private volatile Flow.Subscription subscription;
        private volatile boolean closed;
        private volatile Throwable failed;
        private volatile Iterator<ByteBuffer> currentListItr;
        private volatile ByteBuffer currentBuffer;
        private final AtomicBoolean subscribed = new AtomicBoolean();

        public HttpResponseInputStream() {
            this(MAX_BUFFERS_IN_QUEUE);
        }

        HttpResponseInputStream(int maxBuffers) {
            int capacity = (maxBuffers <= 0 ? MAX_BUFFERS_IN_QUEUE : maxBuffers);
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

                        if (debug.on()) debug.log("Taking list of Buffers");
                        List<ByteBuffer> lb = buffers.take();
                        currentListItr = lb.iterator();
                        if (debug.on()) debug.log("List of Buffers Taken");

                        if (closed || failed != null)
                            throw new IOException("closed", failed);

                        if (lb == LAST_LIST) {
                            currentListItr = null;
                            currentBuffer = LAST_BUFFER;
                            break;
                        }

                        Flow.Subscription s = subscription;
                        if (s != null) {
                            if (debug.on()) debug.log("Increased demand by 1");
                            s.request(1);
                        }
                        assert currentListItr != null;
                        if (lb.isEmpty()) continue;
                    }
                    assert currentListItr != null;
                    assert currentListItr.hasNext();
                    if (debug.on()) debug.log("Next Buffer");
                    currentBuffer = currentListItr.next();
                } catch (InterruptedException ex) {
                    try {
                        close();
                    } catch (IOException ignored) {
                    }
                    Thread.currentThread().interrupt();
                    throw new IOException(ex);
                }
            }
            assert currentBuffer == LAST_BUFFER || currentBuffer.hasRemaining();
            return currentBuffer;
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, bytes.length);
            if (len == 0) {
                return 0;
            }
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
        public int available() throws IOException {
            if (closed) return 0;
            int available = 0;
            ByteBuffer current = currentBuffer;
            if (current == LAST_BUFFER) return 0;
            if (current != null) available = current.remaining();
            if (available != 0) return available;
            Iterator<?> iterator = currentListItr;
            if (iterator != null && iterator.hasNext()) return 1;
            if (!buffers.isEmpty() && buffers.peek() != LAST_LIST ) return 1;
            return available;
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            Objects.requireNonNull(s);
            if (debug.on()) debug.log("onSubscribe called");
            try {
                if (!subscribed.compareAndSet(false, true)) {
                    if (debug.on()) debug.log("Already subscribed: canceling");
                    s.cancel();
                } else {
                    boolean closed;
                    synchronized (this) {
                        closed = this.closed;
                        if (!closed) {
                            this.subscription = s;
                            assert buffers.remainingCapacity() > 1 || failed != null
                                    : "buffers capacity: " + buffers.remainingCapacity()
                                    + ", closed: " + closed + ", terminated: "
                                    + buffers.contains(LAST_LIST)
                                    + ", failed: " + failed;
                        }
                    }
                    if (closed) {
                        if (debug.on()) debug.log("Already closed: canceling");
                        s.cancel();
                        return;
                    }
                    if (debug.on())
                        debug.log("onSubscribe: requesting "
                                  + Math.max(1, buffers.remainingCapacity() - 1));
                    s.request(Math.max(1, buffers.remainingCapacity() - 1));
                }
            } catch (Throwable t) {
                failed = t;
                if (debug.on())
                    debug.log("onSubscribe failed", t);
                try {
                    close();
                } catch (IOException x) {
                } finally {
                    onError(t);
                }
            }
        }

        @Override
        public void onNext(List<ByteBuffer> t) {
            Objects.requireNonNull(t);
            try {
                if (debug.on()) debug.log("next item received");
                if (!buffers.offer(t)) {
                    throw new IllegalStateException("queue is full");
                }
                if (debug.on()) debug.log("item offered");
            } catch (Throwable ex) {
                failed = ex;
                try {
                    close();
                } catch (IOException ex1) {
                } finally {
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable thrwbl) {
            if (debug.on())
                debug.log("onError called: " + thrwbl);
            subscription = null;
            failed = Objects.requireNonNull(thrwbl);
            buffers.offer(LAST_LIST);
        }

        @Override
        public void onComplete() {
            if (debug.on())
                debug.log("onComplete called");
            subscription = null;
            onNext(LAST_LIST);
        }

        @Override
        public void close() throws IOException {
            Flow.Subscription s;
            synchronized (this) {
                if (closed) return;
                closed = true;
                s = subscription;
                subscription = null;
            }
            if (debug.on())
                debug.log("close called");
            try {
                if (s != null) {
                    s.cancel();
                }
            } finally {
                buffers.offer(LAST_LIST);
                super.close();
            }
        }

    }

    public static BodySubscriber<Stream<String>> createLineStream() {
        return createLineStream(UTF_8);
    }

    public static BodySubscriber<Stream<String>> createLineStream(Charset charset) {
        Objects.requireNonNull(charset);
        BodySubscriber<InputStream> s = new HttpResponseInputStream();
        return new MappingSubscriber<InputStream,Stream<String>>(s,
            (InputStream stream) -> {
                return new BufferedReader(new InputStreamReader(stream, charset))
                            .lines().onClose(() -> Utils.close(stream));
            }, true);
    }

    /**
     * Currently this consumes all of the data and ignores it
     */
    public static class NullSubscriber<T> implements TrustedSubscriber<T> {

        private final CompletableFuture<T> cf = new MinimalFuture<>();
        private final Optional<T> result;
        private final AtomicBoolean subscribed = new AtomicBoolean();

        public NullSubscriber(Optional<T> result) {
            this.result = result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (!subscribed.compareAndSet(false, true)) {
                subscription.cancel();
            } else {
                subscription.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            Objects.requireNonNull(items);
        }

        @Override
        public void onError(Throwable throwable) {
            Objects.requireNonNull(throwable);
            cf.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            if (result.isPresent()) {
                cf.complete(result.get());
            } else {
                cf.complete(null);
            }
        }

        @Override
        public CompletionStage<T> getBody() {
            return cf;
        }
    }

    /** An adapter between {@code BodySubscriber} and {@code Flow.Subscriber}. */
    public static final class SubscriberAdapter<S extends Subscriber<? super List<ByteBuffer>>,R>
        implements TrustedSubscriber<R>
    {
        private final CompletableFuture<R> cf = new MinimalFuture<>();
        private final S subscriber;
        private final Function<? super S,? extends R> finisher;
        private volatile Subscription subscription;

        public SubscriberAdapter(S subscriber, Function<? super S,? extends R> finisher) {
            this.subscriber = Objects.requireNonNull(subscriber);
            this.finisher = Objects.requireNonNull(finisher);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (this.subscription != null) {
                subscription.cancel();
            } else {
                this.subscription = subscription;
                subscriber.onSubscribe(subscription);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            Objects.requireNonNull(item);
            try {
                subscriber.onNext(item);
            } catch (Throwable throwable) {
                subscription.cancel();
                onError(throwable);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            Objects.requireNonNull(throwable);
            try {
                subscriber.onError(throwable);
            } finally {
                cf.completeExceptionally(throwable);
            }
        }

        @Override
        public void onComplete() {
            try {
                subscriber.onComplete();
            } finally {
                try {
                    cf.completeAsync(() -> finisher.apply(subscriber));
                } catch (Throwable throwable) {
                    cf.completeExceptionally(throwable);
                }
            }
        }

        @Override
        public CompletionStage<R> getBody() {
            return cf;
        }
    }

    /**
     * A body subscriber which receives input from an upstream subscriber
     * and maps that subscriber's body type to a new type. The upstream subscriber
     * delegates all flow operations directly to this object. The
     * {@link CompletionStage} returned by {@link #getBody()}} takes the output
     * of the upstream {@code getBody()} and applies the mapper function to
     * obtain the new {@code CompletionStage} type.
     *
     * @param <T> the upstream body type
     * @param <U> this subscriber's body type
     */
    public static class MappingSubscriber<T,U> implements TrustedSubscriber<U> {
        private final BodySubscriber<T> upstream;
        private final Function<? super T,? extends U> mapper;
        private final boolean trusted;

        public MappingSubscriber(BodySubscriber<T> upstream,
                                 Function<? super T,? extends U> mapper) {
            this(upstream, mapper, false);
        }

        MappingSubscriber(BodySubscriber<T> upstream,
                          Function<? super T,? extends U> mapper,
                          boolean trusted) {
            this.upstream = Objects.requireNonNull(upstream);
            this.mapper = Objects.requireNonNull(mapper);
            this.trusted = trusted;
        }

        @Override
        public boolean needsExecutor() {
            return !trusted || TrustedSubscriber.needsExecutor(upstream);
        }

        @Override
        public CompletionStage<U> getBody() {
            return upstream.getBody().thenApply(mapper);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            upstream.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            upstream.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            upstream.onError(throwable);
        }

        @Override
        public void onComplete() {
            upstream.onComplete();
        }
    }

    static class PublishingBodySubscriber
            implements TrustedSubscriber<Flow.Publisher<List<ByteBuffer>>> {
        private final MinimalFuture<Flow.Subscription>
                subscriptionCF = new MinimalFuture<>();
        private final MinimalFuture<SubscriberRef>
                subscribedCF = new MinimalFuture<>();
        private AtomicReference<SubscriberRef>
                subscriberRef = new AtomicReference<>();
        private final CompletionStage<Flow.Publisher<List<ByteBuffer>>> body =
                subscriptionCF.thenCompose(
                        (s) -> MinimalFuture.completedFuture(this::subscribe));

        private final MinimalFuture<Void> completionCF;
        private PublishingBodySubscriber() {
            completionCF = new MinimalFuture<>();
            completionCF.whenComplete(
                    (r,t) -> subscribedCF.thenAccept( s -> complete(s, t)));
        }

        static final class SubscriberRef {
            volatile Flow.Subscriber<? super List<ByteBuffer>> ref;
            SubscriberRef(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
                ref = subscriber;
            }
            Flow.Subscriber<? super List<ByteBuffer>> get() {
                return ref;
            }
            Flow.Subscriber<? super List<ByteBuffer>> clear() {
                Flow.Subscriber<? super List<ByteBuffer>> res = ref;
                ref = null;
                return res;
            }
        }

        static final class SubscriptionRef implements Flow.Subscription {
            final Flow.Subscription subscription;
            final SubscriberRef subscriberRef;
            SubscriptionRef(Flow.Subscription subscription,
                            SubscriberRef subscriberRef) {
                this.subscription = subscription;
                this.subscriberRef = subscriberRef;
            }
            @Override
            public void request(long n) {
                if (subscriberRef.get() != null) {
                    subscription.request(n);
                }
            }
            @Override
            public void cancel() {
                subscription.cancel();
                subscriberRef.clear();
            }

            void subscribe() {
                Subscriber<?> subscriber = subscriberRef.get();
                if (subscriber != null) {
                    subscriber.onSubscribe(this);
                }
            }

            @Override
            public String toString() {
                return "SubscriptionRef/"
                        + subscription.getClass().getName()
                        + "@"
                        + System.identityHashCode(subscription);
            }
        }

        private void complete(SubscriberRef ref, Throwable t) {
            assert ref != null;
            Subscriber<?> s = ref.clear();
            if (s == null) return;
            if (t == null) {
                try {
                    s.onComplete();
                } catch (Throwable x) {
                    s.onError(x);
                }
            } else {
                s.onError(t);
            }
        }

        private void signalError(Throwable err) {
            if (err == null) {
                err = new NullPointerException("null throwable");
            }
            completionCF.completeExceptionally(err);
        }

        private void signalComplete() {
            completionCF.complete(null);
        }

        private void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            Objects.requireNonNull(subscriber, "subscriber must not be null");
            SubscriberRef ref = new SubscriberRef(subscriber);
            if (subscriberRef.compareAndSet(null, ref)) {
                subscriptionCF.thenAccept((s) -> {
                    SubscriptionRef subscription = new SubscriptionRef(s,ref);
                    try {
                        subscription.subscribe();
                        subscribedCF.complete(ref);
                    } catch (Throwable t) {
                        if (Log.errors()) {
                            Log.logError("Failed to call onSubscribe: " +
                                    "cancelling subscription: " + t);
                            Log.logError(t);
                        }
                        subscription.cancel();
                    }
                });
            } else {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override public void request(long n) { }
                    @Override public void cancel() { }
                });
                subscriber.onError(new IllegalStateException(
                        "This publisher has already one subscriber"));
            }
        }

        private final AtomicBoolean subscribed = new AtomicBoolean();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (!subscribed.compareAndSet(false, true)) {
                subscription.cancel();
            } else {
                subscriptionCF.complete(subscription);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            Objects.requireNonNull(item);
            try {
                assert subscriptionCF.isDone();
                SubscriberRef ref = subscriberRef.get();
                assert ref != null;
                Flow.Subscriber<? super List<ByteBuffer>>
                        subscriber = ref.get();
                if (subscriber != null) {
                    subscriber.onNext(item);
                }
            } catch (Throwable err) {
                signalError(err);
                subscriptionCF.thenAccept(s -> s.cancel());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            assert suppress(subscriptionCF.isDone(),
                    "onError called before onSubscribe",
                    throwable);
            signalError(throwable);
            Objects.requireNonNull(throwable);
        }

        @Override
        public void onComplete() {
            if (!subscriptionCF.isDone()) {
                signalError(new InternalError(
                        "onComplete called before onSubscribed"));
            } else {
                signalComplete();
            }
        }

        @Override
        public CompletionStage<Flow.Publisher<List<ByteBuffer>>> getBody() {
            return body;
        }

        private boolean suppress(boolean condition,
                                 String assertion,
                                 Throwable carrier) {
            if (!condition) {
                if (carrier != null) {
                    carrier.addSuppressed(new AssertionError(assertion));
                } else if (Log.errors()) {
                    Log.logError(new AssertionError(assertion));
                }
            }
            return true;
        }

    }

    public static BodySubscriber<Flow.Publisher<List<ByteBuffer>>>
    createPublisher() {
        return new PublishingBodySubscriber();
    }


    /**
     * Tries to determine whether bs::getBody must be invoked asynchronously,
     * and if so, uses the provided executor to do it.
     * If the executor is a {@link HttpClientImpl.DelegatingExecutor},
     * uses the executor's delegate.
     * @param e    The executor to use if an executor is required.
     * @param bs   The BodySubscriber (trusted or not)
     * @param <T>  The type of the response.
     * @return A completion stage that completes when the completion
     *         stage returned by bs::getBody completes. This may, or
     *         may not, be the same completion stage.
     */
    public static <T> CompletionStage<T> getBodyAsync(Executor e, BodySubscriber<T> bs) {
        if (TrustedSubscriber.needsExecutor(bs)) {
            return getBodyAsync(e, bs, new MinimalFuture<>());
        } else {
            return bs.getBody();
        }
    }

    /**
     * Invokes bs::getBody using the provided executor.
     * If invoking bs::getBody requires an executor, and the given executor
     * is a {@link HttpClientImpl.DelegatingExecutor}, then the executor's
     * delegate is used. If an error occurs anywhere then the given {@code cf}
     * is completed exceptionally (this method does not throw).
     * @param e   The executor that should be used to call bs::getBody
     * @param bs  The BodySubscriber
     * @param cf  A completable future that this function will set up
     *            to complete when the completion stage returned by
     *            bs::getBody completes.
     *            In case of any error while trying to set up the
     *            completion chain, {@code cf} will be completed
     *            exceptionally with that error.
     * @param <T> The response type.
     * @return The provided {@code cf}.
     */
    public static <T> CompletableFuture<T> getBodyAsync(Executor e,
                                                      BodySubscriber<T> bs,
                                                      CompletableFuture<T> cf) {
        return getBodyAsync(e, bs, cf, cf::completeExceptionally);
    }

    /**
     * Invokes bs::getBody using the provided executor.
     * If invoking bs::getBody requires an executor, and the given executor
     * is a {@link HttpClientImpl.DelegatingExecutor}, then the executor's
     * delegate is used.
     * The provided {@code cf} is completed with the result (exceptional
     * or not) of the completion stage returned by bs::getBody.
     * If an error occurs when trying to set up the
     * completion chain, the provided {@code errorHandler} is invoked,
     * but {@code cf} is not necessarily affected.
     * This method does not throw.
     * @param e   The executor that should be used to call bs::getBody
     * @param bs  The BodySubscriber
     * @param cf  A completable future that this function will set up
     *            to complete when the completion stage returned by
     *            bs::getBody completes.
     *            In case of any error while trying to set up the
     *            completion chain, {@code cf} will be completed
     *            exceptionally with that error.
     * @param errorHandler The handler to invoke if an error is raised
     *                     while trying to set up the completion chain.
     * @param <T> The response type.
     * @return The provide {@code cf}. If the {@code errorHandler} is
     * invoked, it is the responsibility of the {@code errorHandler} to
     * complete the {@code cf}, if needed.
     */
    public static <T> CompletableFuture<T> getBodyAsync(Executor e,
                                                      BodySubscriber<T> bs,
                                                      CompletableFuture<T> cf,
                                                      Consumer<Throwable> errorHandler) {
        assert errorHandler != null;
        try {
            assert e != null;
            assert cf != null;

            if (TrustedSubscriber.needsExecutor(bs)) {
                e = (e instanceof DelegatingExecutor exec)
                        ? exec::ensureExecutedAsync : e;
            }

            e.execute(() -> {
                try {
                    bs.getBody().whenComplete((r, t) -> {
                        if (t != null) {
                            cf.completeExceptionally(t);
                        } else {
                            cf.complete(r);
                        }
                    });
                } catch (Throwable t) {
                    errorHandler.accept(t);
                }
            });
            return cf;

        } catch (Throwable t) {
            errorHandler.accept(t);
        }
        return cf;
    }
}
