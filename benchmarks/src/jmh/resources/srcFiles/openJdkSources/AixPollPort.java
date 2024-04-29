/*
 * Copyright (c) 2008, 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012 SAP SE. All rights reserved.
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

import java.nio.channels.spi.AsynchronousChannelProvider;
import sun.nio.ch.Pollset;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AsynchronousChannelGroup implementation based on the AIX pollset framework.
 */
final class AixPollPort
    extends Port
{
    static {
        IOUtil.load();
        Pollset.init();
    }

    private final int pollset;

    private boolean closed;

    private final int sp[];

    private final int ctlSp[];

    private final AtomicInteger wakeupCount = new AtomicInteger();

    private final long address;

    private static final int MAX_EVENTS_TO_POLL = 512;

    static class Event {
        final PollableChannel channel;
        final int events;

        Event(PollableChannel channel, int events) {
            this.channel = channel;
            this.events = events;
        }

        PollableChannel channel()   { return channel; }
        int events()                { return events; }
    }

    private final ArrayBlockingQueue<Event> queue;
    private final Event NEED_TO_POLL = new Event(null, 0);
    private final Event EXECUTE_TASK_OR_SHUTDOWN = new Event(null, 0);
    private final Event CONTINUE_AFTER_CTL_EVENT = new Event(null, 0);

    static class ControlEvent {
        final int fd;
        final int events;
        final boolean removeOnly;
        int error = 0;

        ControlEvent(int fd, int events, boolean removeOnly) {
            this.fd = fd;
            this.events = events;
            this.removeOnly = removeOnly;
        }

        int fd()                 { return fd; }
        int events()             { return events; }
        boolean removeOnly()     { return removeOnly; }
        int error()              { return error; }
        void setError(int error) { this.error = error; }
    }

    private final HashSet<ControlEvent> controlQueue = new HashSet<ControlEvent>();

    private final ReentrantLock controlLock = new ReentrantLock();

    AixPollPort(AsynchronousChannelProvider provider, ThreadPool pool)
        throws IOException
    {
        super(provider, pool);

        this.pollset = Pollset.pollsetCreate();

        int[] sv = new int[2];
        try {
            Pollset.socketpair(sv);
            Pollset.pollsetCtl(pollset, Pollset.PS_ADD, sv[0], Net.POLLIN);
        } catch (IOException x) {
            Pollset.pollsetDestroy(pollset);
            throw x;
        }
        this.sp = sv;

        sv = new int[2];
        try {
            Pollset.socketpair(sv);
            Pollset.pollsetCtl(pollset, Pollset.PS_ADD, sv[0], Net.POLLIN);
        } catch (IOException x) {
            Pollset.pollsetDestroy(pollset);
            throw x;
        }
        this.ctlSp = sv;

        this.address = Pollset.allocatePollArray(MAX_EVENTS_TO_POLL);

        this.queue = new ArrayBlockingQueue<Event>(MAX_EVENTS_TO_POLL);
        this.queue.offer(NEED_TO_POLL);
    }

    AixPollPort start() {
        startThreads(new EventHandlerTask());
        return this;
    }

    /**
     * Release all resources
     */
    private void implClose() {
        synchronized (this) {
            if (closed)
                return;
            closed = true;
        }
        Pollset.freePollArray(address);
        Pollset.close0(sp[0]);
        Pollset.close0(sp[1]);
        Pollset.close0(ctlSp[0]);
        Pollset.close0(ctlSp[1]);
        Pollset.pollsetDestroy(pollset);
    }

    void wakeup() {
        if (wakeupCount.incrementAndGet() == 1) {
            try {
                Pollset.interrupt(sp[1]);
            } catch (IOException x) {
                throw new AssertionError(x);
            }
        }
    }

    @Override
    void executeOnHandlerTask(Runnable task) {
        synchronized (this) {
            if (closed)
                throw new RejectedExecutionException();
            offerTask(task);
            wakeup();
        }
    }

    @Override
    void shutdownHandlerTasks() {
        /*
         * If no tasks are running then just release resources; otherwise
         * write to the one end of the socketpair to wakeup any polling threads.
         */
        int nThreads = threadCount();
        if (nThreads == 0) {
            implClose();
        } else {
            while (nThreads-- > 0) {
                wakeup();
            }
        }
    }

    @Override
    void startPoll(int fd, int events) {
        queueControlEvent(new ControlEvent(fd, events, false));
    }

    @Override
    protected void preUnregister(int fd) {
        queueControlEvent(new ControlEvent(fd, 0, true));
    }

    private void queueControlEvent(ControlEvent ev) {
        synchronized (controlQueue) {
            controlQueue.add(ev);
            try {
                Pollset.interrupt(ctlSp[1]);
            } catch (IOException x) {
                throw new AssertionError(x);
            }
            do {
                if (controlLock.tryLock()) {
                    try {
                        processControlQueue();
                    } finally {
                        controlLock.unlock();
                    }
                } else {
                    try {
                        controlQueue.wait(100);
                    } catch (InterruptedException e) {
                    }
                }
            } while (controlQueue.contains(ev));
        }
        if (ev.error() != 0) {
            throw new AssertionError();
        }
    }

    private void processControlQueue() {
        synchronized (controlQueue) {
            Iterator<ControlEvent> iter = controlQueue.iterator();
            while (iter.hasNext()) {
                ControlEvent ev = iter.next();
                Pollset.pollsetCtl(pollset, Pollset.PS_DELETE, ev.fd(), 0);
                if (!ev.removeOnly()) {
                    ev.setError(Pollset.pollsetCtl(pollset, Pollset.PS_MOD, ev.fd(), ev.events()));
                }
                iter.remove();
            }
            controlQueue.notifyAll();
        }
    }

    /*
     * Task to process events from pollset and dispatch to the channel's
     * onEvent handler.
     *
     * Events are retrieved from pollset in batch and offered to a BlockingQueue
     * where they are consumed by handler threads. A special "NEED_TO_POLL"
     * event is used to signal one consumer to re-poll when all events have
     * been consumed.
     */
    private class EventHandlerTask implements Runnable {
        private Event poll() throws IOException {
            try {
                for (;;) {
                    int n;
                    controlLock.lock();
                    try {
                        n = Pollset.pollsetPoll(pollset, address,
                                     MAX_EVENTS_TO_POLL, Pollset.PS_NO_TIMEOUT);
                    } finally {
                        controlLock.unlock();
                    }
                    /*
                     * 'n' events have been read. Here we map them to their
                     * corresponding channel in batch and queue n-1 so that
                     * they can be handled by other handler threads. The last
                     * event is handled by this thread (and so is not queued).
                     */
                    fdToChannelLock.readLock().lock();
                    try {
                        while (n-- > 0) {
                            long eventAddress = Pollset.getEvent(address, n);
                            int fd = Pollset.getDescriptor(eventAddress);

                            if (fd != sp[0] && fd != ctlSp[0]) {
                                synchronized (controlQueue) {
                                    Pollset.pollsetCtl(pollset, Pollset.PS_DELETE, fd, 0);
                                }
                            }

                            if (fd == sp[0]) {
                                if (wakeupCount.decrementAndGet() == 0) {
                                    Pollset.drain1(sp[0]);
                                }

                                if (n > 0) {
                                    queue.offer(EXECUTE_TASK_OR_SHUTDOWN);
                                    continue;
                                }
                                return EXECUTE_TASK_OR_SHUTDOWN;
                            }

                            if (fd == ctlSp[0]) {
                                synchronized (controlQueue) {
                                    Pollset.drain1(ctlSp[0]);
                                    processControlQueue();
                                }
                                if (n > 0) {
                                    continue;
                                }
                                return CONTINUE_AFTER_CTL_EVENT;
                            }

                            PollableChannel channel = fdToChannel.get(fd);
                            if (channel != null) {
                                int events = Pollset.getRevents(eventAddress);
                                Event ev = new Event(channel, events);

                                if (n > 0) {
                                    queue.offer(ev);
                                } else {
                                    return ev;
                                }
                            }
                        }
                    } finally {
                        fdToChannelLock.readLock().unlock();
                    }
                }
            } finally {
                queue.offer(NEED_TO_POLL);
            }
        }

        public void run() {
            Invoker.GroupAndInvokeCount myGroupAndInvokeCount =
                Invoker.getGroupAndInvokeCount();
            final boolean isPooledThread = (myGroupAndInvokeCount != null);
            boolean replaceMe = false;
            Event ev;
            try {
                for (;;) {
                    if (isPooledThread)
                        myGroupAndInvokeCount.resetInvokeCount();

                    try {
                        replaceMe = false;
                        ev = queue.take();

                        if (ev == NEED_TO_POLL) {
                            try {
                                ev = poll();
                            } catch (IOException x) {
                                x.printStackTrace();
                                return;
                            }
                        }
                    } catch (InterruptedException x) {
                        continue;
                    }

                    if (ev == CONTINUE_AFTER_CTL_EVENT) {
                        continue;
                    }

                    if (ev == EXECUTE_TASK_OR_SHUTDOWN) {
                        Runnable task = pollTask();
                        if (task == null) {
                            return;
                        }
                        replaceMe = true;
                        task.run();
                        continue;
                    }

                    try {
                        ev.channel().onEvent(ev.events(), isPooledThread);
                    } catch (Error | RuntimeException x) {
                        replaceMe = true;
                        throw x;
                    }
                }
            } finally {
                int remaining = threadExit(this, replaceMe);
                if (remaining == 0 && isShutdown()) {
                    implClose();
                }
            }
        }
    }
}
