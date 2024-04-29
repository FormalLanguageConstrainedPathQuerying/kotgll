/*
 * Copyright (c) 2008, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.LinkPermission;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/**
 * Utility methods for copying and moving files.
 */

class WindowsFileCopy {
    private static final long UNBUFFERED_IO_THRESHOLD = 314572800; 

    private WindowsFileCopy() {
    }

    /**
     * Copy file from source to target
     */
    static void copy(final WindowsPath source,
                     final WindowsPath target,
                     CopyOption... options)
        throws IOException
    {
        boolean replaceExisting = false;
        boolean copyAttributes = false;
        boolean followLinks = true;
        boolean interruptible = false;
        for (CopyOption option: options) {
            if (option == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
                continue;
            }
            if (option == LinkOption.NOFOLLOW_LINKS) {
                followLinks = false;
                continue;
            }
            if (option == StandardCopyOption.COPY_ATTRIBUTES) {
                copyAttributes = true;
                continue;
            }
            if (ExtendedOptions.INTERRUPTIBLE.matches(option)) {
                interruptible = true;
                continue;
            }
            if (option == null)
                throw new NullPointerException();
            throw new UnsupportedOperationException("Unsupported copy option: " + option);
        }

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            source.checkRead();
            target.checkWrite();
        }


        WindowsFileAttributes sourceAttrs = null;
        WindowsFileAttributes targetAttrs = null;

        long sourceHandle = 0L;
        try {
            sourceHandle = source.openForReadAttributeAccess(followLinks);
        } catch (WindowsException x) {
            x.rethrowAsIOException(source);
        }
        try {
            try {
                sourceAttrs = WindowsFileAttributes.readAttributes(sourceHandle);
            } catch (WindowsException x) {
                x.rethrowAsIOException(source);
            }

            long targetHandle = 0L;
            try {
                targetHandle = target.openForReadAttributeAccess(false);
                try {
                    targetAttrs = WindowsFileAttributes.readAttributes(targetHandle);

                    if (WindowsFileAttributes.isSameFile(sourceAttrs, targetAttrs)) {
                        return;
                    }

                    if (!replaceExisting) {
                        throw new FileAlreadyExistsException(
                            target.getPathForExceptionMessage());
                    }

                } finally {
                    CloseHandle(targetHandle);
                }
            } catch (WindowsException x) {
            }

        } finally {
            CloseHandle(sourceHandle);
        }

        if (sm != null && sourceAttrs.isSymbolicLink()) {
            sm.checkPermission(new LinkPermission("symbolic"));
        }

        if (sourceAttrs.isUnixDomainSocket()) {
            throw new IOException("Can not copy socket file");
        }

        final String sourcePath = asWin32Path(source);
        final String targetPath = asWin32Path(target);

        if (targetAttrs != null) {
            try {
                if (targetAttrs.isDirectory() || targetAttrs.isDirectoryLink()) {
                    RemoveDirectory(targetPath);
                } else {
                    DeleteFile(targetPath);
                }
            } catch (WindowsException x) {
                if (targetAttrs.isDirectory()) {
                    if (x.lastError() == ERROR_DIR_NOT_EMPTY ||
                        x.lastError() == ERROR_ALREADY_EXISTS)
                    {
                        throw new DirectoryNotEmptyException(
                            target.getPathForExceptionMessage());
                    }
                }
                if (x.lastError() != ERROR_FILE_NOT_FOUND &&
                    x.lastError() != ERROR_PATH_NOT_FOUND) {
                    x.rethrowAsIOException(target);
                }
            }
        }

        if (!sourceAttrs.isDirectory() && !sourceAttrs.isDirectoryLink()) {
            boolean isBuffering = sourceAttrs.size() <= UNBUFFERED_IO_THRESHOLD;
            final int flags = (followLinks ? 0 : COPY_FILE_COPY_SYMLINK) |
                              (isBuffering ? 0 : COPY_FILE_NO_BUFFERING);

            if (interruptible) {
                Cancellable copyTask = new Cancellable() {
                    @Override
                    public int cancelValue() {
                        return 1;  
                    }
                    @Override
                    public void implRun() throws IOException {
                        try {
                            CopyFileEx(sourcePath, targetPath, flags,
                                       addressToPollForCancel());
                        } catch (WindowsException x) {
                            x.rethrowAsIOException(source, target);
                        }
                    }
                };
                try {
                    Cancellable.runInterruptibly(copyTask);
                } catch (ExecutionException e) {
                    Throwable t = e.getCause();
                    if (t instanceof IOException)
                        throw (IOException)t;
                    throw new IOException(t);
                }
            } else {
                try {
                    CopyFileEx(sourcePath, targetPath, flags, 0L);
                } catch (WindowsException x) {
                    x.rethrowAsIOException(source, target);
                }
            }
            if (copyAttributes) {
                try {
                    copySecurityAttributes(source, target, followLinks);
                } catch (IOException x) {
                }
            }
            return;
        }

        try {
            if (sourceAttrs.isDirectory()) {
                CreateDirectory(targetPath, 0L);
            } else {
                String linkTarget = WindowsLinkSupport.readLink(source);
                int flags = SYMBOLIC_LINK_FLAG_DIRECTORY;
                WindowsLinkSupport.createSymbolicLink(targetPath,
                                                      WindowsPath.addPrefixIfNeeded(linkTarget),
                                                      flags);
            }
        } catch (WindowsException x) {
            x.rethrowAsIOException(target);
        }
        if (copyAttributes) {
            WindowsFileAttributeViews.Dos view =
                WindowsFileAttributeViews.createDosView(target, false);
            try {
                view.setAttributes(sourceAttrs);
            } catch (IOException x) {
                if (sourceAttrs.isDirectory()) {
                    try {
                        RemoveDirectory(targetPath);
                    } catch (WindowsException ignore) { }
                }
            }

            try {
                copySecurityAttributes(source, target, followLinks);
            } catch (IOException ignore) { }
        }
    }

    static void ensureEmptyDir(WindowsPath dir) throws IOException {
        try (WindowsDirectoryStream dirStream =
            new WindowsDirectoryStream(dir, (e) -> true)) {
            if (dirStream.iterator().hasNext()) {
                throw new DirectoryNotEmptyException(
                    dir.getPathForExceptionMessage());
            }
        }
    }

    /**
     * Move file from source to target
     */
    static void move(WindowsPath source, WindowsPath target, CopyOption... options)
        throws IOException
    {
        boolean atomicMove = false;
        boolean replaceExisting = false;
        for (CopyOption option: options) {
            if (option == StandardCopyOption.ATOMIC_MOVE) {
                atomicMove = true;
                continue;
            }
            if (option == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
                continue;
            }
            if (option == LinkOption.NOFOLLOW_LINKS) {
                continue;
            }
            if (option == null) throw new NullPointerException();
            throw new UnsupportedOperationException("Unsupported option: " + option);
        }

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            source.checkWrite();
            target.checkWrite();
        }

        final String sourcePath = asWin32Path(source);
        final String targetPath = asWin32Path(target);

        if (atomicMove) {
            try {
                MoveFileEx(sourcePath, targetPath, MOVEFILE_REPLACE_EXISTING);
            } catch (WindowsException x) {
                if (x.lastError() == ERROR_NOT_SAME_DEVICE) {
                    throw new AtomicMoveNotSupportedException(
                        source.getPathForExceptionMessage(),
                        target.getPathForExceptionMessage(),
                        x.errorString());
                }
                x.rethrowAsIOException(source, target);
            }
            return;
        }


        WindowsFileAttributes sourceAttrs = null;
        WindowsFileAttributes targetAttrs = null;

        long sourceHandle = 0L;
        try {
            sourceHandle = source.openForReadAttributeAccess(false);
        } catch (WindowsException x) {
            x.rethrowAsIOException(source);
        }
        try {
            try {
                sourceAttrs = WindowsFileAttributes.readAttributes(sourceHandle);
            } catch (WindowsException x) {
                x.rethrowAsIOException(source);
            }

            long targetHandle = 0L;
            try {
                targetHandle = target.openForReadAttributeAccess(false);
                try {
                    targetAttrs = WindowsFileAttributes.readAttributes(targetHandle);

                    if (WindowsFileAttributes.isSameFile(sourceAttrs, targetAttrs)) {
                        return;
                    }

                    if (!replaceExisting) {
                        throw new FileAlreadyExistsException(
                            target.getPathForExceptionMessage());
                    }

                } finally {
                    CloseHandle(targetHandle);
                }
            } catch (WindowsException x) {
            }

        } finally {
            CloseHandle(sourceHandle);
        }

        if (targetAttrs != null) {
            try {
                if (targetAttrs.isDirectory() || targetAttrs.isDirectoryLink()) {
                    RemoveDirectory(targetPath);
                } else {
                    DeleteFile(targetPath);
                }
            } catch (WindowsException x) {
                if (targetAttrs.isDirectory()) {
                    if (x.lastError() == ERROR_DIR_NOT_EMPTY ||
                        x.lastError() == ERROR_ALREADY_EXISTS)
                    {
                        throw new DirectoryNotEmptyException(
                            target.getPathForExceptionMessage());
                    }
                }
                if (x.lastError() != ERROR_FILE_NOT_FOUND &&
                    x.lastError() != ERROR_PATH_NOT_FOUND) {
                    x.rethrowAsIOException(target);
                }
            }
        }

        try {
            MoveFileEx(sourcePath, targetPath, 0);
            return;
        } catch (WindowsException x) {
            if (x.lastError() != ERROR_NOT_SAME_DEVICE)
                x.rethrowAsIOException(source, target);
        }

        if (!sourceAttrs.isDirectory() && !sourceAttrs.isDirectoryLink()) {
            try {
                MoveFileEx(sourcePath, targetPath, MOVEFILE_COPY_ALLOWED);
            } catch (WindowsException x) {
                x.rethrowAsIOException(source, target);
            }
            try {
                copySecurityAttributes(source, target, false);
            } catch (IOException x) {
            }
            return;
        }

        assert sourceAttrs.isDirectory() || sourceAttrs.isDirectoryLink();

        try {
            if (sourceAttrs.isDirectory()) {
                ensureEmptyDir(source);
                CreateDirectory(targetPath, 0L);
            } else {
                String linkTarget = WindowsLinkSupport.readLink(source);
                WindowsLinkSupport.createSymbolicLink(targetPath,
                                                      WindowsPath.addPrefixIfNeeded(linkTarget),
                                                      SYMBOLIC_LINK_FLAG_DIRECTORY);
            }
        } catch (WindowsException x) {
            x.rethrowAsIOException(target);
        }

        WindowsFileAttributeViews.Dos view =
                WindowsFileAttributeViews.createDosView(target, false);
        try {
            view.setAttributes(sourceAttrs);
        } catch (IOException x) {
            try {
                RemoveDirectory(targetPath);
            } catch (WindowsException ignore) { }
            throw x;
        }

        try {
            copySecurityAttributes(source, target, false);
        } catch (IOException ignore) { }

        try {
            RemoveDirectory(sourcePath);
        } catch (WindowsException x) {
            try {
                RemoveDirectory(targetPath);
            } catch (WindowsException ignore) { }
            if (x.lastError() == ERROR_DIR_NOT_EMPTY ||
                x.lastError() == ERROR_ALREADY_EXISTS)
            {
                throw new DirectoryNotEmptyException(
                    target.getPathForExceptionMessage());
            }
            x.rethrowAsIOException(source);
        }
    }


    private static String asWin32Path(WindowsPath path) throws IOException {
        try {
            return path.getPathForWin32Calls();
        } catch (WindowsException x) {
            x.rethrowAsIOException(path);
            return null;
        }
    }

    /**
     * Copy DACL/owner/group from source to target
     */
    private static void copySecurityAttributes(WindowsPath source,
                                               WindowsPath target,
                                               boolean followLinks)
        throws IOException
    {
        String path = WindowsLinkSupport.getFinalPath(source, followLinks);

        WindowsSecurity.Privilege priv =
            WindowsSecurity.enablePrivilege("SeRestorePrivilege");
        try {
            int request = (DACL_SECURITY_INFORMATION |
                OWNER_SECURITY_INFORMATION | GROUP_SECURITY_INFORMATION);
            try (NativeBuffer buffer =
                 WindowsAclFileAttributeView.getFileSecurity(path, request)) {
                SetFileSecurity(target.getPathForWin32Calls(), request,
                        buffer.address());
            } catch (WindowsException x) {
                x.rethrowAsIOException(target);
            }
        } finally {
            priv.drop();
        }
    }
}
