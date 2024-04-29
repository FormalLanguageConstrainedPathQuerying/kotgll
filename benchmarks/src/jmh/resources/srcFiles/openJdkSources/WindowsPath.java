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

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.lang.ref.WeakReference;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/**
 * Windows implementation of Path
 */

class WindowsPath implements Path {

    private static final int MAX_PATH = 247;

    private static final int MAX_LONG_PATH = 32000;

    private final WindowsFileSystem fs;

    private final WindowsPathType type;
    private final String root;
    private final String path;

    private volatile WeakReference<String> pathForWin32Calls;

    private volatile Integer[] offsets;

    private int hash;


    /**
     * Initializes a new instance of this class.
     */
    private WindowsPath(WindowsFileSystem fs,
                        WindowsPathType type,
                        String root,
                        String path)
    {
        this.fs = fs;
        this.type = type;
        this.root = root;
        this.path = path;
    }

    /**
     * Creates a Path by parsing the given path.
     */
    static WindowsPath parse(WindowsFileSystem fs, String path) {
        WindowsPathParser.Result result = WindowsPathParser.parse(path);
        return new WindowsPath(fs, result.type(), result.root(), result.path());
    }

    /**
     * Creates a Path from a given path that is known to be normalized.
     */
    static WindowsPath createFromNormalizedPath(WindowsFileSystem fs,
                                                String path,
                                                BasicFileAttributes attrs)
    {
        try {
            WindowsPathParser.Result result =
                WindowsPathParser.parseNormalizedPath(path);
            if (attrs == null) {
                return new WindowsPath(fs,
                                       result.type(),
                                       result.root(),
                                       result.path());
            } else {
                return new WindowsPathWithAttributes(fs,
                                                     result.type(),
                                                     result.root(),
                                                     result.path(),
                                                     attrs);
            }
        } catch (InvalidPathException x) {
            throw new AssertionError(x.getMessage());
        }
    }

    /**
     * Creates a WindowsPath from a given path that is known to be normalized.
     */
    static WindowsPath createFromNormalizedPath(WindowsFileSystem fs,
                                                String path)
    {
        return createFromNormalizedPath(fs, path, null);
    }

    /**
     * Special implementation with attached/cached attributes (used to quicken
     * file tree traversal)
     */
    private static class WindowsPathWithAttributes
        extends WindowsPath implements BasicFileAttributesHolder
    {
        final WeakReference<BasicFileAttributes> ref;

        WindowsPathWithAttributes(WindowsFileSystem fs,
                                  WindowsPathType type,
                                  String root,
                                  String path,
                                  BasicFileAttributes attrs)
        {
            super(fs, type, root, path);
            ref = new WeakReference<BasicFileAttributes>(attrs);
        }

        @Override
        public BasicFileAttributes get() {
            return ref.get();
        }

        @Override
        public void invalidate() {
            ref.clear();
        }

    }

    String getPathForExceptionMessage() {
        return path;
    }

    String getPathForPermissionCheck() {
        return path;
    }

    String getPathForWin32Calls() throws WindowsException {
        return getPathForWin32Calls(true);
    }

    String getPathWithPrefixForWin32Calls() throws WindowsException {
        return getPathForWin32Calls(false);
    }

    private String getPathForWin32Calls(boolean allowShortPath) throws WindowsException {
        if (allowShortPath) {
            if (isAbsolute() && path.length() <= MAX_PATH)
                return path;

            WeakReference<String> ref = pathForWin32Calls;
            String cached = (ref != null) ? ref.get() : null;
            if (cached != null) {
                return cached;
            }
        }

        String resolved = getAbsolutePath();

        if (resolved.length() > MAX_PATH || !allowShortPath) {
            if (resolved.length() > MAX_LONG_PATH) {
                throw new WindowsException("Cannot access file with path exceeding "
                    + MAX_LONG_PATH + " characters");
            }
            resolved = addPrefix(GetFullPathName(resolved));
        }

        if (allowShortPath && type != WindowsPathType.DRIVE_RELATIVE) {
            synchronized (this) {
                pathForWin32Calls = new WeakReference<String>(resolved);
            }
        }
        return resolved;
    }

    private String getAbsolutePath() throws WindowsException {
        if (isAbsolute())
            return path;

        if (type == WindowsPathType.RELATIVE) {
            String defaultDirectory = getFileSystem().defaultDirectory();
            if (isEmpty())
                return defaultDirectory;
            if (defaultDirectory.endsWith("\\")) {
                return defaultDirectory + path;
            } else {
                StringBuilder sb =
                    new StringBuilder(defaultDirectory.length() + path.length() + 1);
                return sb.append(defaultDirectory).append('\\').append(path).toString();
            }
        }

        if (type == WindowsPathType.DIRECTORY_RELATIVE) {
            String defaultRoot = getFileSystem().defaultRoot();
            return defaultRoot + path.substring(1);
        }

        if (isSameDrive(root, getFileSystem().defaultRoot())) {
            String remaining = path.substring(root.length());
            String defaultDirectory = getFileSystem().defaultDirectory();
            if (remaining.isEmpty()) {
                return defaultDirectory;
            } else if (defaultDirectory.endsWith("\\")) {
                 return defaultDirectory + remaining;
            } else {
                return defaultDirectory + "\\" + remaining;
            }
        } else {
            String wd;
            try {
                int dt = GetDriveType(root + "\\");
                if (dt == DRIVE_UNKNOWN || dt == DRIVE_NO_ROOT_DIR)
                    throw new WindowsException("");
                wd = GetFullPathName(root + ".");
            } catch (WindowsException x) {
                throw new WindowsException("Unable to get working directory of drive '" +
                    Character.toUpperCase(root.charAt(0)) + "'");
            }
            String result = wd;
            if (wd.endsWith("\\")) {
                result += path.substring(root.length());
            } else {
                if (path.length() > root.length())
                    result += "\\" + path.substring(root.length());
            }
            return result;
        }
    }

    private static boolean isSameDrive(String root1, String root2) {
        return Character.toUpperCase(root1.charAt(0)) ==
               Character.toUpperCase(root2.charAt(0));
    }

    static String addPrefix(String path) {
        if (path.startsWith("\\\\")) {
            path = "\\\\?\\UNC" + path.substring(1, path.length());
        } else {
            path = "\\\\?\\" + path;
        }
        return path;
    }

    static String addPrefixIfNeeded(String path) {
        if (path.length() > MAX_PATH) {
            return addPrefix(path);
        }
        return path;
    }

    @Override
    public WindowsFileSystem getFileSystem() {
        return fs;
    }


    private boolean isEmpty() {
        return path.isEmpty();
    }

    private WindowsPath emptyPath() {
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", "");
    }

    @Override
    public Path getFileName() {
        int len = path.length();
        if (len == 0)
            return this;
        if (root.length() == len)
            return null;
        int off = path.lastIndexOf('\\');
        if (off < root.length())
            off = root.length();
        else
            off++;
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", path.substring(off));
    }

    @Override
    public WindowsPath getParent() {
        if (root.length() == path.length())
            return null;
        int off = path.lastIndexOf('\\');
        if (off < root.length())
            return getRoot();
        else
            return new WindowsPath(getFileSystem(),
                                   type,
                                   root,
                                   path.substring(0, off));
    }

    @Override
    public WindowsPath getRoot() {
        if (root.isEmpty())
            return null;
        return new WindowsPath(getFileSystem(), type, root, root);
    }

    WindowsPathType type() {
        return type;
    }

    boolean isUnc() {
        return type == WindowsPathType.UNC;
    }

    boolean needsSlashWhenResolving() {
        if (path.endsWith("\\"))
            return false;
        return path.length() > root.length();
    }

    @Override
    public boolean isAbsolute() {
        return type == WindowsPathType.ABSOLUTE || type == WindowsPathType.UNC;
    }

    static WindowsPath toWindowsPath(Path path) {
        if (path == null)
            throw new NullPointerException();
        if (!(path instanceof WindowsPath)) {
            throw new ProviderMismatchException();
        }
        return (WindowsPath)path;
    }

    private boolean hasDotOrDotDot() {
        int n = getNameCount();
        for (int i=0; i<n; i++) {
            String name = elementAsString(i);
            if (name.length() == 1 && name.charAt(0) == '.')
                return true;
            if (name.length() == 2
                    && name.charAt(0) == '.' && name.charAt(1) == '.')
                return true;
        }
        return false;
    }

    @Override
    public WindowsPath relativize(Path obj) {
        WindowsPath child = toWindowsPath(obj);
        if (this.equals(child))
            return emptyPath();

        if (this.type != child.type)
            throw new IllegalArgumentException("'other' is different type of Path");

        if (!this.root.equalsIgnoreCase(child.root))
            throw new IllegalArgumentException("'other' has different root");

        if (this.isEmpty())
            return child;


        WindowsPath base = this;
        if (base.hasDotOrDotDot() || child.hasDotOrDotDot()) {
            base = base.normalize();
            child = child.normalize();
        }

        int baseCount = base.getNameCount();
        int childCount = child.getNameCount();

        int n = Math.min(baseCount, childCount);
        int i = 0;
        while (i < n) {
            if (!base.getName(i).equals(child.getName(i)))
                break;
            i++;
        }

        WindowsPath childRemaining;
        boolean isChildEmpty;
        if (i == childCount) {
            childRemaining = emptyPath();
            isChildEmpty = true;
        } else {
            childRemaining = child.subpath(i, childCount);
            isChildEmpty = childRemaining.isEmpty();
        }

        if (i == baseCount) {
            return childRemaining;
        }

        WindowsPath baseRemaining = base.subpath(i, baseCount);
        if (baseRemaining.hasDotOrDotDot()) {
            throw new IllegalArgumentException("Unable to compute relative "
                    + " path from " + this + " to " + obj);
        }
        if (baseRemaining.isEmpty())
            return childRemaining;

        int dotdots = baseRemaining.getNameCount();
        if (dotdots == 0) {
            return childRemaining;
        }

        StringBuilder result = new StringBuilder();
        for (int j=0; j<dotdots; j++) {
            result.append("..\\");
        }

        if (!isChildEmpty) {
            for (int j=0; j<childRemaining.getNameCount(); j++) {
                result.append(childRemaining.getName(j).toString());
                result.append("\\");
            }
        }

        result.setLength(result.length()-1);
        return createFromNormalizedPath(getFileSystem(), result.toString());
    }

    @Override
    public WindowsPath normalize() {
        final int count = getNameCount();
        if (count == 0 || isEmpty())
            return this;

        boolean[] ignore = new boolean[count];      
        int remaining = count;                      

        int prevRemaining;
        do {
            prevRemaining = remaining;
            int prevName = -1;
            for (int i=0; i<count; i++) {
                if (ignore[i])
                    continue;

                String name = elementAsString(i);

                if (name.length() > 2) {
                    prevName = i;
                    continue;
                }

                if (name.length() == 1) {
                    if (name.charAt(0) == '.') {
                        ignore[i] = true;
                        remaining--;
                    } else {
                        prevName = i;
                    }
                    continue;
                }

                if (name.charAt(0) != '.' || name.charAt(1) != '.') {
                    prevName = i;
                    continue;
                }

                if (prevName >= 0) {
                    ignore[prevName] = true;
                    ignore[i] = true;
                    remaining = remaining - 2;
                    prevName = -1;
                } else {
                    if (isAbsolute() || type == WindowsPathType.DIRECTORY_RELATIVE) {
                        boolean hasPrevious = false;
                        for (int j=0; j<i; j++) {
                            if (!ignore[j]) {
                                hasPrevious = true;
                                break;
                            }
                        }
                        if (!hasPrevious) {
                            ignore[i] = true;
                            remaining--;
                        }
                    }
                }
            }
        } while (prevRemaining > remaining);

        if (remaining == count)
            return this;

        if (remaining == 0) {
            return root.isEmpty() ? emptyPath() : getRoot();
        }

        StringBuilder result = new StringBuilder();
        if (root != null)
            result.append(root);
        for (int i=0; i<count; i++) {
            if (!ignore[i]) {
                result.append(getName(i));
                result.append("\\");
            }
        }

        result.setLength(result.length()-1);
        return createFromNormalizedPath(getFileSystem(), result.toString());
    }

    @Override
    public WindowsPath resolve(Path obj) {
        WindowsPath other = toWindowsPath(obj);
        if (other.isEmpty())
            return this;
        if (other.isAbsolute())
            return other;

        switch (other.type) {
            case RELATIVE: {
                String result;
                if (path.endsWith("\\") || (root.length() == path.length())) {
                    result = path + other.path;
                } else {
                    result = path + "\\" + other.path;
                }
                return new WindowsPath(getFileSystem(), type, root, result);
            }

            case DIRECTORY_RELATIVE: {
                String result;
                if (root.endsWith("\\")) {
                    result = root + other.path.substring(1);
                } else {
                    result = root + other.path;
                }
                return createFromNormalizedPath(getFileSystem(), result);
            }

            case DRIVE_RELATIVE: {
                if (!root.endsWith("\\"))
                    return other;
                String thisRoot = root.substring(0, root.length()-1);
                if (!thisRoot.equalsIgnoreCase(other.root))
                    return other;
                String remaining = other.path.substring(other.root.length());
                String result;
                if (path.endsWith("\\")) {
                    result = path + remaining;
                } else {
                    result = path + "\\" + remaining;
                }
                return createFromNormalizedPath(getFileSystem(), result);
            }

            default:
                throw new AssertionError();
        }
    }

    private void initOffsets() {
        if (offsets == null) {
            ArrayList<Integer> list = new ArrayList<>();
            if (isEmpty()) {
                list.add(0);
            } else {
                int start = root.length();
                int off = root.length();
                while (off < path.length()) {
                    if (path.charAt(off) != '\\') {
                        off++;
                    } else {
                        list.add(start);
                        start = ++off;
                    }
                }
                if (start != off)
                    list.add(start);
            }
            synchronized (this) {
                if (offsets == null)
                    offsets = list.toArray(new Integer[list.size()]);
            }
        }
    }

    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    private String elementAsString(int i) {
        initOffsets();
        if (i == (offsets.length-1))
            return path.substring(offsets[i]);
        return path.substring(offsets[i], offsets[i+1]-1);
    }

    @Override
    public WindowsPath getName(int index) {
        initOffsets();
        if (index < 0 || index >= offsets.length)
            throw new IllegalArgumentException();
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", elementAsString(index));
    }

    @Override
    public WindowsPath subpath(int beginIndex, int endIndex) {
        initOffsets();
        if (beginIndex < 0)
            throw new IllegalArgumentException();
        if (beginIndex >= offsets.length)
            throw new IllegalArgumentException();
        if (endIndex > offsets.length)
            throw new IllegalArgumentException();
        if (beginIndex >= endIndex)
            throw new IllegalArgumentException();

        StringJoiner sb = new StringJoiner("\\");
        for (int i = beginIndex; i < endIndex; i++) {
            sb.add(elementAsString(i));
        }
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", sb.toString());
    }

    @Override
    public boolean startsWith(Path obj) {
        if (!(Objects.requireNonNull(obj) instanceof WindowsPath))
            return false;
        WindowsPath other = (WindowsPath)obj;

        if (!this.root.equalsIgnoreCase(other.root)) {
            return false;
        }

        if (other.isEmpty())
            return this.isEmpty();

        int thisCount = getNameCount();
        int otherCount = other.getNameCount();
        if (otherCount <= thisCount) {
            while (--otherCount >= 0) {
                String thisElement = this.elementAsString(otherCount);
                String otherElement = other.elementAsString(otherCount);
                if (!thisElement.equalsIgnoreCase(otherElement))
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean endsWith(Path obj) {
        if (!(Objects.requireNonNull(obj) instanceof WindowsPath))
            return false;
        WindowsPath other = (WindowsPath)obj;

        if (other.path.length() > this.path.length()) {
            return false;
        }

        if (other.isEmpty()) {
            return this.isEmpty();
        }

        int thisCount = this.getNameCount();
        int otherCount = other.getNameCount();

        if (otherCount > thisCount) {
            return false;
        }

        if (other.root.length() > 0) {
            if (otherCount < thisCount)
                return false;
            if (!this.root.equalsIgnoreCase(other.root))
                return false;
        }

        int off = thisCount - otherCount;
        while (--otherCount >= 0) {
            String thisElement = this.elementAsString(off + otherCount);
            String otherElement = other.elementAsString(otherCount);
            if (!thisElement.equalsIgnoreCase(otherElement))
                return false;
        }
        return true;
    }

    @Override
    public int compareTo(Path obj) {
        if (obj == null)
            throw new NullPointerException();
        String s1 = path;
        String s2 = ((WindowsPath)obj).path;
        int n1 = s1.length();
        int n2 = s2.length();
        int min = Math.min(n1, n2);
        for (int i = 0; i < min; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
             if (c1 != c2) {
                 c1 = Character.toUpperCase(c1);
                 c2 = Character.toUpperCase(c2);
                 if (c1 != c2) {
                     return c1 - c2;
                 }
             }
        }
        return n1 - n2;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WindowsPath other && compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            for (int i = 0; i < path.length(); i++) {
                h = 31*h + Character.toUpperCase(path.charAt(i));
            }
            hash = h;
        }
        return h;
    }

    @Override
    public String toString() {
        return path;
    }


    long openForReadAttributeAccess(boolean followLinks)
        throws WindowsException
    {
        int flags = FILE_FLAG_BACKUP_SEMANTICS;
        if (!followLinks)
            flags |= FILE_FLAG_OPEN_REPARSE_POINT;
        try {
            return openFileForReadAttributeAccess(flags);
        } catch (WindowsException e) {
            if (followLinks && e.lastError() == ERROR_CANT_ACCESS_FILE) {
                try {
                    return openSocketForReadAttributeAccess();
                } catch (WindowsException ignore) {}
            }
            throw e;
        }
    }

    private long openFileForReadAttributeAccess(int flags)
        throws WindowsException
    {
        return CreateFile(getPathForWin32Calls(),
                            FILE_READ_ATTRIBUTES,
                            (FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE),
                            0L,
                            OPEN_EXISTING,
                            flags);
    }

    /**
     * Returns a handle to the file if it is a socket.
     * Throws WindowsException if file is not a socket
     */
    private long openSocketForReadAttributeAccess()
        throws WindowsException
    {
        int flags = FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OPEN_REPARSE_POINT;

        long handle = openFileForReadAttributeAccess(flags);

        try {
            WindowsFileAttributes attrs = WindowsFileAttributes.readAttributes(handle);
            if (!attrs.isUnixDomainSocket()) {
                throw new WindowsException("not a socket");
            }
            return handle;
        } catch (WindowsException e) {
            CloseHandle(handle);
            throw e;
        }
    }

    void checkRead() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkRead(getPathForPermissionCheck());
        }
    }

    void checkWrite() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkWrite(getPathForPermissionCheck());
        }
    }

    void checkDelete() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkDelete(getPathForPermissionCheck());
        }
    }

    @Override
    public URI toUri() {
        return WindowsUriSupport.toUri(this);
    }

    @Override
    public WindowsPath toAbsolutePath() {
        if (isAbsolute())
            return this;

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPropertyAccess("user.dir");
        }

        try {
            return createFromNormalizedPath(getFileSystem(), getAbsolutePath());
        } catch (WindowsException x) {
            throw new IOError(new IOException(x.getMessage()));
        }
    }

    @Override
    public WindowsPath toRealPath(LinkOption... options) throws IOException {
        checkRead();
        String rp = WindowsLinkSupport.getRealPath(this, Util.followLinks(options));
        return createFromNormalizedPath(getFileSystem(), rp);
    }

    @Override
    public WatchKey register(WatchService watcher,
                             WatchEvent.Kind<?>[] events,
                             WatchEvent.Modifier... modifiers)
        throws IOException
    {
        if (watcher == null)
            throw new NullPointerException();
        if (!(watcher instanceof WindowsWatchService))
            throw new ProviderMismatchException();

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            boolean watchSubtree = false;
            final int ml = modifiers.length;
            if (ml > 0) {
                modifiers = Arrays.copyOf(modifiers, ml);
                int i=0;
                while (i < ml) {
                    if (ExtendedOptions.FILE_TREE.matches(modifiers[i++])) {
                        watchSubtree = true;
                        break;
                    }
                }
            }
            String s = getPathForPermissionCheck();
            sm.checkRead(s);
            if (watchSubtree)
                sm.checkRead(s + "\\-");
        }

        return ((WindowsWatchService)watcher).register(this, events, modifiers);
    }
}
