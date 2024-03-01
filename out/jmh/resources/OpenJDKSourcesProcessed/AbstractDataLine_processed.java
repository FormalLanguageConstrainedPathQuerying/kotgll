/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

/**
 * AbstractDataLine
 *
 * @author Kara Kytle
 */
abstract class AbstractDataLine extends AbstractLine implements DataLine {


    private final AudioFormat defaultFormat;

    private final int defaultBufferSize;

    protected final Object lock = new Object();


    protected AudioFormat format;

    protected int bufferSize;

    private volatile boolean running;
    private volatile boolean started;
    private volatile boolean active;

    /**
     * Constructs a new AbstractLine.
     */
    protected AbstractDataLine(DataLine.Info info, AbstractMixer mixer, Control[] controls) {
        this(info, mixer, controls, null, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Constructs a new AbstractLine.
     */
    protected AbstractDataLine(DataLine.Info info, AbstractMixer mixer, Control[] controls, AudioFormat format, int bufferSize) {

        super(info, mixer, controls);

        if (format != null) {
            defaultFormat = format;
        } else {
            defaultFormat = new AudioFormat(44100.0f, 16, 2, true, Platform.isBigEndian());
        }
        if (bufferSize > 0) {
            defaultBufferSize = bufferSize;
        } else {
            defaultBufferSize = ((int) (defaultFormat.getFrameRate() / 2)) * defaultFormat.getFrameSize();
        }

        this.format = defaultFormat;
        this.bufferSize = defaultBufferSize;
    }



    public final void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        synchronized (mixer) {
            if (!isOpen()) {
                Toolkit.isFullySpecifiedAudioFormat(format);
                mixer.open(this);

                try {
                    implOpen(format, bufferSize);

                    setOpen(true);

                } catch (LineUnavailableException e) {
                    mixer.close(this);
                    throw e;
                }
            } else {
                if (!format.matches(getFormat())) {
                    throw new IllegalStateException("Line is already open with format " + getFormat() +
                                                    " and bufferSize " + getBufferSize());
                }
                if (bufferSize > 0) {
                    setBufferSize(bufferSize);
                }
            }
        }
    }

    public final void open(AudioFormat format) throws LineUnavailableException {
        open(format, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * This implementation always returns 0.
     */
    @Override
    public int available() {
        return 0;
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void drain() {
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void flush() {
    }

    @Override
    public final void start() {
        synchronized(mixer) {

            if (isOpen()) {

                if (!isStartedRunning()) {
                    mixer.start(this);
                    implStart();
                    running = true;
                }
            }
        }

        synchronized(lock) {
            lock.notifyAll();
        }
    }

    @Override
    public final void stop() {

        synchronized(mixer) {
            if (isOpen()) {

                if (isStartedRunning()) {

                    implStop();
                    mixer.stop(this);

                    running = false;

                    if (started && (!isActive())) {
                        setStarted(false);
                    }
                }
            }
        }

        synchronized(lock) {
            lock.notifyAll();
        }
    }


    @Override
    public final boolean isRunning() {
        return started;
    }

    @Override
    public final boolean isActive() {
        return active;
    }

    @Override
    public final long getMicrosecondPosition() {

        long microseconds = getLongFramePosition();
        if (microseconds != AudioSystem.NOT_SPECIFIED) {
            microseconds = Toolkit.frames2micros(getFormat(), microseconds);
        }
        return microseconds;
    }

    @Override
    public final AudioFormat getFormat() {
        return format;
    }

    @Override
    public final int getBufferSize() {
        return bufferSize;
    }

    /**
     * This implementation does NOT change the buffer size
     */
    public final int setBufferSize(int newSize) {
        return getBufferSize();
    }

    /**
     * This implementation returns AudioSystem.NOT_SPECIFIED.
     */
    @Override
    public final float getLevel() {
        return (float)AudioSystem.NOT_SPECIFIED;
    }


    /**
     * running is true after start is called and before stop is called,
     * regardless of whether data is actually being presented.
     */

    final boolean isStartedRunning() {
        return running;
    }

    /**
     * This method sets the active state and generates
     * events if it changes.
     */
    final void setActive(boolean active) {

        this.active = active;

    }

    /**
     * This method sets the started state and generates
     * events if it changes.
     */
    final void setStarted(boolean started) {
        boolean sendEvents = false;
        long position = getLongFramePosition();

        if (this.started != started) {
            this.started = started;
            sendEvents = true;
        }

        if (sendEvents) {

            if (started) {
                sendEvents(new LineEvent(this, LineEvent.Type.START, position));
            } else {
                sendEvents(new LineEvent(this, LineEvent.Type.STOP, position));
            }
        }
    }

    /**
     * This method generates a STOP event and sets the started state to false.
     * It is here for historic reasons when an EOM event existed.
     */
    final void setEOM() {
        setStarted(false);
    }


    /**
     * Try to open the line with the current format and buffer size values.
     * If the line is not open, these will be the defaults.  If the
     * line is open, this should return quietly because the values
     * requested will match the current ones.
     */
    @Override
    public final void open() throws LineUnavailableException {
        open(format, bufferSize);
    }

    /**
     * This should also stop the line.  The closed line should not be running or active.
     * After we close the line, we reset the format and buffer size to the defaults.
     */
    @Override
    public final void close() {
        synchronized (mixer) {
            if (isOpen()) {

                stop();

                setOpen(false);

                implClose();

                mixer.close(this);

                format = defaultFormat;
                bufferSize = defaultBufferSize;
            }
        }
    }

    abstract void implOpen(AudioFormat format, int bufferSize) throws LineUnavailableException;
    abstract void implClose();

    abstract void implStart();
    abstract void implStop();
}
