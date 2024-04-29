/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.spi.ToolProvider;
import javacserver.shared.PortFile;
import javacserver.shared.Protocol;
import javacserver.shared.Result;
import javacserver.util.LazyInitFileLog;
import javacserver.util.Log;
import javacserver.util.LoggingOutputStream;
import javacserver.util.Util;

/**
 * Start a new server main thread, that will listen to incoming connection requests from the client,
 * and dispatch these on to worker threads in a thread pool, running javac.
 */
public class Server {
    private ServerSocket serverSocket;
    private PortFile portFile;
    private PortFileMonitor portFileMonitor;
    private IdleMonitor idleMonitor;
    private CompilerThreadPool compilerThreadPool;

    final AtomicBoolean keepAcceptingRequests = new AtomicBoolean();

    private static LazyInitFileLog errorLog;

    public static void main(String... args) {
        initLogging();

        try {
            PortFile portFile = getPortFileFromArguments(args);
            if (portFile == null) {
                System.exit(Result.CMDERR.exitCode);
                return;
            }

            Server server = new Server(portFile);
            if (!server.start()) {
                System.exit(Result.ERROR.exitCode);
            } else {
                System.exit(Result.OK.exitCode);
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            System.exit(Result.ERROR.exitCode);
        }
    }

    private static void initLogging() {
        errorLog = new LazyInitFileLog("server.log");
        Log.setLogForCurrentThread(errorLog);
        Log.setLogLevel(Log.Level.ERROR); 

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            restoreServerErrorLog();
            Log.error(e);
        });

        System.setOut(new PrintStream(new LoggingOutputStream(System.out, Log.Level.INFO, "[stdout] ")));
        System.setErr(new PrintStream(new LoggingOutputStream(System.err, Log.Level.ERROR, "[stderr] ")));
    }

    private static PortFile getPortFileFromArguments(String[] args) {
        if (args.length != 1) {
            Log.error("javacserver daemon incorrectly called");
            return null;
        }
        String portfilename = args[0];
        PortFile portFile = new PortFile(portfilename);
        return portFile;
    }

    public Server(PortFile portFile) throws FileNotFoundException {
        this.portFile = portFile;
    }

    /**
     * Start the daemon, unless another one is already running, in which it returns
     * false and exits immediately.
     */
    private boolean start() throws IOException, InterruptedException {
        portFile.lock();
        portFile.getValues();
        if (portFile.containsPortInfo()) {
            Log.debug("javacserver daemon not started because portfile exists!");
            portFile.unlock();
            return false;
        }

        serverSocket = new ServerSocket();
        InetAddress localhost = InetAddress.getByName(null);
        serverSocket.bind(new InetSocketAddress(localhost, 0));


        long myCookie = new Random().nextLong();
        portFile.setValues(serverSocket.getLocalPort(), myCookie);
        portFile.unlock();

        portFileMonitor = new PortFileMonitor(portFile, this::shutdownServer);
        portFileMonitor.start();
        compilerThreadPool = new CompilerThreadPool();
        idleMonitor = new IdleMonitor(this::shutdownServer);

        Log.debug("javacserver daemon started. Accepting connections...");
        Log.debug("    port: " + serverSocket.getLocalPort());
        Log.debug("    time: " + new java.util.Date());
        Log.debug("    poolsize: " + compilerThreadPool.poolSize());

        keepAcceptingRequests.set(true);
        do {
            try {
                Socket socket = serverSocket.accept();
                compilerThreadPool.execute(() -> handleRequest(socket));
            } catch (SocketException se) {
            }
        } while (keepAcceptingRequests.get());

        Log.debug("Shutting down.");


        idleMonitor.shutdown();
        compilerThreadPool.shutdown();

        return true;
    }

    private void handleRequest(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            try {
                idleMonitor.startCall();

                Log.setLogForCurrentThread(new Protocol.ProtocolLog(out));

                String[] args = Protocol.readCommand(in);

                checkInternalErrorLog();

                int exitCode = runCompiler(args);

                Protocol.sendExitCode(out, exitCode);

                checkInternalErrorLog();
            } finally {
                idleMonitor.endCall();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.error(ex);
        } finally {
            Log.setLogForCurrentThread(null);
        }
    }

    public static int runCompiler(String[] args) {
        StringWriter strWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(strWriter);

        Optional<ToolProvider> tool = ToolProvider.findFirst("javac");
        if (tool.isEmpty()) {
            Log.error("Can't find tool javac");
            return Result.ERROR.exitCode;
        }
        int exitcode = tool.get().run(printWriter, printWriter, args);

        printWriter.flush();
        Util.getLines(strWriter.toString()).forEach(Log::error);

        return exitcode;
    }

    private void checkInternalErrorLog() {
        Path errorLogPath = errorLog.getLogDestination();
        if (errorLogPath != null) {
            Log.error("Server has encountered an internal error. See " + errorLogPath.toAbsolutePath()
                    + " for details.");
        }
    }

    public static void restoreServerErrorLog() {
        Log.setLogForCurrentThread(errorLog);
    }

    public void shutdownServer(String quitMsg) {
        if (!keepAcceptingRequests.compareAndSet(true, false)) {
            return;
        }

        Log.debug("Quitting: " + quitMsg);

        portFileMonitor.shutdown(); 

        try {
            portFile.delete();
        } catch (IOException | InterruptedException e) {
            Log.error(e);
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.error(e);
        }
    }
}
