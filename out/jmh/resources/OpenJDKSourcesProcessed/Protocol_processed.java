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

package javacserver.shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javacserver.util.Log;
import javacserver.util.Util;

/**
 * Implementation of the wire protocol used by the javacserver client and daemon to communicate.
 * Basically, the client sends the argument to javac, one line per string. The server responds
 * with log lines (if there is any output), and the exit code from javac.
 */
public class Protocol {
    private static final String LINE_TYPE_RC = "RC";

    public static void sendCommand(PrintWriter out, String[] args) throws IOException {
        out.println(args.length);
        for (String arg : args)
            out.println(arg);
        out.flush();
    }

    public static String[] readCommand(BufferedReader in) throws IOException {
        int n = Integer.parseInt(in.readLine());
        String[] args = new String[n];
        for (int i = 0; i < n; i++) {
            args[i] = in.readLine();
        }
        return args;
    }

    public static void sendExitCode(PrintWriter out, int exitCode) {
        out.println(LINE_TYPE_RC + ":" + exitCode);
    }

    public static int readResponse(BufferedReader in) throws IOException {
        String line;
        while (null != (line = in.readLine())) {
            Line parsedLine = new Line(line);

            try {
                String content = parsedLine.getContent();
                if (Log.isDebugging()) {
                    content = "[javacserver] " + content;
                }
                Log.log(Log.Level.valueOf(parsedLine.getType()), content);
                continue;
            } catch (IllegalArgumentException e) {
            }

            if (parsedLine.isExitCode()) {
                return parsedLine.getExitCode();
            }
        }
        return Result.ERROR.exitCode;
    }

    public static class Line {
        private final String type;

        public String getType() {
            return type;
        }

        public String getContent() {
            return content;
        }

        public boolean isExitCode() {
            return type.equals(LINE_TYPE_RC);
        }

        public int getExitCode() {
            return Integer.parseInt(content);
        }

        private final String content;

        public Line(String line) {
            if (!line.contains(":")) {
                throw new AssertionError("Could not parse protocol line: >>\"" + line + "\"<<");
            }
            String[] typeAndContent = line.split(":", 2);
            type = typeAndContent[0];
            content = typeAndContent[1];
        }
    }

    public static class ProtocolLog extends Log {
        public ProtocolLog(PrintWriter out) {
            super(out, out);
        }

        @Override
        protected boolean isLevelLogged(Level l) {
            return true;
        }

        @Override
        protected void printLogMsg(Level msgLevel, String msg) {
            Util.getLines(msg)
                .map(line -> msgLevel + ":" + line)
                .forEach(line -> super.printLogMsg(msgLevel, line));
        }
    }
}
