/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javacserver.server;

import java.io.IOException;
import java.util.Timer;
import java.util.function.Consumer;
import javacserver.shared.PortFile;
import javacserver.util.Log;
import javacserver.util.RunnableTimerTask;

/**
 * Monitors the presence of a port file and shuts down the server
 * whenever the port file is deleted or invalidated.
 *
 * TODO: JDK-8046882
 */
public class PortFileMonitor {
    private static final int CHECK_PORTFILE_INTERVAL = 5000;

    private final Timer timer = new Timer();
    private final PortFile portFile;
    private final Consumer<String> onShutdown;

    public PortFileMonitor(PortFile portFile,
                           Consumer<String> onShutdown) {
        this.portFile = portFile;
        this.onShutdown = onShutdown;
    }

    public void start() {
        Log log = Log.get();
        timer.schedule(new RunnableTimerTask(() -> checkPortFile(log)), 0, CHECK_PORTFILE_INTERVAL);
    }

    private void checkPortFile(Log log) {
        Log.setLogForCurrentThread(log);
        Log.debug("Checking port file status...");
        try {
            if (!portFile.exists()) {
                onShutdown.accept("Quitting because portfile was deleted!");
            } else if (portFile.markedForStop()) {
                onShutdown.accept("Quitting because a portfile.stop file was found!");
            } else if (!portFile.stillMyValues()) {
                onShutdown.accept("Quitting because portfile is now owned by another javac server!");
            }
        } catch (IOException e) {
            Log.error("IOException caught in PortFileMonitor.");
            Log.debug(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.error(e);
        }
    }

    public void shutdown() {
        timer.cancel();
    }
}
