/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.management;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.security.AccessControlContext;

import jdk.jfr.Configuration;
import jdk.jfr.EventSettings;
import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.internal.JVMSupport;
import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.MetadataRepository;
import jdk.jfr.internal.PlatformRecording;
import jdk.jfr.internal.PrivateAccess;
import jdk.jfr.internal.SecuritySupport;
import jdk.jfr.internal.SecuritySupport.SafePath;
import jdk.jfr.internal.util.Utils;
import jdk.jfr.internal.util.ValueFormatter;
import jdk.jfr.internal.util.ValueParser;
import jdk.jfr.internal.WriteableUserPath;
import jdk.jfr.internal.consumer.AbstractEventStream;
import jdk.jfr.internal.consumer.EventDirectoryStream;
import jdk.jfr.internal.consumer.FileAccess;
import jdk.jfr.internal.instrument.JDKEvents;

/**
 * The management API in module jdk.management.jfr should be built on top of the
 * public API in jdk.jfr. Before putting more functionality here, consider if it
 * should not be part of the public API, and if not, please provide motivation
 *
 */
public final class ManagementSupport {

    public static List<EventType> getEventTypes() {
        SecuritySupport.checkAccessFlightRecorder();
        if (JVMSupport.isNotAvailable()) {
            return List.of();
        }
        JDKEvents.initialize(); 
        return Collections.unmodifiableList(MetadataRepository.getInstance().getRegisteredEventTypes());
    }

    public static long parseTimespan(String s) {
        return ValueParser.parseTimespan(s);
    }

    public static Instant epochNanosToInstant(long epochNanos) {
      return Utils.epochNanosToInstant(epochNanos);
    }

    public static final String formatTimespan(Duration dValue, String separation) {
        return ValueFormatter.formatTimespan(dValue, separation);
    }

    public static void logError(String message) {
        Logger.log(LogTag.JFR, LogLevel.ERROR, message);
    }

    public static void logDebug(String message) {
        Logger.log(LogTag.JFR, LogLevel.DEBUG, message);
    }

    public static String getDestinationOriginalText(Recording recording) {
        PlatformRecording pr = PrivateAccess.getInstance().getPlatformRecording(recording);
        WriteableUserPath wup = pr.getDestination();
        return wup == null ? null : wup.getOriginalText();
    }

    public static void checkSetDestination(Recording recording, String destination) throws IOException{
        PlatformRecording pr = PrivateAccess.getInstance().getPlatformRecording(recording);
        if(destination != null){
            WriteableUserPath wup = new WriteableUserPath(Paths.get(destination));
            pr.checkSetDestination(wup);
        }
    }

    public static EventSettings newEventSettings(EventSettingsModifier esm) {
        return PrivateAccess.getInstance().newEventSettings(esm);
    }

    public static void removePath(Recording recording, Path path) {
        PlatformRecording pr = PrivateAccess.getInstance().getPlatformRecording(recording);
        pr.removePath(new SafePath(path));
    }

    public static void setOnChunkCompleteHandler(EventStream stream, Consumer<Long> consumer) {
        EventDirectoryStream eds = (EventDirectoryStream) stream;
        eds.setChunkCompleteHandler(consumer);
    }

    public static long getStartTimeNanos(Recording recording) {
        PlatformRecording pr = PrivateAccess.getInstance().getPlatformRecording(recording);
        return pr.getStartNanos();
    }

    public static Configuration newConfiguration(String name, String label, String description, String provider,
          Map<String, String> settings, String contents) {
        return PrivateAccess.getInstance().newConfiguration(name, label, description, provider, settings, contents);
    }

    public static EventStream newEventDirectoryStream(
            @SuppressWarnings("removal")
            AccessControlContext acc,
            Path directory,
            List<Configuration> confs) throws IOException {
        return new EventDirectoryStream(
            acc,
            directory,
            FileAccess.UNPRIVILEGED,
            null,
            confs,
            false
        );
    }

    public static void setCloseOnComplete(EventStream stream, boolean closeOnComplete) {
        AbstractEventStream aes = (AbstractEventStream) stream;
        aes.setCloseOnComplete(closeOnComplete);
    }

    public static StreamBarrier activateStreamBarrier(EventStream stream) {
        EventDirectoryStream aes = (EventDirectoryStream) stream;
        return aes.activateStreamBarrier();
    }
}
