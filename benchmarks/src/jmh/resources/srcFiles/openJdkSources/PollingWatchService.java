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

package sun.nio.fs;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/**
 * Simple WatchService implementation that uses periodic tasks to poll
 * registered directories for changes.  This implementation is for use on
 * operating systems that do not have native file change notification support.
 */

class PollingWatchService
    extends AbstractWatchService
{
    private static final int POLLING_INTERVAL = 2;

    private final Map<Object, PollingWatchKey> map = new HashMap<>();

    private final ScheduledExecutorService scheduledExecutor;

    PollingWatchService() {
        scheduledExecutor = Executors
            .newSingleThreadScheduledExecutor(new ThreadFactory() {
                 @Override
                 public Thread newThread(Runnable r) {
                     Thread t = new Thread(null, r, "FileSystemWatcher", 0, false);
                     t.setDaemon(true);
                     return t;
                 }});
    }

    /**
     * Register the given file with this watch service
     */
    @SuppressWarnings("removal")
    @Override
    WatchKey register(final Path path,
                      WatchEvent.Kind<?>[] events,
                      WatchEvent.Modifier... modifiers)
         throws IOException
    {
        final Set<WatchEvent.Kind<?>> eventSet = HashSet.newHashSet(events.length);
        for (WatchEvent.Kind<?> event: events) {
            if (event == StandardWatchEventKinds.ENTRY_CREATE ||
                event == StandardWatchEventKinds.ENTRY_MODIFY ||
                event == StandardWatchEventKinds.ENTRY_DELETE)
            {
                eventSet.add(event);
                continue;
            }

            if (event == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }

            if (event == null)
                throw new NullPointerException("An element in event set is 'null'");
            throw new UnsupportedOperationException(event.name());
        }
        if (eventSet.isEmpty())
            throw new IllegalArgumentException("No events to register");

        for (WatchEvent.Modifier modifier : modifiers) {
            if (modifier == null)
                throw new NullPointerException();
            if (!ExtendedOptions.SENSITIVITY_HIGH.matches(modifier) &&
                !ExtendedOptions.SENSITIVITY_MEDIUM.matches(modifier) &&
                !ExtendedOptions.SENSITIVITY_LOW.matches(modifier)) {
                throw new UnsupportedOperationException("Modifier not supported");
            }
        }

        if (!isOpen())
            throw new ClosedWatchServiceException();

        try {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<PollingWatchKey>() {
                    @Override
                    public PollingWatchKey run() throws IOException {
                        return doPrivilegedRegister(path, eventSet);
                    }
                });
        } catch (PrivilegedActionException pae) {
            Throwable cause = pae.getCause();
            if (cause instanceof IOException ioe)
                throw ioe;
            throw new AssertionError(pae);
        }
    }

    private PollingWatchKey doPrivilegedRegister(Path path,
                                                 Set<? extends WatchEvent.Kind<?>> events)
        throws IOException
    {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        if (!attrs.isDirectory()) {
            throw new NotDirectoryException(path.toString());
        }
        Object fileKey = attrs.fileKey();
        if (fileKey == null)
            throw new AssertionError("File keys must be supported");

        synchronized (closeLock()) {
            if (!isOpen())
                throw new ClosedWatchServiceException();

            PollingWatchKey watchKey;
            synchronized (map) {
                watchKey = map.get(fileKey);
                if (watchKey == null) {
                    watchKey = new PollingWatchKey(path, this, fileKey);
                    map.put(fileKey, watchKey);
                } else {
                    watchKey.disable();
                }
            }
            watchKey.enable(events);
            return watchKey;
        }

    }

    @SuppressWarnings("removal")
    @Override
    void implClose() throws IOException {
        synchronized (map) {
            for (Map.Entry<Object, PollingWatchKey> entry: map.entrySet()) {
                PollingWatchKey watchKey = entry.getValue();
                watchKey.disable();
                watchKey.invalidate();
            }
            map.clear();
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                scheduledExecutor.shutdown();
                return null;
            }
         });
    }

    /**
     * Entry in directory cache to record file last-modified-time and tick-count
     */
    private static class CacheEntry {
        private FileTime lastModified;
        private int lastTickCount;

        CacheEntry(FileTime lastModified, int lastTickCount) {
            this.lastModified = lastModified;
            this.lastTickCount = lastTickCount;
        }

        int lastTickCount() {
            return lastTickCount;
        }

        FileTime lastModified() {
            return lastModified;
        }

        void update(FileTime lastModified, int tickCount) {
            this.lastModified = lastModified;
            this.lastTickCount = tickCount;
        }
    }

    /**
     * WatchKey implementation that encapsulates a map of the entries of the
     * entries in the directory. Polling the key causes it to re-scan the
     * directory and queue keys when entries are added, modified, or deleted.
     */
    private class PollingWatchKey extends AbstractWatchKey {
        private final Object fileKey;

        private Set<? extends WatchEvent.Kind<?>> events;

        private ScheduledFuture<?> poller;

        private volatile boolean valid;

        private int tickCount;

        private Map<Path,CacheEntry> entries;

        PollingWatchKey(Path dir, PollingWatchService watcher, Object fileKey)
            throws IOException
        {
            super(dir, watcher);
            this.fileKey = fileKey;
            this.valid = true;
            this.tickCount = 0;
            this.entries = new HashMap<Path,CacheEntry>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry: stream) {
                    FileTime lastModified = Files.getLastModifiedTime(entry, NOFOLLOW_LINKS);
                    entries.put(entry.getFileName(), new CacheEntry(lastModified, tickCount));
                }
            } catch (DirectoryIteratorException e) {
                throw e.getCause();
            }
        }

        Object fileKey() {
            return fileKey;
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        void invalidate() {
            valid = false;
        }

        void enable(Set<? extends WatchEvent.Kind<?>> events) {
            synchronized (this) {
                this.events = events;

                Runnable thunk = new Runnable() { public void run() { poll(); }};
                this.poller = scheduledExecutor
                    .scheduleAtFixedRate(thunk, POLLING_INTERVAL,
                                         POLLING_INTERVAL, TimeUnit.SECONDS);
            }
        }

        void disable() {
            synchronized (this) {
                if (poller != null)
                    poller.cancel(false);
            }
        }

        @Override
        public void cancel() {
            valid = false;
            synchronized (map) {
                map.remove(fileKey());
            }
            disable();
        }

        /**
         * Polls the directory to detect for new files, modified files, or
         * deleted files.
         */
        synchronized void poll() {
            if (!valid) {
                return;
            }

            tickCount++;

            DirectoryStream<Path> stream = null;
            try {
                stream = Files.newDirectoryStream(watchable());
            } catch (IOException x) {
                cancel();
                signal();
                return;
            }

            try {
                for (Path entry: stream) {
                    FileTime lastModified;
                    try {
                        lastModified = Files.getLastModifiedTime(entry, NOFOLLOW_LINKS);
                    } catch (IOException x) {
                        continue;
                    }

                    CacheEntry e = entries.get(entry.getFileName());
                    if (e == null) {
                        entries.put(entry.getFileName(), new CacheEntry(lastModified, tickCount));

                        if (events.contains(StandardWatchEventKinds.ENTRY_CREATE)) {
                            signalEvent(StandardWatchEventKinds.ENTRY_CREATE, entry.getFileName());
                            continue;
                        } else {
                            if (events.contains(StandardWatchEventKinds.ENTRY_MODIFY)) {
                                signalEvent(StandardWatchEventKinds.ENTRY_MODIFY, entry.getFileName());
                            }
                        }
                        continue;
                    }

                    if (!e.lastModified().equals(lastModified)) {
                        if (events.contains(StandardWatchEventKinds.ENTRY_MODIFY)) {
                            signalEvent(StandardWatchEventKinds.ENTRY_MODIFY,
                                        entry.getFileName());
                        }
                    }
                    e.update(lastModified, tickCount);

                }
            } catch (DirectoryIteratorException e) {
            } finally {

                try {
                    stream.close();
                } catch (IOException x) {
                }
            }

            Iterator<Map.Entry<Path,CacheEntry>> i = entries.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Path,CacheEntry> mapEntry = i.next();
                CacheEntry entry = mapEntry.getValue();
                if (entry.lastTickCount() != tickCount) {
                    Path name = mapEntry.getKey();
                    i.remove();
                    if (events.contains(StandardWatchEventKinds.ENTRY_DELETE)) {
                        signalEvent(StandardWatchEventKinds.ENTRY_DELETE, name);
                    }
                }
            }
        }
    }
}
