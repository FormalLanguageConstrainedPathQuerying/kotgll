/*
 * Copyright (c) 1999, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioPermission;

/** Managing security in the Java Sound implementation.
 * This class contains all code that uses and is used by
 * SecurityManager.doPrivileged().
 *
 * @author Matthias Pfisterer
 */
final class JSSecurityManager {

    /** Prevent instantiation.
     */
    private JSSecurityManager() {
    }

    static void checkRecordPermission() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new AudioPermission("record"));
        }
    }

    /**
     * Load properties from a file.
     * <p>
     * This method tries to load properties from the filename give into the
     * passed properties object. If the file cannot be found or something else
     * goes wrong, the method silently fails.
     * <p>
     * If the file referenced in "javax.sound.config.file" property exists and
     * the user has an access to it, then it will be loaded, otherwise default
     * configuration file "JAVA_HOME/conf/sound.properties" will be loaded.
     *
     * @param  properties the properties bundle to store the values of the
     *         properties file
     */
    @SuppressWarnings("removal")
    static void loadProperties(final Properties properties) {
        final String customFile = AccessController.doPrivileged(
                (PrivilegedAction<String>) () -> System.getProperty(
                        "javax.sound.config.file"));
        if (customFile != null) {
            if (loadPropertiesImpl(properties, customFile)) {
                return;
            }
        }
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            final String home = System.getProperty("java.home");
            if (home == null) {
                throw new Error("Can't find java.home ??");
            }
            loadPropertiesImpl(properties, home, "conf", "sound.properties");
            return null;
        });
    }

    private static boolean loadPropertiesImpl(final Properties properties,
                                              String first, String... more) {
        final Path fname = Paths.get(first, more);
        try (final Reader reader = Files.newBufferedReader(fname)) {
            properties.load(reader);
            return true;
        } catch (final Throwable t) {
            return false;
        }
    }

    /** Create a Thread in the current ThreadGroup.
     */
    static Thread createThread(final Runnable runnable,
                               final String threadName,
                               final boolean isDaemon, final int priority,
                               final boolean doStart)
    {
        Thread thread = new Thread(null, runnable, threadName, 0, false);

        thread.setDaemon(isDaemon);
        if (priority >= 0) {
            thread.setPriority(priority);
        }
        if (doStart) {
            thread.start();
        }
        return thread;
    }

    @SuppressWarnings("removal")
    static synchronized <T> List<T> getProviders(final Class<T> providerClass) {
        List<T> p = new ArrayList<>(7);
        final PrivilegedAction<Iterator<T>> psAction =
                new PrivilegedAction<Iterator<T>>() {
                    @Override
                    public Iterator<T> run() {
                        return ServiceLoader.load(providerClass).iterator();
                    }
                };
        final Iterator<T> ps = AccessController.doPrivileged(psAction);

        PrivilegedAction<Boolean> hasNextAction = new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return ps.hasNext();
            }
        };

        while (AccessController.doPrivileged(hasNextAction)) {
            try {
                T provider = ps.next();
                if (providerClass.isInstance(provider)) {
                    p.add(0, provider);
                }
            } catch (Throwable t) {
                if (Printer.err) t.printStackTrace();
            }
        }
        return p;
    }
}
