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
package jdk.internal.logger;

import java.io.FilePermission;
import java.lang.System.Logger;
import java.lang.System.LoggerFinder;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Locale;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.BooleanSupplier;

import jdk.internal.vm.annotation.Stable;
import sun.security.util.SecurityConstants;
import sun.security.action.GetBooleanAction;
import sun.security.action.GetPropertyAction;

/**
 * Helper class used to load the {@link java.lang.System.LoggerFinder}.
 */
public final class LoggerFinderLoader {
    private static volatile System.LoggerFinder service;
    private static final Object lock = new int[0];
    static final Permission CLASSLOADER_PERMISSION =
            SecurityConstants.GET_CLASSLOADER_PERMISSION;
    static final Permission READ_PERMISSION =
            new FilePermission("<<ALL FILES>>",
                    SecurityConstants.FILE_READ_ACTION);
    public static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");

    private static enum ErrorPolicy { ERROR, WARNING, DEBUG, QUIET };

    private LoggerFinderLoader() {
        throw new InternalError("LoggerFinderLoader cannot be instantiated");
    }

    static volatile Thread loadingThread;
    private static System.LoggerFinder service() {
        if (service != null) return service;
        BootstrapLogger.ensureBackendDetected();
        synchronized(lock) {
            if (service != null) return service;
            Thread currentThread = Thread.currentThread();
            if (loadingThread == currentThread) {
                return TemporaryLoggerFinder.INSTANCE;
            }
            loadingThread = currentThread;
            try {
                service = loadLoggerFinder();
            } finally {
                loadingThread = null;
            }
        }
        BootstrapLogger.redirectTemporaryLoggers();
        return service;
    }

    static boolean isLoadingThread() {
        return loadingThread != null && loadingThread == Thread.currentThread();
    }

    private static ErrorPolicy configurationErrorPolicy() {
        String errorPolicy =
                GetPropertyAction.privilegedGetProperty("jdk.logger.finder.error");
        if (errorPolicy == null || errorPolicy.isEmpty()) {
            return ErrorPolicy.WARNING;
        }
        try {
            return ErrorPolicy.valueOf(errorPolicy.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException x) {
            return ErrorPolicy.WARNING;
        }
    }

    private static boolean ensureSingletonProvider() {
        return GetBooleanAction.privilegedGetProperty
            ("jdk.logger.finder.singleton");
    }

    @SuppressWarnings("removal")
    private static Iterator<System.LoggerFinder> findLoggerFinderProviders() {
        final Iterator<System.LoggerFinder> iterator;
        if (System.getSecurityManager() == null) {
            iterator = ServiceLoader.load(System.LoggerFinder.class,
                        ClassLoader.getSystemClassLoader()).iterator();
        } else {
            final PrivilegedAction<Iterator<System.LoggerFinder>> pa =
                    () -> ServiceLoader.load(System.LoggerFinder.class,
                        ClassLoader.getSystemClassLoader()).iterator();
            iterator = AccessController.doPrivileged(pa, null,
                        LOGGERFINDER_PERMISSION, CLASSLOADER_PERMISSION,
                        READ_PERMISSION);
        }
        return iterator;
    }

    public static final class TemporaryLoggerFinder extends LoggerFinder {
        private TemporaryLoggerFinder() {}
        @Stable
        private LoggerFinder loadedService;

        private static final BooleanSupplier isLoadingThread = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return LoggerFinderLoader.isLoadingThread();
            }
        };
        private static final TemporaryLoggerFinder INSTANCE = new TemporaryLoggerFinder();

        @Override
        public Logger getLogger(String name, Module module) {
            if (loadedService == null) {
                loadedService = service;
                if (loadedService == null) {
                    return LazyLoggers.makeLazyLogger(name, module, isLoadingThread);
                }
            }
            assert loadedService != null;
            assert !LoggerFinderLoader.isLoadingThread();
            assert loadedService != this;
            return LazyLoggers.getLogger(name, module);
        }
    }

    private static System.LoggerFinder loadLoggerFinder() {
        System.LoggerFinder result;
        try {
            final Iterator<System.LoggerFinder> iterator =
                    findLoggerFinderProviders();
            if (iterator.hasNext()) {
                result = iterator.next();
                if (iterator.hasNext() && ensureSingletonProvider()) {
                    throw new ServiceConfigurationError(
                            "More than one LoggerFinder implementation");
                }
            } else {
                result = loadDefaultImplementation();
            }
        } catch (Error | RuntimeException x) {
            service = result = new DefaultLoggerFinder();
            ErrorPolicy errorPolicy = configurationErrorPolicy();
            if (errorPolicy == ErrorPolicy.ERROR) {
                if (x instanceof Error) {
                    throw x;
                } else {
                    throw new ServiceConfigurationError(
                        "Failed to instantiate LoggerFinder provider; Using default.", x);
                }
            } else if (errorPolicy != ErrorPolicy.QUIET) {
                SimpleConsoleLogger logger =
                        new SimpleConsoleLogger("jdk.internal.logger", false);
                logger.log(System.Logger.Level.WARNING,
                        "Failed to instantiate LoggerFinder provider; Using default.");
                if (errorPolicy == ErrorPolicy.DEBUG) {
                    logger.log(System.Logger.Level.WARNING,
                        "Exception raised trying to instantiate LoggerFinder", x);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("removal")
    private static System.LoggerFinder loadDefaultImplementation() {
        final SecurityManager sm = System.getSecurityManager();
        final Iterator<DefaultLoggerFinder> iterator;
        if (sm == null) {
            iterator = ServiceLoader.loadInstalled(DefaultLoggerFinder.class).iterator();
        } else {
            PrivilegedAction<Iterator<DefaultLoggerFinder>> pa = () ->
                    ServiceLoader.loadInstalled(DefaultLoggerFinder.class).iterator();
            iterator = AccessController.doPrivileged(pa, null,
                    LOGGERFINDER_PERMISSION, CLASSLOADER_PERMISSION,
                    READ_PERMISSION);
        }
        DefaultLoggerFinder result = null;
        try {
            if (iterator.hasNext()) {
                result = iterator.next();
            }
        } catch (RuntimeException x) {
            throw new ServiceConfigurationError(
                    "Failed to instantiate default LoggerFinder", x);
        }
        if (result == null) {
            result = new DefaultLoggerFinder();
        }
        return result;
    }

    public static System.LoggerFinder getLoggerFinder() {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(LOGGERFINDER_PERMISSION);
        }
        return service();
    }

}
