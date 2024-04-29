/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.IOException;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/**
 * Windows implementation of DirectoryStream
 */

class WindowsDirectoryStream
    implements DirectoryStream<Path>
{
    private final WindowsPath dir;
    private final DirectoryStream.Filter<? super Path> filter;

    private final long handle;
    private final String firstName;

    private final NativeBuffer findDataBuffer;

    private final Object closeLock = new Object();

    private boolean isOpen = true;
    private Iterator<Path> iterator;


    WindowsDirectoryStream(WindowsPath dir, DirectoryStream.Filter<? super Path> filter)
        throws IOException
    {
        this.dir = dir;
        this.filter = filter;

        try {
            String search = dir.getPathForWin32Calls();
            char last = search.charAt(search.length() -1);
            if (last == ':' || last == '\\') {
                search += "*";
            } else {
                search += "\\*";
            }

            FirstFile first = FindFirstFile(search);
            this.handle = first.handle();
            this.firstName = first.name();
            this.findDataBuffer = WindowsFileAttributes.getBufferForFindData();
        } catch (WindowsException x) {
            if (x.lastError() == ERROR_DIRECTORY) {
                throw new NotDirectoryException(dir.getPathForExceptionMessage());
            }
            x.rethrowAsIOException(dir);

            throw new AssertionError();
        }
    }

    @Override
    public void close()
        throws IOException
    {
        synchronized (closeLock) {
            if (!isOpen)
                return;
            isOpen = false;
        }
        findDataBuffer.release();
        try {
            FindClose(handle);
        } catch (WindowsException x) {
            x.rethrowAsIOException(dir);
        }
    }

    @Override
    public Iterator<Path> iterator() {
        if (!isOpen) {
            throw new IllegalStateException("Directory stream is closed");
        }
        synchronized (this) {
            if (iterator != null)
                throw new IllegalStateException("Iterator already obtained");
            iterator = new WindowsDirectoryIterator(firstName);
            return iterator;
        }
    }

    private class WindowsDirectoryIterator implements Iterator<Path> {
        private boolean atEof;
        private String first;
        private Path nextEntry;
        private String prefix;

        WindowsDirectoryIterator(String first) {
            atEof = false;
            this.first = first;
            if (dir.needsSlashWhenResolving()) {
                prefix = dir.toString() + "\\";
            } else {
                prefix = dir.toString();
            }
        }

        private boolean isSelfOrParent(String name) {
            return name.equals(".") || name.equals("..");
        }

        private Path acceptEntry(String s, BasicFileAttributes attrs) {
            Path entry = WindowsPath
                .createFromNormalizedPath(dir.getFileSystem(), prefix + s, attrs);
            try {
                if (filter.accept(entry))
                    return entry;
            } catch (IOException ioe) {
                throw new DirectoryIteratorException(ioe);
            }
            return null;
        }

        private Path readNextEntry() {
            if (first != null) {
                nextEntry = isSelfOrParent(first) ? null : acceptEntry(first, null);
                first = null;
                if (nextEntry != null)
                    return nextEntry;
            }

            for (;;) {
                String name = null;
                WindowsFileAttributes attrs;

                synchronized (closeLock) {
                    try {
                        if (isOpen) {
                            name = FindNextFile(handle, findDataBuffer.address());
                        }
                    } catch (WindowsException x) {
                        IOException ioe = x.asIOException(dir);
                        throw new DirectoryIteratorException(ioe);
                    }

                    if (name == null) {
                        atEof = true;
                        return null;
                    }

                    if (isSelfOrParent(name))
                        continue;

                    attrs = WindowsFileAttributes
                        .fromFindData(findDataBuffer.address());
                }

                Path entry = acceptEntry(name, attrs);
                if (entry != null)
                    return entry;
            }
        }

        @Override
        public synchronized boolean hasNext() {
            if (nextEntry == null && !atEof)
                nextEntry = readNextEntry();
            return nextEntry != null;
        }

        @Override
        public synchronized Path next() {
            Path result = null;
            if (nextEntry == null && !atEof) {
                result = readNextEntry();
            } else {
                result = nextEntry;
                nextEntry = null;
            }
            if (result == null)
                throw new NoSuchElementException();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
