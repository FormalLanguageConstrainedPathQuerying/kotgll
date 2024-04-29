/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import jdk.internal.net.http.common.Deadline;
import jdk.internal.net.http.common.FlowTube;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.TimeLine;
import jdk.internal.net.http.common.TimeSource;
import jdk.internal.net.http.common.Utils;
import static jdk.internal.net.http.HttpClientImpl.KEEP_ALIVE_TIMEOUT; 

/**
 * Http 1.1 connection pool.
 */
final class ConnectionPool {

    static final long MAX_POOL_SIZE = Utils.getIntegerNetProperty(
            "jdk.httpclient.connectionPoolSize", 0); 
    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);


    private final ReentrantLock stateLock = new ReentrantLock();
    private final HashMap<CacheKey,LinkedList<HttpConnection>> plainPool;
    private final HashMap<CacheKey,LinkedList<HttpConnection>> sslPool;
    private final ExpiryList expiryList;
    private final String dbgTag; 
    private final TimeLine timeSource;
    volatile boolean stopped;

    /**
     * Entries in connection pool are keyed by destination address and/or
     * proxy address:
     * case 1: plain TCP not via proxy (destination IP only)
     * case 2: plain TCP via proxy (proxy only)
     * case 3: SSL not via proxy (destination IP+hostname only)
     * case 4: SSL over tunnel (destination IP+hostname and proxy)
     */
    static class CacheKey {
        final InetSocketAddress proxy;
        final InetSocketAddress destination;
        final boolean secure;

        private CacheKey(boolean secure, InetSocketAddress destination,
                         InetSocketAddress proxy) {
            this.proxy = proxy;
            this.destination = destination;
            this.secure = secure;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CacheKey other = (CacheKey) obj;
            if (this.secure != other.secure) {
                return false;
            }
            if (!Objects.equals(this.proxy, other.proxy)) {
                return false;
            }
            if (!Objects.equals(this.destination, other.destination)) {
                return false;
            }
            if (secure && destination != null) {
                String hostString = destination.getHostString();
                if (hostString == null || !hostString.equalsIgnoreCase(
                        other.destination.getHostString())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(proxy, destination);
        }
    }

    ConnectionPool(long clientId) {
        this(clientId, TimeSource.source());
    }

    ConnectionPool(long clientId, TimeLine timeSource) {
        this("ConnectionPool("+clientId+")", Objects.requireNonNull(timeSource));
    }

        /**
         * There should be one of these per HttpClient.
         */
    private ConnectionPool(String tag, TimeLine timeSource) {
        dbgTag = tag;
        plainPool = new HashMap<>();
        sslPool = new HashMap<>();
        this.timeSource = timeSource;
        this.expiryList = new ExpiryList(timeSource);
    }

    final String dbgString() {
        return dbgTag;
    }

    void start() {
        assert !stopped : "Already stopped";
    }

    static CacheKey cacheKey(boolean secure, InetSocketAddress destination,
                             InetSocketAddress proxy)
    {
        return new CacheKey(secure, destination, proxy);
    }

    HttpConnection getConnection(boolean secure,
                                 InetSocketAddress addr,
                                 InetSocketAddress proxy) {
        if (stopped) return null;
        stateLock.lock();
        try {
            return getConnection0(secure, addr, proxy);
        } finally {
            stateLock.unlock();
        }
    }

    private HttpConnection getConnection0(boolean secure,
                                          InetSocketAddress addr,
                                          InetSocketAddress proxy) {
        if (stopped) return null;
        addr = secure || proxy == null ? addr : null;
        CacheKey key = new CacheKey(secure, addr, proxy);
        HttpConnection c = secure ? findConnection(key, sslPool)
                                  : findConnection(key, plainPool);
        assert c == null || c.isSecure() == secure;
        return c;
    }

    /**
     * Returns the connection to the pool.
     */
    void returnToPool(HttpConnection conn) {
        returnToPool(conn, timeSource.instant(), KEEP_ALIVE_TIMEOUT);
    }

    void returnToPool(HttpConnection conn, Deadline now, long keepAlive) {

        assert (conn instanceof PlainHttpConnection) || conn.isSecure()
            : "Attempting to return unsecure connection to SSL pool: "
                + conn.getClass();

        CleanupTrigger cleanup = registerCleanupTrigger(conn);

        HttpConnection toClose = null;
        stateLock.lock();
        try {
            if (cleanup.isDone()) {
                return;
            } else if (stopped) {
                conn.close();
                return;
            }
            if (MAX_POOL_SIZE > 0 && expiryList.size() >= MAX_POOL_SIZE) {
                toClose = expiryList.removeOldest();
                if (toClose != null) removeFromPool(toClose);
            }
            if (conn instanceof PlainHttpConnection) {
                putConnection(conn, plainPool);
            } else {
                assert conn.isSecure();
                putConnection(conn, sslPool);
            }
            expiryList.add(conn, now, keepAlive);
        } finally {
            stateLock.unlock();
        }
        if (toClose != null) {
            if (debug.on()) {
                debug.log("Maximum pool size reached: removing oldest connection %s",
                          toClose.dbgString());
            }
            close(toClose);
        }
    }

    private CleanupTrigger registerCleanupTrigger(HttpConnection conn) {
        CleanupTrigger cleanup = new CleanupTrigger(conn);
        FlowTube flow = conn.getConnectionFlow();
        if (debug.on()) debug.log("registering %s", cleanup);
        flow.connectFlows(cleanup, cleanup);
        return cleanup;
    }

    private HttpConnection
    findConnection(CacheKey key,
                   HashMap<CacheKey,LinkedList<HttpConnection>> pool) {
        LinkedList<HttpConnection> l = pool.get(key);
        if (l == null || l.isEmpty()) {
            return null;
        } else {
            HttpConnection c = l.removeFirst();
            expiryList.remove(c);
            return c;
        }
    }

    /* called from cache cleaner only  */
    private boolean
    removeFromPool(HttpConnection c,
                   HashMap<CacheKey,LinkedList<HttpConnection>> pool) {
        assert stateLock.isHeldByCurrentThread();
        CacheKey k = c.cacheKey();
        List<HttpConnection> l = pool.get(k);
        if (l == null || l.isEmpty()) {
            pool.remove(k);
            return false;
        }
        return l.remove(c);
    }

    private void
    putConnection(HttpConnection c,
                  HashMap<CacheKey,LinkedList<HttpConnection>> pool) {
        CacheKey key = c.cacheKey();
        LinkedList<HttpConnection> l = pool.get(key);
        if (l == null) {
            l = new LinkedList<>();
            pool.put(key, l);
        }
        l.add(c);
    }

    /**
     * Purge expired connection and return the number of milliseconds
     * in which the next connection is scheduled to expire.
     * If no connections are scheduled to be purged return 0.
     * @return the delay in milliseconds in which the next connection will
     *         expire.
     */
    long purgeExpiredConnectionsAndReturnNextDeadline() {
        if (!expiryList.purgeMaybeRequired()) return 0;
        return purgeExpiredConnectionsAndReturnNextDeadline(timeSource.instant());
    }

    long purgeExpiredConnectionsAndReturnNextDeadline(Deadline now) {
        long nextPurge = 0;

        if (!expiryList.purgeMaybeRequired()) return nextPurge;

        List<HttpConnection> closelist;
        stateLock.lock();
        try {
            closelist = expiryList.purgeUntil(now);
            for (HttpConnection c : closelist) {
                if (c instanceof PlainHttpConnection) {
                    boolean wasPresent = removeFromPool(c, plainPool);
                    assert wasPresent;
                } else {
                    boolean wasPresent = removeFromPool(c, sslPool);
                    assert wasPresent;
                }
            }
            nextPurge = now.until(
                    expiryList.nextExpiryDeadline().orElse(now),
                    ChronoUnit.MILLIS);
        } finally {
            stateLock.unlock();
        }
        closelist.forEach(this::close);
        return nextPurge;
    }

    private void close(HttpConnection c) {
        try {
            c.close();
        } catch (Throwable e) {} 
    }

    void stop() {
        List<HttpConnection> closelist = Collections.emptyList();
        try {
            stateLock.lock();
            try {
                stopped = true;
                closelist = expiryList.stream()
                    .map(e -> e.connection)
                    .collect(Collectors.toList());
                expiryList.clear();
                plainPool.clear();
                sslPool.clear();
            } finally {
                stateLock.unlock();
            }
        } finally {
            closelist.forEach(this::close);
        }
    }

    static final class ExpiryEntry {
        final HttpConnection connection;
        final Deadline expiry; 
        ExpiryEntry(HttpConnection connection, Deadline expiry) {
            this.connection = connection;
            this.expiry = expiry;
        }
    }

    /**
     * Manages a LinkedList of sorted ExpiryEntry. The entry with the closer
     * deadline is at the tail of the list, and the entry with the farther
     * deadline is at the head. In the most common situation, new elements
     * will need to be added at the head (or close to it), and expired elements
     * will need to be purged from the tail.
     */
    private static final class ExpiryList {
        private final LinkedList<ExpiryEntry> list = new LinkedList<>();
        private final TimeLine timeSource;
        private volatile boolean mayContainEntries;

        ExpiryList(TimeLine timeSource) {
            this.timeSource = timeSource;
        }

        int size() { return list.size(); }

        boolean purgeMaybeRequired() {
            return mayContainEntries;
        }

        Optional<Deadline> nextExpiryDeadline() {
            if (list.isEmpty()) return Optional.empty();
            else return Optional.of(list.getLast().expiry);
        }

        HttpConnection removeOldest() {
            ExpiryEntry entry = list.pollLast();
            return entry == null ? null : entry.connection;
        }

        void add(HttpConnection conn) {
            add(conn, timeSource.instant(), KEEP_ALIVE_TIMEOUT);
        }

        void add(HttpConnection conn, Deadline now, long keepAlive) {
            Deadline then = now.truncatedTo(ChronoUnit.SECONDS)
                    .plus(keepAlive, ChronoUnit.SECONDS);

            ListIterator<ExpiryEntry> li = list.listIterator();
            while (li.hasNext()) {
                ExpiryEntry entry = li.next();

                if (then.isAfter(entry.expiry)) {
                    li.previous();
                    li.add(new ExpiryEntry(conn, then));
                    mayContainEntries = true;
                    return;
                }
            }
            list.add(new ExpiryEntry(conn, then));
            mayContainEntries = true;
        }

        void remove(HttpConnection c) {
            if (c == null || list.isEmpty()) return;
            ListIterator<ExpiryEntry> li = list.listIterator();
            while (li.hasNext()) {
                ExpiryEntry e = li.next();
                if (e.connection.equals(c)) {
                    li.remove();
                    mayContainEntries = !list.isEmpty();
                    return;
                }
            }
        }

        List<HttpConnection> purgeUntil(Deadline now) {
            if (list.isEmpty()) return Collections.emptyList();

            List<HttpConnection> closelist = new ArrayList<>();

            Iterator<ExpiryEntry> li = list.descendingIterator();
            while (li.hasNext()) {
                ExpiryEntry entry = li.next();
                if (!entry.expiry.isAfter(now)) {
                    li.remove();
                    HttpConnection c = entry.connection;
                    closelist.add(c);
                } else break; 
            }
            mayContainEntries = !list.isEmpty();
            return closelist;
        }

        java.util.stream.Stream<ExpiryEntry> stream() {
            return list.stream();
        }

        void clear() {
            list.clear();
            mayContainEntries = false;
        }
    }

    private void removeFromPool(HttpConnection c) {
        assert stateLock.isHeldByCurrentThread();
        if (c instanceof PlainHttpConnection) {
            removeFromPool(c, plainPool);
        } else {
            assert c.isSecure() : "connection " + c + " is not secure!";
            removeFromPool(c, sslPool);
        }
    }

    boolean contains(HttpConnection c) {
        stateLock.lock();
        try {
            return contains0(c);
        } finally {
            stateLock.unlock();
        }
    }

    private boolean contains0(HttpConnection c) {
        final CacheKey key = c.cacheKey();
        List<HttpConnection> list;
        if ((list = plainPool.get(key)) != null) {
            if (list.contains(c)) return true;
        }
        if ((list = sslPool.get(key)) != null) {
            if (list.contains(c)) return true;
        }
        return false;
    }

    void cleanup(HttpConnection c, Throwable error) {
        if (debug.on())
            debug.log("%s : ConnectionPool.cleanup(%s)",
                    String.valueOf(c.getConnectionFlow()), error);
        stateLock.lock();
        try {
            removeFromPool(c);
            expiryList.remove(c);
        } finally {
            stateLock.unlock();
        }
        c.close();
    }

    /**
     * An object that subscribes to the flow while the connection is in
     * the pool. Anything that comes in will cause the connection to be closed
     * and removed from the pool.
     */
    private final class CleanupTrigger implements
            FlowTube.TubeSubscriber, FlowTube.TubePublisher,
            Flow.Subscription {

        private final HttpConnection connection;
        private volatile boolean done;

        public CleanupTrigger(HttpConnection connection) {
            this.connection = connection;
        }

        public boolean isDone() { return done;}

        private void triggerCleanup(Throwable error) {
            done = true;
            cleanup(connection, error);
        }

        @Override public void request(long n) {}
        @Override public void cancel() {}

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(1);
        }
        @Override
        public void onError(Throwable error) { triggerCleanup(error); }
        @Override
        public void onComplete() { triggerCleanup(null); }
        @Override
        public void onNext(List<ByteBuffer> item) {
            triggerCleanup(new IOException("Data received while in pool"));
        }

        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            subscriber.onSubscribe(this);
        }

        @Override
        public String toString() {
            return "CleanupTrigger(" + connection.getConnectionFlow() + ")";
        }
    }
}
