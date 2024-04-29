/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.preallocate;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Structure;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

final class MacOsPreallocator extends AbstractPosixPreallocator {

    MacOsPreallocator() {
        super(new PosixConstants(144, 96, 512));
    }

    @Override
    public boolean useNative() {
        return Natives.NATIVES_AVAILABLE && super.useNative();
    }

    @Override
    public int preallocate(final int fd, final long currentSize /* unused */ , final long fileSize) {
        final Natives.Fcntl.FStore fst = AccessController.doPrivileged((PrivilegedAction<Natives.Fcntl.FStore>) Natives.Fcntl.FStore::new);
        fst.fst_flags = Natives.Fcntl.F_ALLOCATECONTIG;
        fst.fst_posmode = Natives.Fcntl.F_PEOFPOSMODE;
        fst.fst_offset = new NativeLong(0);
        fst.fst_length = new NativeLong(fileSize);
        if (Natives.fcntl(fd, Natives.Fcntl.F_PREALLOCATE, fst) != 0) {
            fst.fst_flags = Natives.Fcntl.F_ALLOCATEALL;
            if (Natives.fcntl(fd, Natives.Fcntl.F_PREALLOCATE, fst) != 0) {
                return Native.getLastError();
            }
        }
        if (Natives.ftruncate(fd, new NativeLong(fileSize)) != 0) {
            return Native.getLastError();
        }
        return 0;
    }

    private static class Natives {

        static boolean NATIVES_AVAILABLE;

        static {
            NATIVES_AVAILABLE = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                try {
                    Native.register(Natives.class, Platform.C_LIBRARY_NAME);
                } catch (final UnsatisfiedLinkError e) {
                    return false;
                }
                return true;
            });
        }

        static class Fcntl {
            private static final int F_PREALLOCATE = 42;

            @SuppressWarnings("unused")
            private static final int F_ALLOCATECONTIG = 0x00000002; 
            private static final int F_ALLOCATEALL = 0x00000004; 

            private static final int F_PEOFPOSMODE = 3; 
            @SuppressWarnings("unused")
            private static final int F_VOLPOSMODE = 4; 

            public static final class FStore extends Structure implements Structure.ByReference {
                public int fst_flags = 0;
                public int fst_posmode = 0;
                public NativeLong fst_offset = new NativeLong(0);
                public NativeLong fst_length = new NativeLong(0);
                @SuppressWarnings("unused")
                public NativeLong fst_bytesalloc = new NativeLong(0);

                @Override
                protected List<String> getFieldOrder() {
                    return Arrays.asList("fst_flags", "fst_posmode", "fst_offset", "fst_length", "fst_bytesalloc");
                }

            }
        }

        static native int fcntl(int fd, int cmd, Fcntl.FStore fst);

        static native int ftruncate(int fd, NativeLong length);
    }

}
