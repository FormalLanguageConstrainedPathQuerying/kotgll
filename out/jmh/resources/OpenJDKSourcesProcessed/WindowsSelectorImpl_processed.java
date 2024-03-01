/*
 * Copyright (c) 2002, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import jdk.internal.misc.Unsafe;

/**
 * A multi-threaded implementation of Selector for Windows.
 *
 * @author Konstantin Kladko
 * @author Mark Reinhold
 */

class WindowsSelectorImpl extends SelectorImpl {
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    private static int dependsArch(int value32, int value64) {
        return (unsafe.addressSize() == 4) ? value32 : value64;
    }

    private static final int INIT_CAP = 8;
    private static final int MAX_SELECTABLE_FDS = 1024;

    private static final long SIZEOF_FD_SET = dependsArch(
            4 + MAX_SELECTABLE_FDS * 4,      
            4 + MAX_SELECTABLE_FDS * 8 + 4); 

    private SelectionKeyImpl[] channelArray = new SelectionKeyImpl[INIT_CAP];

    private final PollArrayWrapper pollWrapper;

    private int totalChannels = 1;

    private int threadsCount = 0;

    private final List<SelectThread> threads = new ArrayList<SelectThread>();

    private final Pipe wakeupPipe;

    private final int wakeupSourceFd, wakeupSinkFd;

    private static final class FdMap extends HashMap<Integer, MapEntry> {
        static final long serialVersionUID = 0L;
        private MapEntry get(int desc) {
            return get(Integer.valueOf(desc));
        }
        private MapEntry put(SelectionKeyImpl ski) {
            return put(Integer.valueOf(ski.getFDVal()), new MapEntry(ski));
        }
        private MapEntry remove(SelectionKeyImpl ski) {
            Integer fd = Integer.valueOf(ski.getFDVal());
            MapEntry x = get(fd);
            if ((x != null) && (x.ski.channel() == ski.channel()))
                return remove(fd);
            return null;
        }
    }

    private static final class MapEntry {
        final SelectionKeyImpl ski;
        long updateCount = 0;
        MapEntry(SelectionKeyImpl ski) {
            this.ski = ski;
        }
    }
    private final FdMap fdMap = new FdMap();

    private final SubSelector subSelector = new SubSelector();

    private long timeout; 

    private final Object interruptLock = new Object();
    private volatile boolean interruptTriggered;

    private final Object updateLock = new Object();
    private final Deque<SelectionKeyImpl> newKeys = new ArrayDeque<>();
    private final Deque<SelectionKeyImpl> updateKeys = new ArrayDeque<>();


    WindowsSelectorImpl(SelectorProvider sp) throws IOException {
        super(sp);
        pollWrapper = new PollArrayWrapper(INIT_CAP);
        wakeupPipe = new PipeImpl(sp, /* AF_UNIX */ true, /*buffering*/ false);
        wakeupSourceFd = ((SelChImpl)wakeupPipe.source()).getFDVal();
        wakeupSinkFd = ((SelChImpl)wakeupPipe.sink()).getFDVal();
        pollWrapper.addWakeupSocket(wakeupSourceFd, 0);
    }

    private void ensureOpen() {
        if (!isOpen())
            throw new ClosedSelectorException();
    }

    @Override
    protected int doSelect(Consumer<SelectionKey> action, long timeout)
        throws IOException
    {
        assert Thread.holdsLock(this);
        this.timeout = timeout; 
        processUpdateQueue();
        processDeregisterQueue();
        if (interruptTriggered) {
            resetWakeupSocket();
            return 0;
        }
        adjustThreadsCount();
        finishLock.reset(); 
        startLock.startThreads();
        try {
            begin();
            try {
                subSelector.poll();
            } catch (IOException e) {
                finishLock.setException(e); 
            }
            if (threads.size() > 0)
                finishLock.waitForHelperThreads();
          } finally {
              end();
          }
        finishLock.checkForException();
        processDeregisterQueue();
        int updated = updateSelectedKeys(action);
        resetWakeupSocket();
        return updated;
    }

    /**
     * Process new registrations and changes to the interest ops.
     */
    private void processUpdateQueue() {
        assert Thread.holdsLock(this);

        synchronized (updateLock) {
            SelectionKeyImpl ski;

            while ((ski = newKeys.pollFirst()) != null) {
                if (ski.isValid()) {
                    growIfNeeded();
                    channelArray[totalChannels] = ski;
                    ski.setIndex(totalChannels);
                    pollWrapper.putEntry(totalChannels, ski);
                    totalChannels++;
                    MapEntry previous = fdMap.put(ski);
                    assert previous == null;
                }
            }

            while ((ski = updateKeys.pollFirst()) != null) {
                int events = ski.translateInterestOps();
                int fd = ski.getFDVal();
                if (ski.isValid() && fdMap.containsKey(fd)) {
                    int index = ski.getIndex();
                    assert index >= 0 && index < totalChannels;
                    pollWrapper.putEventOps(index, events);
                }
            }
        }
    }

    private final StartLock startLock = new StartLock();

    private final class StartLock {
        private long runsCounter;
        private synchronized void startThreads() {
            runsCounter++; 
            notifyAll(); 
        }
        private synchronized boolean waitForStart(SelectThread thread) {
            while (true) {
                while (runsCounter == thread.lastRun) {
                    try {
                        startLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (thread.isZombie()) { 
                    return true; 
                } else {
                    thread.lastRun = runsCounter; 
                    return false; 
                }
            }
        }
    }

    private final FinishLock finishLock = new FinishLock();

    private final class FinishLock  {
        private int threadsToFinish;

        IOException exception = null;

        private void reset() {
            threadsToFinish = threads.size(); 
        }

        private synchronized void threadFinished() {
            if (threadsToFinish == threads.size()) { 
                wakeup();
            }
            threadsToFinish--;
            if (threadsToFinish == 0) 
                notify();             
        }

        private synchronized void waitForHelperThreads() {
            if (threadsToFinish == threads.size()) {
                wakeup();
            }
            while (threadsToFinish != 0) {
                try {
                    finishLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private synchronized void setException(IOException e) {
            exception = e;
        }

        private void checkForException() throws IOException {
            if (exception == null)
                return;
            String message = "An exception occurred" +
                    " during the execution of select(): \n" +
                    exception + '\n';
            exception = null;
            throw new IOException(message);
        }
    }

    private final class SubSelector {
        private final int pollArrayIndex; 
        private final int[] readFds = new int [MAX_SELECTABLE_FDS + 1];
        private final int[] writeFds = new int [MAX_SELECTABLE_FDS + 1];
        private final int[] exceptFds = new int [MAX_SELECTABLE_FDS + 1];
        private final long fdsBuffer = unsafe.allocateMemory(SIZEOF_FD_SET * 3);

        private SubSelector() {
            this.pollArrayIndex = 0; 
        }

        private SubSelector(int threadIndex) { 
            this.pollArrayIndex = (threadIndex + 1) * MAX_SELECTABLE_FDS;
        }

        private int poll() throws IOException{ 
            return poll0(pollWrapper.pollArrayAddress,
                         Math.min(totalChannels, MAX_SELECTABLE_FDS),
                         readFds, writeFds, exceptFds, timeout, fdsBuffer);
        }

        private int poll(int index) throws IOException {
            return  poll0(pollWrapper.pollArrayAddress +
                     (pollArrayIndex * PollArrayWrapper.SIZE_POLLFD),
                     Math.min(MAX_SELECTABLE_FDS,
                             totalChannels - (index + 1) * MAX_SELECTABLE_FDS),
                     readFds, writeFds, exceptFds, timeout, fdsBuffer);
        }

        private native int poll0(long pollAddress, int numfds,
             int[] readFds, int[] writeFds, int[] exceptFds, long timeout, long fdsBuffer);

        private int processSelectedKeys(long updateCount, Consumer<SelectionKey> action)
            throws IOException
        {
            int numKeysUpdated = 0;
            numKeysUpdated += processFDSet(updateCount, action, readFds,
                                           Net.POLLIN,
                                           false);
            numKeysUpdated += processFDSet(updateCount, action, writeFds,
                                           Net.POLLCONN |
                                           Net.POLLOUT,
                                           false);
            numKeysUpdated += processFDSet(updateCount, action, exceptFds,
                                           Net.POLLIN |
                                           Net.POLLCONN |
                                           Net.POLLOUT,
                                           true);
            return numKeysUpdated;
        }

        /**
         * updateCount is used to tell if a key has been counted as updated
         * in this select operation.
         *
         * me.updateCount <= updateCount
         */
        private int processFDSet(long updateCount,
                                 Consumer<SelectionKey> action,
                                 int[] fds, int rOps,
                                 boolean isExceptFds)
            throws IOException
        {
            int numKeysUpdated = 0;
            for (int i = 1; i <= fds[0]; i++) {
                int desc = fds[i];
                if (desc == wakeupSourceFd) {
                    synchronized (interruptLock) {
                        interruptTriggered = true;
                    }
                    continue;
                }
                MapEntry me = fdMap.get(desc);
                if (me == null)
                    continue;
                SelectionKeyImpl ski = me.ski;

                SelectableChannel sc = ski.channel();
                if (isExceptFds && (sc instanceof SocketChannelImpl)
                        && ((SocketChannelImpl) sc).isNetSocket()
                        && Net.discardOOB(ski.getFD())) {
                    continue;
                }

                int updated = processReadyEvents(rOps, ski, action);
                if (updated > 0 && me.updateCount != updateCount) {
                    me.updateCount = updateCount;
                    numKeysUpdated++;
                }
            }
            return numKeysUpdated;
        }

        private void freeFDSetBuffer() {
            unsafe.freeMemory(fdsBuffer);
        }
    }

    private final class SelectThread extends Thread {
        private final int index; 
        final SubSelector subSelector;
        private long lastRun = 0; 
        private volatile boolean zombie;
        private SelectThread(int i) {
            super(null, null, "SelectorHelper", 0, false);
            this.index = i;
            this.subSelector = new SubSelector(i);
            this.lastRun = startLock.runsCounter;
        }
        void makeZombie() {
            zombie = true;
        }
        boolean isZombie() {
            return zombie;
        }
        public void run() {
            while (true) { 
                if (startLock.waitForStart(this)) {
                    subSelector.freeFDSetBuffer();
                    return;
                }
                try {
                    subSelector.poll(index);
                } catch (IOException e) {
                    finishLock.setException(e);
                }
                finishLock.threadFinished();
            }
        }
    }

    private void adjustThreadsCount() {
        if (threadsCount > threads.size()) {
            for (int i = threads.size(); i < threadsCount; i++) {
                SelectThread newThread = new SelectThread(i);
                threads.add(newThread);
                newThread.setDaemon(true);
                newThread.start();
            }
        } else if (threadsCount < threads.size()) {
            for (int i = threads.size() - 1 ; i >= threadsCount; i--)
                threads.remove(i).makeZombie();
        }
    }

    private void setWakeupSocket() {
        setWakeupSocket0(wakeupSinkFd);
    }
    private native void setWakeupSocket0(int wakeupSinkFd);

    private void resetWakeupSocket() {
        synchronized (interruptLock) {
            if (interruptTriggered == false)
                return;
            resetWakeupSocket0(wakeupSourceFd);
            interruptTriggered = false;
        }
    }

    private native void resetWakeupSocket0(int wakeupSourceFd);

    private long updateCount = 0;

    private int updateSelectedKeys(Consumer<SelectionKey> action) throws IOException {
        updateCount++;
        int numKeysUpdated = 0;
        numKeysUpdated += subSelector.processSelectedKeys(updateCount, action);
        for (SelectThread t: threads) {
            numKeysUpdated += t.subSelector.processSelectedKeys(updateCount, action);
        }
        return numKeysUpdated;
    }

    @Override
    protected void implClose() throws IOException {
        assert !isOpen();
        assert Thread.holdsLock(this);

        synchronized (interruptLock) {
            interruptTriggered = true;
        }

        wakeupPipe.sink().close();
        wakeupPipe.source().close();
        pollWrapper.free();

        for (SelectThread t: threads)
             t.makeZombie();
        startLock.startThreads();
        subSelector.freeFDSetBuffer();
    }

    @Override
    protected void implRegister(SelectionKeyImpl ski) {
        ensureOpen();
        synchronized (updateLock) {
            newKeys.addLast(ski);
        }
    }

    private void growIfNeeded() {
        if (channelArray.length == totalChannels) {
            int newSize = totalChannels * 2; 
            SelectionKeyImpl temp[] = new SelectionKeyImpl[newSize];
            System.arraycopy(channelArray, 1, temp, 1, totalChannels - 1);
            channelArray = temp;
            pollWrapper.grow(newSize);
        }
        if (totalChannels % MAX_SELECTABLE_FDS == 0) { 
            pollWrapper.addWakeupSocket(wakeupSourceFd, totalChannels);
            totalChannels++;
            threadsCount++;
        }
    }

    @Override
    protected void implDereg(SelectionKeyImpl ski) {
        assert !ski.isValid();
        assert Thread.holdsLock(this);

        if (fdMap.remove(ski) != null) {
            int i = ski.getIndex();
            assert (i >= 0);

            if (i != totalChannels - 1) {
                SelectionKeyImpl endChannel = channelArray[totalChannels-1];
                channelArray[i] = endChannel;
                endChannel.setIndex(i);
                pollWrapper.replaceEntry(pollWrapper, totalChannels-1, pollWrapper, i);
            }
            ski.setIndex(-1);

            channelArray[totalChannels - 1] = null;
            totalChannels--;
            if (totalChannels != 1 && totalChannels % MAX_SELECTABLE_FDS == 1) {
                totalChannels--;
                threadsCount--; 
            }
        }
    }

    @Override
    public void setEventOps(SelectionKeyImpl ski) {
        ensureOpen();
        synchronized (updateLock) {
            updateKeys.addLast(ski);
        }
    }

    @Override
    public Selector wakeup() {
        synchronized (interruptLock) {
            if (!interruptTriggered) {
                setWakeupSocket();
                interruptTriggered = true;
            }
        }
        return this;
    }

    static {
        IOUtil.load();
    }
}
