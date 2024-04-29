/*
 * Copyright (c) 2002, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.dns;

import jdk.internal.misc.InnocuousThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
 * An implementation of sun.net.ResolverConfiguration for Windows.
 */

public final class ResolverConfigurationImpl
    extends ResolverConfiguration
{
    private static final Object lock = new Object();

    private final Options opts;

    private static boolean changed = true;

    private static long lastRefresh;

    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(120);

    private static String os_searchlist;
    private static String os_nameservers;

    private static ArrayList<String> searchlist;
    private static ArrayList<String> nameservers;

    private ArrayList<String> stringToList(String str) {
        String[] tokens = str.split(",");
        ArrayList<String> l = new ArrayList<>(tokens.length);
        for (String s : tokens) {
            if (!s.isEmpty() && !l.contains(s)) {
                l.add(s);
            }
        }
        l.trimToSize();
        return l;
    }

    private ArrayList<String> addressesToList(String str) {
        String[] tokens = str.split(",");
        ArrayList<String> l = new ArrayList<>(tokens.length);

        for (String s : tokens) {
            if (!s.isEmpty()) {
                if (s.indexOf(':') >= 0 && s.charAt(0) != '[') {
                    s = '[' + s + ']';
                }
                if (!s.isEmpty() && !l.contains(s)) {
                    l.add(s);
                }
            }
        }
        l.trimToSize();
        return l;
    }


    private void loadConfig() {
        assert Thread.holdsLock(lock);

        if (changed) {
            changed = false;
        } else {
            long currTime = System.nanoTime();
            if ((currTime - lastRefresh) < TIMEOUT_NANOS) {
                return;
            }
        }

        loadDNSconfig0();

        lastRefresh = System.nanoTime();
        searchlist = stringToList(os_searchlist);
        nameservers = addressesToList(os_nameservers);
        os_searchlist = null;                       
        os_nameservers = null;
    }

    ResolverConfigurationImpl() {
        opts = new OptionsImpl();
    }

    @SuppressWarnings("unchecked") 
    public List<String> searchlist() {
        synchronized (lock) {
            loadConfig();

            return (List<String>)searchlist.clone();
        }
    }

    @SuppressWarnings("unchecked") 
    public List<String> nameservers() {
        synchronized (lock) {
            loadConfig();

            return (List<String>)nameservers.clone();
         }
    }

    public Options options() {
        return opts;
    }


    static class AddressChangeListener implements Runnable {
        @Override
        public void run() {
            for (;;) {
                if (notifyAddrChange0() != 0)
                    return;
                synchronized (lock) {
                    changed = true;
                }
            }
        }
    }



    static native void init0();

    static native void loadDNSconfig0();

    static native int notifyAddrChange0();

    static {
        jdk.internal.loader.BootLoader.loadLibrary("net");
        init0();

        String name = "Jndi-Dns-address-change-listener";
        Thread addrChangeListener = InnocuousThread.newSystemThread(name,
                new AddressChangeListener());
        addrChangeListener.setDaemon(true);
        addrChangeListener.start();
    }
}

/**
 * Implementation of {@link ResolverConfiguration.Options}
 */
class OptionsImpl extends ResolverConfiguration.Options {
}
