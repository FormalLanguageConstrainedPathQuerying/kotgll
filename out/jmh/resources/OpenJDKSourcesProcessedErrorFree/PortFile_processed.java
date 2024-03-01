/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javacserver.shared;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.util.concurrent.Semaphore;
import javacserver.util.Log;

/**
 * The PortFile class mediates access to a short binary file containing the tcp/ip port (for the localhost)
 * and a cookie necessary for the server answering on that port. The file can be locked using file system
 * primitives to avoid race conditions when several javac clients are started at the same. Note that file
 * system locking is not always supported on all operating systems and/or file systems.
 */
public class PortFile {
    private static final int magicNr = 0x1174;

    private String filename;
    private File file;
    private File stopFile;
    private RandomAccessFile rwfile;
    private FileChannel channel;

    private FileLock lock;
    private Semaphore lockSem = new Semaphore(1);

    private boolean containsPortInfo;
    private int serverPort;
    private long serverCookie;
    private int myServerPort;
    private long myServerCookie;

    /**
     * Create a new portfile.
     * @param fn is the path to the file.
     */
    public PortFile(String fn) {
        filename = fn;
        file = new File(filename);
        stopFile = new File(filename+".stop");
        containsPortInfo = false;
        lock = null;
    }

    private void initializeChannel() throws PortFileInaccessibleException {
        try {
            rwfile = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            throw new PortFileInaccessibleException(e);
        }
        channel = rwfile.getChannel();
    }

    /**
     * Lock the port file.
     */
    public void lock() throws IOException, InterruptedException {
        if (channel == null) {
            initializeChannel();
        }
        lockSem.acquire();
        lock = channel.lock();
    }

    /**
     * Read the values from the port file in the file system.
     * Expects the port file to be locked.
     */
    public void getValues()  {
        containsPortInfo = false;
        if (lock == null) {
            return;
        }
        try {
            if (rwfile.length()>0) {
                rwfile.seek(0);
                int nr = rwfile.readInt();
                serverPort = rwfile.readInt();
                serverCookie = rwfile.readLong();

                if (nr == magicNr) {
                    containsPortInfo = true;
                } else {
                    containsPortInfo = false;
                }
            }
        } catch (IOException e) {
            containsPortInfo = false;
        }
    }

    /**
     * Did the locking and getValues succeed?
     */
    public boolean containsPortInfo() {
        return containsPortInfo;
    }

    /**
     * If so, then we can acquire the tcp/ip port on localhost.
     */
    public int getPort() {
        return serverPort;
    }

    /**
     * If so, then we can acquire the server cookie.
     */
    public long getCookie() {
        return serverCookie;
    }

    /**
     * Store the values into the locked port file.
     */
    public void setValues(int port, long cookie) throws IOException {
        rwfile.seek(0);
        rwfile.writeInt(magicNr);
        rwfile.writeInt(port);
        rwfile.writeLong(cookie);
        myServerPort = port;
        myServerCookie = cookie;
    }

    /**
     * Delete the port file.
     */
    public void delete() throws IOException, InterruptedException {
        rwfile.close();

        file.delete();

        for (int i = 0; i < 10 && file.exists(); i++) {
            Thread.sleep(1000);
        }
        if (file.exists()) {
            throw new IOException("Failed to delete file.");
        }
    }

    /**
     * Is the port file still there?
     */
    public boolean exists() throws IOException {
        return file.exists();
    }

    /**
     * Is a stop file there?
     */
    public boolean markedForStop() throws IOException {
        if (stopFile.exists()) {
            try {
                stopFile.delete();
            } catch (Exception e) {
            }
            return true;
        }
        return false;
    }

    /**
     * Unlock the port file.
     */
    public void unlock() throws IOException {
        if (lock == null) {
            return;
        }
        lock.release();
        lock = null;
        lockSem.release();
    }

    public boolean hasValidValues() throws IOException, InterruptedException {
        if (exists()) {
            lock();
            getValues();
            unlock();

            if (containsPortInfo()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wait for the port file to contain values that look valid.
     */
    public void waitForValidValues() throws IOException, InterruptedException {
        final int MS_BETWEEN_ATTEMPTS = 500;
        long startTime = System.currentTimeMillis();
        long timeout = startTime + getServerStartupTimeoutSeconds() * 1000;
        while (true) {
            Log.debug("Looking for valid port file values...");
            if (exists()) {
                lock();
                getValues();
                unlock();
            }
            if (containsPortInfo) {
                Log.debug("Valid port file values found after " + (System.currentTimeMillis() - startTime) + " ms");
                return;
            }
            if (System.currentTimeMillis() > timeout) {
                break;
            }
            Thread.sleep(MS_BETWEEN_ATTEMPTS);
        }
        throw new IOException("No port file values materialized. Giving up after " +
                                      (System.currentTimeMillis() - startTime) + " ms");
    }

    /**
     * Check if the portfile still contains my values, assuming that I am the server.
     */
    public boolean stillMyValues() throws IOException, FileNotFoundException, InterruptedException {
        for (;;) {
            try {
                lock();
                getValues();
                unlock();
                if (containsPortInfo) {
                    if (serverPort == myServerPort &&
                        serverCookie == myServerCookie) {
                        return true;
                    }
                    return false;
                }
                return false;
            } catch (FileLockInterruptionException e) {
                continue;
            }
            catch (ClosedChannelException e) {
                return false;
            }
        }
    }

    /**
     * Return the name of the port file.
     */
    public String getFilename() {
        return filename;
    }

    private long getServerStartupTimeoutSeconds() {
        String str = System.getProperty("serverStartupTimeout");
        if (str != null) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
            }
        }
        return 60;
    }
}
