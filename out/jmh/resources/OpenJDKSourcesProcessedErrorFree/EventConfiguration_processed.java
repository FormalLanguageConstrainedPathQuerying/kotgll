/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.event;

import jdk.jfr.EventType;
import jdk.jfr.internal.EventControl;
import jdk.jfr.internal.JVM;
import jdk.jfr.internal.PlatformEventType;
import jdk.jfr.internal.PrivateAccess;
import jdk.jfr.SettingControl;

public final class EventConfiguration {
    private final PlatformEventType platformEventType;
    private final EventType eventType;
    private final EventControl eventControl;
    private final SettingControl[] settings;

    private EventConfiguration(EventType eventType, EventControl eventControl) {
        this.eventType = eventType;
        this.platformEventType = PrivateAccess.getInstance().getPlatformEventType(eventType);
        this.eventControl = eventControl;
        this.settings = eventControl.getSettingControls().toArray(new SettingControl[0]);
    }

    public PlatformEventType getPlatformEventType() {
        return platformEventType;
    }

    public EventControl getEventControl() {
        return eventControl;
    }

    public boolean shouldCommit(long duration) {
        return isEnabled() && duration >= platformEventType.getThresholdTicks();
    }

    public SettingControl getSetting(int index) {
        return settings[index];
    }

    public boolean isEnabled() {
        return platformEventType.isCommittable();
    }

    public EventType getEventType() {
        return eventType;
    }

    public static long timestamp() {
        return JVM.counterTime();
    }

    public static long duration(long startTime) {
        if (startTime == 0) {
            return 0;
        }
        return timestamp() - startTime;
    }

    public boolean isRegistered() {
        return platformEventType.isRegistered();
    }

    public long getId() {
        return eventType.getId();
    }
}
