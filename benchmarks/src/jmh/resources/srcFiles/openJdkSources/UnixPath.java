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
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.file.DirectoryStream;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Objects;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.util.ArraysSupport;

import static sun.nio.fs.UnixConstants.*;
import static sun.nio.fs.UnixNativeDispatcher.*;

/**
 * Linux/Mac implementation of java.nio.file.Path
 */
class UnixPath implements Path {

    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    private final UnixFileSystem fs;

    private final byte[] path;

    private String stringValue;

    private int hash;

    private volatile int[] offsets;

    UnixPath(UnixFileSystem fs, byte[] path) {
        this.fs = fs;
        this.path = path;
    }

    UnixPath(UnixFileSystem fs, String input) {
        this(fs, encode(fs, normalizeAndCheck(input)));
    }

    static String normalizeAndCheck(String input) {
        int n = input.length();
        char prevChar = 0;
        for (int i=0; i < n; i++) {
            char c = input.charAt(i);
            if ((c == '/') && (prevChar == '/'))
                return normalize(input, n, i - 1);
            checkNotNul(input, c);
            prevChar = c;
        }
        if (prevChar == '/' && n > 1) {
            return input.substring(0, n - 1);
        }
        return input;
    }

    private static void checkNotNul(String input, char c) {
        if (c == '\u0000')
            throw new InvalidPathException(input, "Nul character not allowed");
    }

    private static String normalize(String input, int len, int off) {
        if (len == 0)
            return input;
        int n = len;
        while ((n > 0) && (input.charAt(n - 1) == '/')) n--;
        if (n == 0)
            return "/";
        StringBuilder sb = new StringBuilder(input.length());
        if (off > 0)
            sb.append(input, 0, off);
        char prevChar = 0;
        for (int i=off; i < n; i++) {
            char c = input.charAt(i);
            if ((c == '/') && (prevChar == '/'))
                continue;
            checkNotNul(input, c);
            sb.append(c);
            prevChar = c;
        }
        return sb.toString();
    }

    private static byte[] encode(UnixFileSystem fs, String input) {
        input = fs.normalizeNativePath(input);
        try {
            return JLA.getBytesNoRepl(input, Util.jnuEncoding());
        } catch (CharacterCodingException cce) {
            throw new InvalidPathException(input,
                "Malformed input or input contains unmappable characters");
        }
    }

    byte[] asByteArray() {
        return path;
    }

    byte[] getByteArrayForSysCalls() {
        if (getFileSystem().needToResolveAgainstDefaultDirectory()) {
            return resolve(getFileSystem().defaultDirectory(), path);
        } else {
            if (!isEmpty()) {
                return path;
            } else {
                byte[] here = { '.' };
                return here;
            }
        }
    }

    String getPathForExceptionMessage() {
        return toString();
    }

    String getPathForPermissionCheck() {
        if (getFileSystem().needToResolveAgainstDefaultDirectory()) {
            return Util.toString(getByteArrayForSysCalls());
        } else {
            return toString();
        }
    }

    static UnixPath toUnixPath(Path obj) {
        if (obj == null)
            throw new NullPointerException();
        if (!(obj instanceof UnixPath))
            throw new ProviderMismatchException();
        return (UnixPath)obj;
    }

    private void initOffsets() {
        if (offsets == null) {
            int count, index;

            count = 0;
            index = 0;
            if (isEmpty()) {
                count = 1;
            } else {
                while (index < path.length) {
                    byte c = path[index++];
                    if (c != '/') {
                        count++;
                        while (index < path.length && path[index] != '/')
                            index++;
                    }
                }
            }

            int[] result = new int[count];
            count = 0;
            index = 0;
            while (index < path.length) {
                byte c = path[index];
                if (c == '/') {
                    index++;
                } else {
                    result[count++] = index++;
                    while (index < path.length && path[index] != '/')
                        index++;
                }
            }
            synchronized (this) {
                if (offsets == null)
                    offsets = result;
            }
        }
    }

    boolean isEmpty() {
        return path.length == 0;
    }

    private UnixPath emptyPath() {
        return new UnixPath(getFileSystem(), new byte[0]);
    }


    private boolean hasDotOrDotDot() {
        int n = getNameCount();
        for (int i=0; i<n; i++) {
            byte[] bytes = getName(i).path;
            if ((bytes.length == 1 && bytes[0] == '.'))
                return true;
            if ((bytes.length == 2 && bytes[0] == '.') && bytes[1] == '.') {
                return true;
            }
        }
        return false;
    }

    @Override
    public UnixFileSystem getFileSystem() {
        return fs;
    }

    @Override
    public UnixPath getRoot() {
        if (path.length > 0 && path[0] == '/') {
            return getFileSystem().rootDirectory();
        } else {
            return null;
        }
    }

    @Override
    public UnixPath getFileName() {
        initOffsets();

        int count = offsets.length;

        if (count == 0)
            return null;

        if (count == 1 && path.length > 0 && path[0] != '/')
            return this;

        int lastOffset = offsets[count-1];
        int len = path.length - lastOffset;
        byte[] result = new byte[len];
        System.arraycopy(path, lastOffset, result, 0, len);
        return new UnixPath(getFileSystem(), result);
    }

    @Override
    public UnixPath getParent() {
        initOffsets();

        int count = offsets.length;
        if (count == 0) {
            return null;
        }
        int len = offsets[count-1] - 1;
        if (len <= 0) {
            return getRoot();
        }
        byte[] result = new byte[len];
        System.arraycopy(path, 0, result, 0, len);
        return new UnixPath(getFileSystem(), result);
    }

    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    @Override
    public UnixPath getName(int index) {
        initOffsets();
        if (index < 0)
            throw new IllegalArgumentException();
        if (index >= offsets.length)
            throw new IllegalArgumentException();

        int begin = offsets[index];
        int len;
        if (index == (offsets.length-1)) {
            len = path.length - begin;
        } else {
            len = offsets[index+1] - begin - 1;
        }

        byte[] result = new byte[len];
        System.arraycopy(path, begin, result, 0, len);
        return new UnixPath(getFileSystem(), result);
    }

    @Override
    public UnixPath subpath(int beginIndex, int endIndex) {
        initOffsets();

        if (beginIndex < 0)
            throw new IllegalArgumentException();
        if (beginIndex >= offsets.length)
            throw new IllegalArgumentException();
        if (endIndex > offsets.length)
            throw new IllegalArgumentException();
        if (beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }

        int begin = offsets[beginIndex];
        int len;
        if (endIndex == offsets.length) {
            len = path.length - begin;
        } else {
            len = offsets[endIndex] - begin - 1;
        }

        byte[] result = new byte[len];
        System.arraycopy(path, begin, result, 0, len);
        return new UnixPath(getFileSystem(), result);
    }

    @Override
    public boolean isAbsolute() {
        return (path.length > 0 && path[0] == '/');
    }

    private static byte[] resolve(byte[] base, byte[] child) {
        int baseLength = base.length;
        int childLength = child.length;
        if (childLength == 0)
            return base;
        if (baseLength == 0 || child[0] == '/')
            return child;
        byte[] result;
        if (baseLength == 1 && base[0] == '/') {
            result = new byte[childLength + 1];
            result[0] = '/';
            System.arraycopy(child, 0, result, 1, childLength);
        } else {
            result = new byte[baseLength + 1 + childLength];
            System.arraycopy(base, 0, result, 0, baseLength);
            result[base.length] = '/';
            System.arraycopy(child, 0, result, baseLength+1, childLength);
        }
        return result;
    }

    @Override
    public UnixPath resolve(Path obj) {
        byte[] other = toUnixPath(obj).path;
        if (other.length > 0 && other[0] == '/')
            return ((UnixPath)obj);
        byte[] result = resolve(path, other);
        return new UnixPath(getFileSystem(), result);
    }

    UnixPath resolve(byte[] other) {
        return resolve(new UnixPath(getFileSystem(), other));
    }

   private static final byte[] resolve(byte[] base, byte[]... children) {
       int start = 0;
       int resultLength = base.length;

       final int count = children.length;
       if (count > 0) {
           for (int i = 0; i < count; i++) {
               byte[] b = children[i];
               if (b.length > 0) {
                   if (b[0] == '/') {
                       start = i + 1;
                       resultLength = b.length;
                   } else {
                       if (resultLength > 0)
                           resultLength++;
                       resultLength += b.length;
                   }
               }
           }
       }

       if (start == 0 && resultLength > base.length && base.length == 1 && base[0] == '/')
           resultLength--;

       byte[] result = new byte[resultLength];
       if (result.length == 0)
           return result;

       int offset = 0;
       if (start == 0 && base.length > 0) {
           System.arraycopy(base, 0, result, 0, base.length);
           offset += base.length;
       }

       if (count > 0) {
           int idx = Math.max(0, start - 1);
           for (int i = idx; i < count; i++) {
               byte[] b = children[i];
               if (b.length > 0) {
                   if (offset > 0 && result[offset - 1] != '/')
                       result[offset++] = '/';
                   System.arraycopy(b, 0, result, offset, b.length);
                   offset += b.length;
               }
           }
       }

       return result;
   }

    @Override
    public UnixPath resolve(Path first, Path... more) {
        if (more.length == 0)
            return resolve(first);

        byte[][] children = new byte[1 + more.length][];
        children[0] = toUnixPath(first).path;
        for (int i = 0; i < more.length; i++)
            children[i + 1] = toUnixPath(more[i]).path;

        byte[] result = resolve(path, children);
        return new UnixPath(getFileSystem(), result);
    }

    @Override
    public UnixPath relativize(Path obj) {
        UnixPath child = toUnixPath(obj);
        if (child.equals(this))
            return emptyPath();

        if (this.isAbsolute() != child.isAbsolute())
            throw new IllegalArgumentException("'other' is different type of Path");

        if (this.isEmpty())
            return child;

        UnixPath base = this;
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

        UnixPath childRemaining;
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

        UnixPath baseRemaining = base.subpath(i, baseCount);
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

        int len = dotdots*3 + childRemaining.path.length;
        if (isChildEmpty) {
            assert childRemaining.isEmpty();
            len--;
        }
        byte[] result = new byte[len];
        int pos = 0;
        while (dotdots > 0) {
            result[pos++] = (byte)'.';
            result[pos++] = (byte)'.';
            if (isChildEmpty) {
                if (dotdots > 1) result[pos++] = (byte)'/';
            } else {
                result[pos++] = (byte)'/';
            }
            dotdots--;
        }
        System.arraycopy(childRemaining.path,0, result, pos,
                             childRemaining.path.length);
        return new UnixPath(getFileSystem(), result);
    }

    @Override
    public UnixPath normalize() {
        final int count = getNameCount();
        if (count == 0 || isEmpty())
            return this;

        boolean[] ignore = new boolean[count];      
        int[] size = new int[count];                
        int remaining = count;                      
        boolean hasDotDot = false;                  
        boolean isAbsolute = isAbsolute();

        for (int i=0; i<count; i++) {
            int begin = offsets[i];
            int len;
            if (i == (offsets.length-1)) {
                len = path.length - begin;
            } else {
                len = offsets[i+1] - begin - 1;
            }
            size[i] = len;

            if (path[begin] == '.') {
                if (len == 1) {
                    ignore[i] = true;  
                    remaining--;
                }
                else {
                    if (path[begin+1] == '.')   
                        hasDotDot = true;
                }
            }
        }

        if (hasDotDot) {
            int prevRemaining;
            do {
                prevRemaining = remaining;
                int prevName = -1;
                for (int i=0; i<count; i++) {
                    if (ignore[i])
                        continue;

                    if (size[i] != 2) {
                        prevName = i;
                        continue;
                    }

                    int begin = offsets[i];
                    if (path[begin] != '.' || path[begin+1] != '.') {
                        prevName = i;
                        continue;
                    }

                    if (prevName >= 0) {
                        ignore[prevName] = true;
                        ignore[i] = true;
                        remaining = remaining - 2;
                        prevName = -1;
                    } else {
                        if (isAbsolute) {
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
        }

        if (remaining == count)
            return this;

        if (remaining == 0) {
            return isAbsolute ? getFileSystem().rootDirectory() : emptyPath();
        }

        int len = remaining - 1;
        if (isAbsolute)
            len++;

        for (int i=0; i<count; i++) {
            if (!ignore[i])
                len += size[i];
        }
        byte[] result = new byte[len];

        int pos = 0;
        if (isAbsolute)
            result[pos++] = '/';
        for (int i=0; i<count; i++) {
            if (!ignore[i]) {
                System.arraycopy(path, offsets[i], result, pos, size[i]);
                pos += size[i];
                if (--remaining > 0) {
                    result[pos++] = '/';
                }
            }
        }
        return new UnixPath(getFileSystem(), result);
    }

    @Override
    public boolean startsWith(Path other) {
        if (!(Objects.requireNonNull(other) instanceof UnixPath))
            return false;
        UnixPath that = (UnixPath)other;

        if (that.path.length > path.length)
            return false;

        int thisOffsetCount = getNameCount();
        int thatOffsetCount = that.getNameCount();

        if (thatOffsetCount == 0 && this.isAbsolute()) {
            return that.isEmpty() ? false : true;
        }

        if (thatOffsetCount > thisOffsetCount)
            return false;

        if ((thatOffsetCount == thisOffsetCount) &&
            (path.length != that.path.length)) {
            return false;
        }

        for (int i=0; i<thatOffsetCount; i++) {
            Integer o1 = offsets[i];
            Integer o2 = that.offsets[i];
            if (!o1.equals(o2))
                return false;
        }

        int i=0;
        while (i < that.path.length) {
            if (this.path[i] != that.path[i])
                return false;
            i++;
        }

        if (i < path.length && this.path[i] != '/')
            return false;

        return true;
    }

    @Override
    public boolean endsWith(Path other) {
        if (!(Objects.requireNonNull(other) instanceof UnixPath))
            return false;
        UnixPath that = (UnixPath)other;

        int thisLen = path.length;
        int thatLen = that.path.length;

        if (thatLen > thisLen)
            return false;

        if (thisLen > 0 && thatLen == 0)
            return false;

        if (that.isAbsolute() && !this.isAbsolute())
            return false;

        int thisOffsetCount = getNameCount();
        int thatOffsetCount = that.getNameCount();

        if (thatOffsetCount > thisOffsetCount) {
            return false;
        } else {
            if (thatOffsetCount == thisOffsetCount) {
                if (thisOffsetCount == 0)
                    return true;
                int expectedLen = thisLen;
                if (this.isAbsolute() && !that.isAbsolute())
                    expectedLen--;
                if (thatLen != expectedLen)
                    return false;
            } else {
                if (that.isAbsolute())
                    return false;
            }
        }

        int thisPos = offsets[thisOffsetCount - thatOffsetCount];
        int thatPos = that.offsets[0];
        return Arrays.equals(this.path, thisPos, thisLen, that.path, thatPos, thatLen);
    }

    @Override
    public int compareTo(Path other) {
        return Arrays.compareUnsigned(path, ((UnixPath) other).path);
    }

    @Override
    public boolean equals(Object ob) {
        return ob instanceof UnixPath p && compareTo(p) == 0;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = ArraysSupport.vectorizedHashCode(path, 0, path.length, 0,
                    /* unsigned bytes */ ArraysSupport.T_BOOLEAN);
            hash = h;
        }
        return h;
    }

    @Override
    public String toString() {
        String stringValue = this.stringValue;
        if (stringValue == null) {
            this.stringValue = stringValue = fs.normalizeJavaPath(Util.toString(path));     
        }
        return stringValue;
    }


    int openForAttributeAccess(boolean followLinks) throws UnixException {
        int flags = O_RDONLY;
        if (!followLinks) {
            if (O_NOFOLLOW == 0)
                throw new UnixException
                    ("NOFOLLOW_LINKS is not supported on this platform");
            flags |= O_NOFOLLOW;
        }
        return open(this, flags, 0);
    }

    void checkRead() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkRead(getPathForPermissionCheck());
    }

    void checkWrite() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkWrite(getPathForPermissionCheck());
    }

    void checkDelete() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkDelete(getPathForPermissionCheck());
    }

    @Override
    public UnixPath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        }
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPropertyAccess("user.dir");
        }
        return new UnixPath(getFileSystem(),
            resolve(getFileSystem().defaultDirectory(), path));
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        checkRead();

        UnixPath absolute = toAbsolutePath();

        if (Util.followLinks(options)) {
            try {
                byte[] rp = realpath(absolute);
                return new UnixPath(getFileSystem(), rp);
            } catch (UnixException x) {
                x.rethrowAsIOException(this);
            }
        }

        UnixPath result = fs.rootDirectory();
        boolean parentIsDotDot = false;
        for (int i = 0; i < absolute.getNameCount(); i++) {
            UnixPath element = absolute.getName(i);

            if ((element.asByteArray().length == 1) &&
                (element.asByteArray()[0] == '.'))
                continue;

            if ((element.asByteArray().length == 2) &&
                (element.asByteArray()[0] == '.') &&
                (element.asByteArray()[1] == '.'))
            {
                UnixFileAttributes attrs = null;
                try {
                    attrs = UnixFileAttributes.get(result, false);
                } catch (UnixException x) {
                    x.rethrowAsIOException(result);
                }
                if (!attrs.isSymbolicLink() && !parentIsDotDot) {
                    result = result.getParent();
                    if (result == null) {
                        result = fs.rootDirectory();
                    }
                    continue;
                }
                parentIsDotDot = true;
            } else {
                parentIsDotDot = false;
            }
            result = result.resolve(element);
        }

        try {
            UnixFileAttributes.get(result, false);
        } catch (UnixException x) {
            x.rethrowAsIOException(result);
        }

        if (!fs.isCaseInsensitiveAndPreserving())
            return result;

        UnixPath path = fs.rootDirectory();

        for (int i = 0; i < result.getNameCount(); i++ ) {
            UnixPath element = result.getName(i);

            if (element.toString().equals("..")) {
                path = path.resolve(element);
                continue;
            }

            UnixPath elementPath = path.resolve(element);

            UnixFileAttributes attrs = null;
            try {
                attrs = UnixFileAttributes.get(elementPath, false);
            } catch (UnixException x) {
                x.rethrowAsIOException(result);
            }
            final UnixFileKey elementKey = attrs.fileKey();

            long dp = -1;
            try {
                dp = opendir(path);
            } catch (UnixException x) {
                x.rethrowAsIOException(path);
            }

            DirectoryStream.Filter<Path> filter = (p) -> { return true; };
            try (DirectoryStream<Path> entries = new UnixDirectoryStream(path, dp, filter)) {
                boolean found = false;
                for (Path entry : entries) {
                    UnixPath p = path.resolve(entry.getFileName());
                    UnixFileAttributes attributes = null;
                    try {
                        attributes = UnixFileAttributes.get(p, false);
                        UnixFileKey key = attributes.fileKey();
                        if (key.equals(elementKey)) {
                            path = path.resolve(entry);
                            found = true;
                            break;
                        }
                    } catch (UnixException ignore) {
                        continue;
                    }
                }

                if (!found) {
                    path = path.resolve(element);
                }
            }
        }

        return path;
    }

    @Override
    public URI toUri() {
        return UnixUriUtils.toUri(this);
    }

    @Override
    public WatchKey register(WatchService watcher,
                             WatchEvent.Kind<?>[] events,
                             WatchEvent.Modifier... modifiers)
        throws IOException
    {
        if (watcher == null)
            throw new NullPointerException();
        if (!(watcher instanceof AbstractWatchService))
            throw new ProviderMismatchException();
        checkRead();
        return ((AbstractWatchService)watcher).register(this, events, modifiers);
    }
}
