/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import static javax.sound.midi.SysexMessage.SYSTEM_EXCLUSIVE;

/**
 * @test
 * @bug 8221445
 * @summary Checks exceptions thrown by javax.sound.midi.SysexMessage class
 */
public final class Exceptions {

    public static void main(final String[] args) throws Exception {
        testInvalidMidiDataException();
        testIndexOutOfBoundsException();
        testNullPointerException();
    }

    private static void testInvalidMidiDataException() {
        try {
            new SysexMessage(new byte[0], 0);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final InvalidMidiDataException ignored) {
        }
        try {
            new SysexMessage(new byte[]{(byte) (SYSTEM_EXCLUSIVE)}, 0);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final InvalidMidiDataException ignored) {
        }
        try {
            new SysexMessage(0, new byte[0], 2);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final InvalidMidiDataException ignored) {
        }
        SysexMessage sysexMessage = new SysexMessage();
        try {
            sysexMessage.setMessage(new byte[0], 0);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final InvalidMidiDataException ignored) {
        }
        try {
            sysexMessage.setMessage(new byte[]{(byte) (SYSTEM_EXCLUSIVE)}, 0);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final InvalidMidiDataException ignored) {
        }
        try {
            sysexMessage.setMessage(new byte[]{0}, 0);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final InvalidMidiDataException ignored) {
        }
        try {
            sysexMessage.setMessage(0, new byte[0], 0);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final InvalidMidiDataException ignored) {
        }
    }

    private static void testIndexOutOfBoundsException() throws Exception {
        try {
            new SysexMessage(new byte[]{(byte) (0xF0 & 0xFF)}, 2);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final IndexOutOfBoundsException ignored) {
        }
        try {
            new SysexMessage(0xF0, new byte[0], 2);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final IndexOutOfBoundsException ignored) {
        }
        SysexMessage sysexMessage = new SysexMessage();
        try {
            sysexMessage.setMessage(new byte[]{(byte) (0xF0 & 0xFF)}, 2);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final IndexOutOfBoundsException ignored) {
        }
        try {
            sysexMessage.setMessage(0xF0, new byte[0], 2);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final IndexOutOfBoundsException ignored) {
        }

        try {
            new SysexMessage(new byte[]{(byte) (0xF0 & 0xFF)}, -1);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final IndexOutOfBoundsException ignored) {
        }
        try {
            new SysexMessage(0xF0, new byte[0], -1);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final IndexOutOfBoundsException ignored) {
        }
        sysexMessage = new SysexMessage();
        try {
            sysexMessage.setMessage(new byte[]{(byte) (0xF0 & 0xFF)}, -1);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final IndexOutOfBoundsException ignored) {
        }
        try {
            sysexMessage.setMessage(0xF0, new byte[0], -1);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final IndexOutOfBoundsException ignored) {
        }
    }

    private static void testNullPointerException() throws Exception {
        try {
            new SysexMessage(null, 0);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final NullPointerException ignored) {
        }
        try {
            new SysexMessage(SYSTEM_EXCLUSIVE, null, 2);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final NullPointerException ignored) {
        }
        SysexMessage sysexMessage = new SysexMessage();
        try {
            sysexMessage.setMessage(null, 0);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final NullPointerException ignored) {
        }
        sysexMessage = new SysexMessage();
        try {
            sysexMessage.setMessage(SYSTEM_EXCLUSIVE, null, 2);
            throw new RuntimeException("Expected exception is not thrown");
        } catch (final NullPointerException ignored) {
        }
    }
}
