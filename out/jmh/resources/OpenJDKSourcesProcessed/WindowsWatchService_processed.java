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

import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jdk.internal.misc.Unsafe;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/*
 * Win32 implementation of WatchService based on ReadDirectoryChangesW.
 */

class WindowsWatchService
    extends AbstractWatchService
{
    private static final int WAKEUP_COMPLETION_KEY = 0;

    private final Poller poller;

    /**
     * Creates an I/O completion port and a daemon thread to service it
     */
    WindowsWatchService(WindowsFileSystem fs) throws IOException {
        long port = 0L;
        try {
            port = CreateIoCompletionPort(INVALID_HANDLE_VALUE, 0, 0);
        } catch (WindowsException x) {
            throw new IOException(x.getMessage());
        }

        this.poller = new Poller(fs, this, port);
        this.poller.start();
    }

    @Override
    WatchKey register(Path path,
                      WatchEvent.Kind<?>[] events,
                      WatchEvent.Modifier... modifiers)
         throws IOException
    {
        return poller.register(path, events, modifiers);
    }

    @Override
    void implClose() throws IOException {
        poller.close();
    }

    /**
     * Windows implementation of WatchKey.
     */
    private static class WindowsWatchKey extends AbstractWatchKey {
        private final FileKey fileKey;

        private volatile long handle = INVALID_HANDLE_VALUE;

        private Set<? extends WatchEvent.Kind<?>> events;

        private boolean watchSubtree;

        private NativeBuffer buffer;

        private long countAddress;

        private long overlappedAddress;

        private int completionKey;

        private boolean errorStartingOverlapped;

        WindowsWatchKey(Path dir,
                        AbstractWatchService watcher,
                        FileKey fileKey)
        {
            super(dir, watcher);
            this.fileKey = fileKey;
        }

        WindowsWatchKey init(long handle,
                             Set<? extends WatchEvent.Kind<?>> events,
                             boolean watchSubtree,
                             NativeBuffer buffer,
                             long countAddress,
                             long overlappedAddress,
                             int completionKey)
        {
            this.handle = handle;
            this.events = events;
            this.watchSubtree = watchSubtree;
            this.buffer = buffer;
            this.countAddress = countAddress;
            this.overlappedAddress = overlappedAddress;
            this.completionKey = completionKey;
            return this;
        }

        long handle() {
            return handle;
        }

        Set<? extends WatchEvent.Kind<?>> events() {
            return events;
        }

        void setEvents(Set<? extends WatchEvent.Kind<?>> events) {
            this.events = events;
        }

        boolean watchSubtree() {
            return watchSubtree;
        }

        NativeBuffer buffer() {
            return buffer;
        }

        long countAddress() {
            return countAddress;
        }

        long overlappedAddress() {
            return overlappedAddress;
        }

        FileKey fileKey() {
            return fileKey;
        }

        int completionKey() {
            return completionKey;
        }

        void setErrorStartingOverlapped(boolean value) {
            errorStartingOverlapped = value;
        }

        boolean isErrorStartingOverlapped() {
            return errorStartingOverlapped;
        }

        void invalidate() {
            ((WindowsWatchService)watcher()).poller.releaseResources(this);
            handle = INVALID_HANDLE_VALUE;
            buffer = null;
            countAddress = 0;
            overlappedAddress = 0;
            errorStartingOverlapped = false;
        }

        @Override
        public boolean isValid() {
            return handle != INVALID_HANDLE_VALUE;
        }

        @Override
        public void cancel() {
            if (isValid()) {
                ((WindowsWatchService)watcher()).poller.cancel(this);
            }
        }
    }

    private static class FileKey {
        private final int volSerialNumber;
        private final int fileIndexHigh;
        private final int fileIndexLow;

        FileKey(int volSerialNumber, int fileIndexHigh, int fileIndexLow) {
            this.volSerialNumber = volSerialNumber;
            this.fileIndexHigh = fileIndexHigh;
            this.fileIndexLow = fileIndexLow;
        }

        @Override
        public int hashCode() {
            return volSerialNumber ^ fileIndexHigh ^ fileIndexLow;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            return obj instanceof FileKey other
                    && this.volSerialNumber == other.volSerialNumber
                    && this.fileIndexHigh == other.fileIndexHigh
                    && this.fileIndexLow == other.fileIndexLow;
        }
    }

    private static final int ALL_FILE_NOTIFY_EVENTS =
        FILE_NOTIFY_CHANGE_FILE_NAME |
        FILE_NOTIFY_CHANGE_DIR_NAME |
        FILE_NOTIFY_CHANGE_ATTRIBUTES  |
        FILE_NOTIFY_CHANGE_SIZE |
        FILE_NOTIFY_CHANGE_LAST_WRITE |
        FILE_NOTIFY_CHANGE_CREATION |
        FILE_NOTIFY_CHANGE_SECURITY;

    /**
     * Background thread to service I/O completion port.
     */
    private static class Poller extends AbstractPoller {
        private static final Unsafe UNSAFE = Unsafe.getUnsafe();

        /*
         * typedef struct _OVERLAPPED {
         *     ULONG_PTR  Internal;
         *     ULONG_PTR  InternalHigh;
         *     union {
         *         struct { DWORD Offset; DWORD OffsetHigh; };
         *         PVOID  Pointer;
         *     };
         *     HANDLE    hEvent;
         * } OVERLAPPED;
         */
        private static final short SIZEOF_DWORD         = 4;
        private static final short SIZEOF_OVERLAPPED    = 32; 
        private static final short OFFSETOF_HEVENT      =
            (UNSAFE.addressSize() == 4) ? (short) 16 : 24;


        /*
         * typedef struct _FILE_NOTIFY_INFORMATION {
         *     DWORD NextEntryOffset;
         *     DWORD Action;
         *     DWORD FileNameLength;
         *     WCHAR FileName[1];
         * } FileNameLength;
         */
        private static final short OFFSETOF_NEXTENTRYOFFSET = 0;
        private static final short OFFSETOF_ACTION          = 4;
        private static final short OFFSETOF_FILENAMELENGTH  = 8;
        private static final short OFFSETOF_FILENAME        = 12;

        private static final int CHANGES_BUFFER_SIZE    = 16 * 1024;

        private final WindowsFileSystem fs;
        private final WindowsWatchService watcher;
        private final long port;

        private final Map<Integer, WindowsWatchKey> ck2key;

        private final Map<FileKey, WindowsWatchKey> fk2key;

        private int lastCompletionKey;

        Poller(WindowsFileSystem fs, WindowsWatchService watcher, long port) {
            this.fs = fs;
            this.watcher = watcher;
            this.port = port;
            this.ck2key = new HashMap<>();
            this.fk2key = new HashMap<>();
            this.lastCompletionKey = 0;
        }

        @Override
        void wakeup() throws IOException {
            try {
                PostQueuedCompletionStatus(port, WAKEUP_COMPLETION_KEY);
            } catch (WindowsException x) {
                throw new IOException(x.getMessage());
            }
        }

        /**
         * Register a directory for changes as follows:
         *
         * 1. Open directory
         * 2. Read its attributes (and check it really is a directory)
         * 3. Assign completion key and associated handle with completion port
         * 4. Call ReadDirectoryChangesW to start (async) read of changes
         * 5. Create or return existing key representing registration
         */
        @Override
        Object implRegister(Path obj,
                            Set<? extends WatchEvent.Kind<?>> events,
                            WatchEvent.Modifier... modifiers)
        {
            WindowsPath dir = (WindowsPath)obj;
            boolean watchSubtree = false;

            for (WatchEvent.Modifier modifier: modifiers) {
                if (ExtendedOptions.FILE_TREE.matches(modifier)) {
                    watchSubtree = true;
                } else {
                    if (modifier == null)
                        return new NullPointerException();
                    if (!ExtendedOptions.SENSITIVITY_HIGH.matches(modifier) &&
                            !ExtendedOptions.SENSITIVITY_MEDIUM.matches(modifier) &&
                            !ExtendedOptions.SENSITIVITY_LOW.matches(modifier)) {
                        return new UnsupportedOperationException("Modifier not supported");
                    }
                }
            }

            long handle;
            try {
                handle = CreateFile(dir.getPathForWin32Calls(),
                                    FILE_LIST_DIRECTORY,
                                    (FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE),
                                    OPEN_EXISTING,
                                    FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OVERLAPPED);
            } catch (WindowsException x) {
                return x.asIOException(dir);
            }

            boolean registered = false;
            try {
                WindowsFileAttributes attrs;
                try {
                    attrs = WindowsFileAttributes.readAttributes(handle);
                } catch (WindowsException x) {
                    return x.asIOException(dir);
                }
                if (!attrs.isDirectory()) {
                    return new NotDirectoryException(dir.getPathForExceptionMessage());
                }

                FileKey fk = new FileKey(attrs.volSerialNumber(),
                                         attrs.fileIndexHigh(),
                                         attrs.fileIndexLow());
                WindowsWatchKey existing = fk2key.get(fk);

                if (existing != null && watchSubtree == existing.watchSubtree()) {
                    existing.setEvents(events);
                    return existing;
                }

                int completionKey = ++lastCompletionKey;
                if (completionKey == WAKEUP_COMPLETION_KEY)
                    completionKey = ++lastCompletionKey;

                try {
                    CreateIoCompletionPort(handle, port, completionKey);
                } catch (WindowsException x) {
                    return new IOException(x.getMessage());
                }

                int size = CHANGES_BUFFER_SIZE + SIZEOF_DWORD + SIZEOF_OVERLAPPED;
                NativeBuffer buffer = NativeBuffers.getNativeBuffer(size);

                long bufferAddress = buffer.address();
                long overlappedAddress = bufferAddress + size - SIZEOF_OVERLAPPED;
                long countAddress = overlappedAddress - SIZEOF_DWORD;

                UNSAFE.setMemory(overlappedAddress, SIZEOF_OVERLAPPED, (byte)0);

                try {
                    createAndAttachEvent(overlappedAddress);

                    ReadDirectoryChangesW(handle,
                                          bufferAddress,
                                          CHANGES_BUFFER_SIZE,
                                          watchSubtree,
                                          ALL_FILE_NOTIFY_EVENTS,
                                          countAddress,
                                          overlappedAddress);
                } catch (WindowsException x) {
                    closeAttachedEvent(overlappedAddress);
                    buffer.release();
                    return new IOException(x.getMessage());
                }

                WindowsWatchKey watchKey;
                if (existing == null) {
                    watchKey = new WindowsWatchKey(dir, watcher, fk)
                        .init(handle, events, watchSubtree, buffer, countAddress,
                              overlappedAddress, completionKey);
                    fk2key.put(fk, watchKey);
                } else {
                    ck2key.remove(existing.completionKey());
                    releaseResources(existing);
                    watchKey = existing.init(handle, events, watchSubtree, buffer,
                        countAddress, overlappedAddress, completionKey);
                }
                ck2key.put(completionKey, watchKey);

                registered = true;
                return watchKey;

            } finally {
                if (!registered) CloseHandle(handle);
            }
        }

        /**
         * Cancels the outstanding I/O operation on the directory
         * associated with the given key and releases the associated
         * resources.
         */
        private void releaseResources(WindowsWatchKey key) {
            if (!key.isErrorStartingOverlapped()) {
                try {
                    CancelIo(key.handle());
                    GetOverlappedResult(key.handle(), key.overlappedAddress());
                } catch (WindowsException expected) {
                }
            }
            CloseHandle(key.handle());
            closeAttachedEvent(key.overlappedAddress());
            key.buffer().free();
        }

        /**
         * Creates an unnamed event and set it as the hEvent field
         * in the given OVERLAPPED structure
         */
        private void createAndAttachEvent(long ov) throws WindowsException {
            long hEvent = CreateEvent(false, false);
            UNSAFE.putAddress(ov + OFFSETOF_HEVENT, hEvent);
        }

        /**
         * Closes the event attached to the given OVERLAPPED structure. A
         * no-op if there isn't an event attached.
         */
        private void closeAttachedEvent(long ov) {
            long hEvent = UNSAFE.getAddress(ov + OFFSETOF_HEVENT);
            if (hEvent != 0 && hEvent != INVALID_HANDLE_VALUE)
               CloseHandle(hEvent);
        }

        @Override
        void implCancelKey(WatchKey obj) {
            WindowsWatchKey key = (WindowsWatchKey)obj;
            if (key.isValid()) {
                fk2key.remove(key.fileKey());
                ck2key.remove(key.completionKey());
                key.invalidate();
            }
        }

        @Override
        void implCloseAll() {
            ck2key.values().forEach(WindowsWatchKey::invalidate);

            fk2key.clear();
            ck2key.clear();

            CloseHandle(port);
        }

        private WatchEvent.Kind<?> translateActionToEvent(int action) {
            switch (action) {
                case FILE_ACTION_MODIFIED :
                    return StandardWatchEventKinds.ENTRY_MODIFY;

                case FILE_ACTION_ADDED :
                case FILE_ACTION_RENAMED_NEW_NAME :
                    return StandardWatchEventKinds.ENTRY_CREATE;

                case FILE_ACTION_REMOVED :
                case FILE_ACTION_RENAMED_OLD_NAME :
                    return StandardWatchEventKinds.ENTRY_DELETE;

                default :
                    return null;  
            }
        }

        private void processEvents(WindowsWatchKey key, int size) {
            long address = key.buffer().address();

            int nextOffset;
            do {
                int action = UNSAFE.getInt(address + OFFSETOF_ACTION);

                WatchEvent.Kind<?> kind = translateActionToEvent(action);
                if (key.events().contains(kind)) {
                    int nameLengthInBytes = UNSAFE.getInt(address + OFFSETOF_FILENAMELENGTH);
                    if ((nameLengthInBytes % 2) != 0) {
                        throw new AssertionError("FileNameLength is not a multiple of 2");
                    }
                    char[] nameAsArray = new char[nameLengthInBytes/2];
                    UNSAFE.copyMemory(null, address + OFFSETOF_FILENAME, nameAsArray,
                        Unsafe.ARRAY_CHAR_BASE_OFFSET, nameLengthInBytes);

                    WindowsPath name = WindowsPath
                        .createFromNormalizedPath(fs, new String(nameAsArray));
                    key.signalEvent(kind, name);
                }

                nextOffset = UNSAFE.getInt(address + OFFSETOF_NEXTENTRYOFFSET);
                address += (long)nextOffset;
            } while (nextOffset != 0);
        }

        /**
         * Poller main loop
         */
        @Override
        public void run() {
            for (;;) {
                CompletionStatus info;
                try {
                    info = GetQueuedCompletionStatus(port);
                } catch (WindowsException x) {
                    x.printStackTrace();
                    return;
                }

                if (info.completionKey() == WAKEUP_COMPLETION_KEY) {
                    boolean shutdown = processRequests();
                    if (shutdown) {
                        return;
                    }
                    continue;
                }

                WindowsWatchKey key = ck2key.get((int)info.completionKey());
                if (key == null) {
                    continue;
                }

                boolean criticalError = false;
                int errorCode = info.error();
                int messageSize = info.bytesTransferred();
                if (errorCode == ERROR_NOTIFY_ENUM_DIR) {
                    key.signalEvent(StandardWatchEventKinds.OVERFLOW, null);
                } else if (errorCode != 0 && errorCode != ERROR_MORE_DATA) {
                    criticalError = true;
                } else {

                    if (messageSize > 0) {
                        processEvents(key, messageSize);
                    } else if (errorCode == 0) {
                        key.signalEvent(StandardWatchEventKinds.OVERFLOW, null);
                    }

                    try {
                        ReadDirectoryChangesW(key.handle(),
                                              key.buffer().address(),
                                              CHANGES_BUFFER_SIZE,
                                              key.watchSubtree(),
                                              ALL_FILE_NOTIFY_EVENTS,
                                              key.countAddress(),
                                              key.overlappedAddress());
                    } catch (WindowsException x) {
                        criticalError = true;
                        key.setErrorStartingOverlapped(true);
                    }
                }
                if (criticalError) {
                    implCancelKey(key);
                    key.signal();
                }
            }
        }
    }
}
