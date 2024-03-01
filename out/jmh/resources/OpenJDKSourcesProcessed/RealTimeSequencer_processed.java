/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

/**
 * A Real Time Sequencer
 *
 * @author Florian Bomers
 */

/* TODO:
 * - rename PlayThread to PlayEngine (because isn't a thread)
 */
final class RealTimeSequencer extends AbstractMidiDevice
        implements Sequencer, AutoConnectSequencer {

    /**
     * Event Dispatcher thread. Should be using a shared event
     * dispatcher instance with a factory in EventDispatcher
     */
    private static final Map<ThreadGroup, EventDispatcher> dispatchers =
            new WeakHashMap<>();

    /**
     * All RealTimeSequencers share this info object.
     */
    static final MidiDevice.Info info = new RealTimeSequencerInfo();


    private static final Sequencer.SyncMode[] masterSyncModes = { Sequencer.SyncMode.INTERNAL_CLOCK };
    private static final Sequencer.SyncMode[] slaveSyncModes  = { Sequencer.SyncMode.NO_SYNC };

    private static final Sequencer.SyncMode masterSyncMode    = Sequencer.SyncMode.INTERNAL_CLOCK;
    private static final Sequencer.SyncMode slaveSyncMode     = Sequencer.SyncMode.NO_SYNC;

    /**
     * Sequence on which this sequencer is operating.
     */
    private Sequence sequence = null;


    /**
     * Same for setTempoInMPQ...
     * -1 means not set.
     */
    private double cacheTempoMPQ = -1;

    /**
     * cache value for tempo factor until sequence is set
     * -1 means not set.
     */
    private float cacheTempoFactor = -1;

    /** if a particular track is muted */
    private boolean[] trackMuted = null;
    /** if a particular track is solo */
    private boolean[] trackSolo = null;

    /** tempo cache for getMicrosecondPosition */
    private final MidiUtils.TempoCache tempoCache = new MidiUtils.TempoCache();

    /**
     * True if the sequence is running.
     */
    private volatile boolean running;

    /**
     * the thread for pushing out the MIDI messages.
     */
    private PlayThread playThread;

    /**
     * True if we are recording.
     */
    private volatile boolean recording;

    /**
     * List of tracks to which we're recording.
     */
    private final List<RecordingTrack> recordingTracks = new ArrayList<>();

    private long loopStart = 0;
    private long loopEnd = -1;
    private int loopCount = 0;

    /**
     * Meta event listeners.
     */
    private final ArrayList<Object> metaEventListeners = new ArrayList<>();

    /**
     * Control change listeners.
     */
    private final ArrayList<ControllerListElement> controllerEventListeners = new ArrayList<>();

    /**
     * automatic connection support.
     */
    private boolean autoConnect = false;

    /**
     * if we need to autoconnect at next open.
     */
    private boolean doAutoConnectAtNextOpen = false;

    /**
     * the receiver that this device is auto-connected to.
     */
    Receiver autoConnectedReceiver = null;


    /* ****************************** CONSTRUCTOR ****************************** */

    RealTimeSequencer(){
        super(info);
    }

    /* ****************************** SEQUENCER METHODS ******************** */

    @Override
    public synchronized void setSequence(Sequence sequence)
        throws InvalidMidiDataException {
        if (sequence != this.sequence) {
            if (this.sequence != null && sequence == null) {
                setCaches();
                stop();
                trackMuted = null;
                trackSolo = null;
                loopStart = 0;
                loopEnd = -1;
                loopCount = 0;
                if (getDataPump() != null) {
                    getDataPump().setTickPos(0);
                    getDataPump().resetLoopCount();
                }
            }

            if (playThread != null) {
                playThread.setSequence(sequence);
            }

            this.sequence = sequence;

            if (sequence != null) {
                tempoCache.refresh(sequence);
                setTickPosition(0);
                propagateCaches();
            }
        }
        else if (sequence != null) {
            tempoCache.refresh(sequence);
            if (playThread != null) {
                playThread.setSequence(sequence);
            }
        }
    }

    @Override
    public synchronized void setSequence(InputStream stream) throws IOException, InvalidMidiDataException {
        if (stream == null) {
            setSequence((Sequence) null);
            return;
        }

        Sequence seq = MidiSystem.getSequence(stream); 

        setSequence(seq);
    }

    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public synchronized void start() {
        if (!isOpen()) {
            throw new IllegalStateException("sequencer not open");
        }

        if (sequence == null) {
            throw new IllegalStateException("sequence not set");
        }

        if (running == true) {
            return;
        }

        implStart();
    }

    @Override
    public synchronized void stop() {
        if (!isOpen()) {
            throw new IllegalStateException("sequencer not open");
        }
        stopRecording();

        if (running == false) {
            return;
        }

        implStop();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void startRecording() {
        if (!isOpen()) {
            throw new IllegalStateException("Sequencer not open");
        }

        start();
        recording = true;
    }

    @Override
    public void stopRecording() {
        if (!isOpen()) {
            throw new IllegalStateException("Sequencer not open");
        }
        recording = false;
    }

    @Override
    public boolean isRecording() {
        return recording;
    }

    @Override
    public void recordEnable(Track track, int channel) {
        if (!findTrack(track)) {
            throw new IllegalArgumentException("Track does not exist in the current sequence");
        }

        synchronized(recordingTracks) {
            RecordingTrack rc = RecordingTrack.get(recordingTracks, track);
            if (rc != null) {
                rc.channel = channel;
            } else {
                recordingTracks.add(new RecordingTrack(track, channel));
            }
        }

    }

    @Override
    public void recordDisable(Track track) {
        synchronized(recordingTracks) {
            RecordingTrack rc = RecordingTrack.get(recordingTracks, track);
            if (rc != null) {
                recordingTracks.remove(rc);
            }
        }

    }

    private boolean findTrack(Track track) {
        boolean found = false;
        if (sequence != null) {
            Track[] tracks = sequence.getTracks();
            for (int i = 0; i < tracks.length; i++) {
                if (track == tracks[i]) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    @Override
    public float getTempoInBPM() {
        return (float) MidiUtils.convertTempo(getTempoInMPQ());
    }

    @Override
    public void setTempoInBPM(float bpm) {
        if (bpm <= 0) {
            bpm = 1.0f;
        }

        setTempoInMPQ((float) MidiUtils.convertTempo((double) bpm));
    }

    @Override
    public float getTempoInMPQ() {
        if (needCaching()) {
            if (cacheTempoMPQ != -1) {
                return (float) cacheTempoMPQ;
            }
            if (sequence != null) {
                return tempoCache.getTempoMPQAt(getTickPosition());
            }

            return (float) MidiUtils.DEFAULT_TEMPO_MPQ;
        }
        return getDataPump().getTempoMPQ();
    }

    @Override
    public void setTempoInMPQ(float mpq) {
        if (mpq <= 0) {
            mpq = 1.0f;
        }
        if (needCaching()) {
            cacheTempoMPQ = mpq;
        } else {
            getDataPump().setTempoMPQ(mpq);

            cacheTempoMPQ = -1;
        }
    }

    @Override
    public void setTempoFactor(float factor) {
        if (factor <= 0) {
            return;
        }
        if (needCaching()) {
            cacheTempoFactor = factor;
        } else {
            getDataPump().setTempoFactor(factor);
            cacheTempoFactor = -1;
        }
    }

    @Override
    public float getTempoFactor() {
        if (needCaching()) {
            if (cacheTempoFactor != -1) {
                return cacheTempoFactor;
            }
            return 1.0f;
        }
        return getDataPump().getTempoFactor();
    }

    @Override
    public long getTickLength() {
        if (sequence == null) {
            return 0;
        }

        return sequence.getTickLength();
    }

    @Override
    public synchronized long getTickPosition() {
        if (getDataPump() == null || sequence == null) {
            return 0;
        }

        return getDataPump().getTickPos();
    }

    @Override
    public synchronized void setTickPosition(long tick) {
        if (tick < 0) {
            return;
        }
        if (getDataPump() == null) {
            if (tick != 0) {
            }
        }
        else if (sequence == null) {
            if (tick != 0) {
            }
        } else {
            getDataPump().setTickPos(tick);
        }
    }

    @Override
    public long getMicrosecondLength() {
        if (sequence == null) {
            return 0;
        }

        return sequence.getMicrosecondLength();
    }

    @Override
    public long getMicrosecondPosition() {
        if (getDataPump() == null || sequence == null) {
            return 0;
        }
        synchronized (tempoCache) {
            return MidiUtils.tick2microsecond(sequence, getDataPump().getTickPos(), tempoCache);
        }
    }

    @Override
    public void setMicrosecondPosition(long microseconds) {
        if (microseconds < 0) {
            return;
        }
        if (getDataPump() == null) {
            if (microseconds != 0) {
            }
        }
        else if (sequence == null) {
            if (microseconds != 0) {
            }
        } else {
            synchronized(tempoCache) {
                setTickPosition(MidiUtils.microsecond2tick(sequence, microseconds, tempoCache));
            }
        }
    }

    @Override
    public void setMasterSyncMode(Sequencer.SyncMode sync) {
    }

    @Override
    public Sequencer.SyncMode getMasterSyncMode() {
        return masterSyncMode;
    }

    @Override
    public Sequencer.SyncMode[] getMasterSyncModes() {
        Sequencer.SyncMode[] returnedModes = new Sequencer.SyncMode[masterSyncModes.length];
        System.arraycopy(masterSyncModes, 0, returnedModes, 0, masterSyncModes.length);
        return returnedModes;
    }

    @Override
    public void setSlaveSyncMode(Sequencer.SyncMode sync) {
    }

    @Override
    public Sequencer.SyncMode getSlaveSyncMode() {
        return slaveSyncMode;
    }

    @Override
    public Sequencer.SyncMode[] getSlaveSyncModes() {
        Sequencer.SyncMode[] returnedModes = new Sequencer.SyncMode[slaveSyncModes.length];
        System.arraycopy(slaveSyncModes, 0, returnedModes, 0, slaveSyncModes.length);
        return returnedModes;
    }

    int getTrackCount() {
        Sequence seq = getSequence();
        if (seq != null) {
            return sequence.getTracks().length;
        }
        return 0;
    }

    @Override
    public synchronized void setTrackMute(int track, boolean mute) {
        int trackCount = getTrackCount();
        if (track < 0 || track >= getTrackCount()) return;
        trackMuted = ensureBoolArraySize(trackMuted, trackCount);
        trackMuted[track] = mute;
        if (getDataPump() != null) {
            getDataPump().muteSoloChanged();
        }
    }

    @Override
    public synchronized boolean getTrackMute(int track) {
        if (track < 0 || track >= getTrackCount()) return false;
        if (trackMuted == null || trackMuted.length <= track) return false;
        return trackMuted[track];
    }

    @Override
    public synchronized void setTrackSolo(int track, boolean solo) {
        int trackCount = getTrackCount();
        if (track < 0 || track >= getTrackCount()) return;
        trackSolo = ensureBoolArraySize(trackSolo, trackCount);
        trackSolo[track] = solo;
        if (getDataPump() != null) {
            getDataPump().muteSoloChanged();
        }
    }

    @Override
    public synchronized boolean getTrackSolo(int track) {
        if (track < 0 || track >= getTrackCount()) return false;
        if (trackSolo == null || trackSolo.length <= track) return false;
        return trackSolo[track];
    }

    @Override
    public boolean addMetaEventListener(MetaEventListener listener) {
        synchronized(metaEventListeners) {
            if (! metaEventListeners.contains(listener)) {

                metaEventListeners.add(listener);
            }
            return true;
        }
    }

    @Override
    public void removeMetaEventListener(MetaEventListener listener) {
        synchronized(metaEventListeners) {
            int index = metaEventListeners.indexOf(listener);
            if (index >= 0) {
                metaEventListeners.remove(index);
            }
        }
    }

    @Override
    public int[] addControllerEventListener(ControllerEventListener listener, int[] controllers) {
        synchronized(controllerEventListeners) {

            ControllerListElement cve = null;
            boolean flag = false;
            for(int i=0; i < controllerEventListeners.size(); i++) {

                cve = controllerEventListeners.get(i);

                if (cve.listener.equals(listener)) {
                    cve.addControllers(controllers);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                cve = new ControllerListElement(listener, controllers);
                controllerEventListeners.add(cve);
            }

            return cve.getControllers();
        }
    }

    @Override
    public int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers) {
        synchronized(controllerEventListeners) {
            ControllerListElement cve = null;
            boolean flag = false;
            for (int i=0; i < controllerEventListeners.size(); i++) {
                cve = controllerEventListeners.get(i);
                if (cve.listener.equals(listener)) {
                    cve.removeControllers(controllers);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                return new int[0];
            }
            if (controllers == null) {
                int index = controllerEventListeners.indexOf(cve);
                if (index >= 0) {
                    controllerEventListeners.remove(index);
                }
                return new int[0];
            }
            return cve.getControllers();
        }
    }


    @Override
    public void setLoopStartPoint(long tick) {
        if ((tick > getTickLength())
            || ((loopEnd != -1) && (tick > loopEnd))
            || (tick < 0)) {
            throw new IllegalArgumentException("invalid loop start point: "+tick);
        }
        loopStart = tick;
    }

    @Override
    public long getLoopStartPoint() {
        return loopStart;
    }

    @Override
    public void setLoopEndPoint(long tick) {
        if ((tick > getTickLength())
            || ((loopStart > tick) && (tick != -1))
            || (tick < -1)) {
            throw new IllegalArgumentException("invalid loop end point: "+tick);
        }
        loopEnd = tick;
    }

    @Override
    public long getLoopEndPoint() {
        return loopEnd;
    }

    @Override
    public void setLoopCount(int count) {
        if (count != LOOP_CONTINUOUSLY
            && count < 0) {
            throw new IllegalArgumentException("illegal value for loop count: "+count);
        }
        loopCount = count;
        if (getDataPump() != null) {
            getDataPump().resetLoopCount();
        }
    }

    @Override
    public int getLoopCount() {
        return loopCount;
    }

    /* *********************************** play control ************************* */

    @Override
    protected void implOpen() throws MidiUnavailableException {

        playThread = new PlayThread();

        if (sequence != null) {
            playThread.setSequence(sequence);
        }

        propagateCaches();

        if (doAutoConnectAtNextOpen) {
            doAutoConnect();
        }
    }

    private void doAutoConnect() {
        Receiver rec = null;
        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            if (synth instanceof ReferenceCountingDevice) {
                rec = ((ReferenceCountingDevice) synth).getReceiverReferenceCounting();
            } else {
                synth.open();
                try {
                    rec = synth.getReceiver();
                } finally {
                    if (rec == null) {
                        synth.close();
                    }
                }
            }
        } catch (Exception e) {
        }
        if (rec == null) {
            try {
                rec = MidiSystem.getReceiver();
            } catch (Exception e) {
            }
        }
        if (rec != null) {
            autoConnectedReceiver = rec;
            try {
                getTransmitter().setReceiver(rec);
            } catch (Exception e) {}
        }
    }

    private synchronized void propagateCaches() {
        if (sequence != null && isOpen()) {
            if (cacheTempoFactor != -1) {
                setTempoFactor(cacheTempoFactor);
            }
            if (cacheTempoMPQ == -1) {
                setTempoInMPQ((new MidiUtils.TempoCache(sequence)).getTempoMPQAt(getTickPosition()));
            } else {
                setTempoInMPQ((float) cacheTempoMPQ);
            }
        }
    }

    /**
     * populate the caches with the current values.
     */
    private synchronized void setCaches() {
        cacheTempoFactor = getTempoFactor();
        cacheTempoMPQ = getTempoInMPQ();
    }

    @Override
    protected synchronized void implClose() {
        if (playThread == null) {
            if (Printer.err) Printer.err("RealTimeSequencer.implClose() called, but playThread not instantiated!");
        } else {
            playThread.close();
            playThread = null;
        }

        super.implClose();

        sequence = null;
        running = false;
        cacheTempoMPQ = -1;
        cacheTempoFactor = -1;
        trackMuted = null;
        trackSolo = null;
        loopStart = 0;
        loopEnd = -1;
        loopCount = 0;

        /** if this sequencer is set to autoconnect, need to
         * re-establish the connection at next open!
         */
        doAutoConnectAtNextOpen = autoConnect;

        if (autoConnectedReceiver != null) {
            try {
                autoConnectedReceiver.close();
            } catch (Exception e) {}
            autoConnectedReceiver = null;
        }
    }

    void implStart() {
        if (playThread == null) {
            if (Printer.err) Printer.err("RealTimeSequencer.implStart() called, but playThread not instantiated!");
            return;
        }

        tempoCache.refresh(sequence);
        if (!running) {
            running  = true;
            playThread.start();
        }
    }

    void implStop() {
        if (playThread == null) {
            if (Printer.err) Printer.err("RealTimeSequencer.implStop() called, but playThread not instantiated!");
            return;
        }

        recording = false;
        if (running) {
            running = false;
            playThread.stop();
        }
    }

    private static EventDispatcher getEventDispatcher() {
        final ThreadGroup tg = Thread.currentThread().getThreadGroup();
        synchronized (dispatchers) {
            EventDispatcher eventDispatcher = dispatchers.get(tg);
            if (eventDispatcher == null) {
                eventDispatcher = new EventDispatcher();
                dispatchers.put(tg, eventDispatcher);
                eventDispatcher.start();
            }
            return eventDispatcher;
        }
    }

    /**
     * Send midi player events.
     * must not be synchronized on "this"
     */
    void sendMetaEvents(MidiMessage message) {
        if (metaEventListeners.size() == 0) return;

        getEventDispatcher().sendAudioEvents(message, metaEventListeners);
    }

    /**
     * Send midi player events.
     */
    void sendControllerEvents(MidiMessage message) {
        int size = controllerEventListeners.size();
        if (size == 0) return;

        if (! (message instanceof ShortMessage)) {
            return;
        }
        ShortMessage msg = (ShortMessage) message;
        int controller = msg.getData1();
        List<Object> sendToListeners = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ControllerListElement cve = controllerEventListeners.get(i);
            for(int j = 0; j < cve.controllers.length; j++) {
                if (cve.controllers[j] == controller) {
                    sendToListeners.add(cve.listener);
                    break;
                }
            }
        }
        getEventDispatcher().sendAudioEvents(message, sendToListeners);
    }

    private boolean needCaching() {
        return !isOpen() || (sequence == null) || (playThread == null);
    }

    /**
     * return the data pump instance, owned by play thread
     * if playthread is null, return null.
     * This method is guaranteed to return non-null if
     * needCaching returns false
     */
    private DataPump getDataPump() {
        if (playThread != null) {
            return playThread.getDataPump();
        }
        return null;
    }

    private MidiUtils.TempoCache getTempoCache() {
        return tempoCache;
    }

    private static boolean[] ensureBoolArraySize(boolean[] array, int desiredSize) {
        if (array == null) {
            return new boolean[desiredSize];
        }
        if (array.length < desiredSize) {
            boolean[] newArray = new boolean[desiredSize];
            System.arraycopy(array, 0, newArray, 0, array.length);
            return newArray;
        }
        return array;
    }


    @Override
    protected boolean hasReceivers() {
        return true;
    }

    @Override
    protected Receiver createReceiver() throws MidiUnavailableException {
        return new SequencerReceiver();
    }

    @Override
    protected boolean hasTransmitters() {
        return true;
    }

    @Override
    protected Transmitter createTransmitter() throws MidiUnavailableException {
        return new SequencerTransmitter();
    }

    @Override
    public void setAutoConnect(Receiver autoConnectedReceiver) {
        this.autoConnect = (autoConnectedReceiver != null);
        this.autoConnectedReceiver = autoConnectedReceiver;
    }

    /**
     * An own class to distinguish the class name from
     * the transmitter of other devices.
     */
    private class SequencerTransmitter extends BasicTransmitter {
        private SequencerTransmitter() {
            super();
        }
    }

    final class SequencerReceiver extends AbstractReceiver {

        @Override
        void implSend(MidiMessage message, long timeStamp) {
            if (recording) {
                long tickPos = 0;

                if (timeStamp < 0) {
                    tickPos = getTickPosition();
                } else {
                    synchronized(tempoCache) {
                        tickPos = MidiUtils.microsecond2tick(sequence, timeStamp, tempoCache);
                    }
                }

                Track track = null;
                if (message.getLength() > 1) {
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if ((sm.getStatus() & 0xF0) != 0xF0) {
                            track = RecordingTrack.get(recordingTracks, sm.getChannel());
                        }
                    } else {
                        track = RecordingTrack.get(recordingTracks, -1);
                    }
                    if (track != null) {
                        if (message instanceof ShortMessage) {
                            message = new FastShortMessage((ShortMessage) message);
                        } else {
                            message = (MidiMessage) message.clone();
                        }

                        MidiEvent me = new MidiEvent(message, tickPos);
                        track.add(me);
                    }
                }
            }
        }
    }

    private static class RealTimeSequencerInfo extends MidiDevice.Info {

        private static final String name = "Real Time Sequencer";
        private static final String vendor = "Oracle Corporation";
        private static final String description = "Software sequencer";
        private static final String version = "Version 1.0";

        RealTimeSequencerInfo() {
            super(name, vendor, description, version);
        }
    } 

    private static class ControllerListElement {

        int []  controllers;
        final ControllerEventListener listener;

        private ControllerListElement(ControllerEventListener listener, int[] controllers) {

            this.listener = listener;
            if (controllers == null) {
                controllers = new int[128];
                for (int i = 0; i < 128; i++) {
                    controllers[i] = i;
                }
            }
            this.controllers = controllers;
        }

        private void addControllers(int[] c) {

            if (c==null) {
                controllers = new int[128];
                for (int i = 0; i < 128; i++) {
                    controllers[i] = i;
                }
                return;
            }
            int[] temp = new int[ controllers.length + c.length ];
            int elements;

            for(int i=0; i<controllers.length; i++) {
                temp[i] = controllers[i];
            }
            elements = controllers.length;
            for(int i=0; i<c.length; i++) {
                boolean flag = false;

                for(int j=0; j<controllers.length; j++) {
                    if (c[i] == controllers[j]) {
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    temp[elements++] = c[i];
                }
            }
            int[] newc = new int[ elements ];
            for(int i=0; i<elements; i++){
                newc[i] = temp[i];
            }
            controllers = newc;
        }

        private void removeControllers(int[] c) {

            if (c==null) {
                controllers = new int[0];
            } else {
                int[] temp = new int[ controllers.length ];
                int elements = 0;


                for(int i=0; i<controllers.length; i++){
                    boolean flag = false;
                    for(int j=0; j<c.length; j++) {
                        if (controllers[i] == c[j]) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag){
                        temp[elements++] = controllers[i];
                    }
                }
                int[] newc = new int[ elements ];
                for(int i=0; i<elements; i++) {
                    newc[i] = temp[i];
                }
                controllers = newc;

            }
        }

        private int[] getControllers() {

            if (controllers == null) {
                return null;
            }

            int[] c = new int[controllers.length];

            for(int i=0; i<controllers.length; i++){
                c[i] = controllers[i];
            }
            return c;
        }

    } 

    static class RecordingTrack {

        private final Track track;
        private int channel;

        RecordingTrack(Track track, int channel) {
            this.track = track;
            this.channel = channel;
        }

        static RecordingTrack get(List<RecordingTrack> recordingTracks, Track track) {

            synchronized(recordingTracks) {
                int size = recordingTracks.size();

                for (int i = 0; i < size; i++) {
                    RecordingTrack current = recordingTracks.get(i);
                    if (current.track == track) {
                        return current;
                    }
                }
            }
            return null;
        }

        static Track get(List<RecordingTrack> recordingTracks, int channel) {

            synchronized(recordingTracks) {
                int size = recordingTracks.size();
                for (int i = 0; i < size; i++) {
                    RecordingTrack current = recordingTracks.get(i);
                    if ((current.channel == channel) || (current.channel == -1)) {
                        return current.track;
                    }
                }
            }
            return null;

        }
    }

    final class PlayThread implements Runnable {
        private Thread thread;
        private final Object lock = new Object();

        /** true if playback is interrupted (in close) */
        boolean interrupted = false;
        boolean isPumping = false;

        private final DataPump dataPump = new DataPump();


        PlayThread() {
            int priority = Thread.NORM_PRIORITY
                + ((Thread.MAX_PRIORITY - Thread.NORM_PRIORITY) * 3) / 4;
            thread = JSSecurityManager.createThread(this,
                                                    "Java Sound Sequencer", 
                                                    false,                  
                                                    priority,               
                                                    true);                  
        }

        DataPump getDataPump() {
            return dataPump;
        }

        synchronized void setSequence(Sequence seq) {
            dataPump.setSequence(seq);
        }


        /** start thread and pump. Requires up-to-date tempoCache */
        synchronized void start() {
            running = true;

            if (!dataPump.hasCachedTempo()) {
                long tickPos = getTickPosition();
                dataPump.setTempoMPQ(tempoCache.getTempoMPQAt(tickPos));
            }
            dataPump.checkPointMillis = 0; 
            dataPump.clearNoteOnCache();
            dataPump.needReindex = true;

            dataPump.resetLoopCount();

            synchronized(lock) {
                lock.notifyAll();
            }
        }

        synchronized void stop() {
            playThreadImplStop();
            long t = System.nanoTime() / 1000000l;
            while (isPumping) {
                synchronized(lock) {
                    try {
                        lock.wait(2000);
                    } catch (InterruptedException ie) {
                    }
                }
                if ((System.nanoTime()/1000000l) - t > 1900) {
                    if (Printer.err) Printer.err("Waited more than 2 seconds in RealTimeSequencer.PlayThread.stop()!");
                }
            }
        }

        void playThreadImplStop() {
            running = false;
            synchronized(lock) {
                lock.notifyAll();
            }
        }

        void close() {
            Thread oldThread = null;
            synchronized (this) {
                interrupted = true;
                oldThread = thread;
                thread = null;
            }
            if (oldThread != null) {
                synchronized(lock) {
                    lock.notifyAll();
                }
            }
            if (oldThread != null) {
                try {
                    oldThread.join(2000);
                } catch (InterruptedException ie) {}
            }
        }

        /**
         * Main process loop driving the media flow.
         *
         * Make sure to NOT synchronize on RealTimeSequencer
         * anywhere here (even implicit). That is a sure deadlock!
         */
        @Override
        public void run() {

            while (!interrupted) {
                boolean EOM = false;
                boolean wasRunning = running;
                isPumping = !interrupted && running;
                while (!EOM && !interrupted && running) {
                    EOM = dataPump.pump();

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ie) {
                    }
                }

                playThreadImplStop();
                if (wasRunning) {
                    dataPump.notesOff(true);
                }
                if (EOM) {
                    dataPump.setTickPos(sequence.getTickLength());

                    MetaMessage message = new MetaMessage();
                    try{
                        message.setMessage(MidiUtils.META_END_OF_TRACK_TYPE, new byte[0], 0);
                    } catch(InvalidMidiDataException e1) {}
                    sendMetaEvents(message);
                }
                synchronized (lock) {
                    isPumping = false;
                    lock.notifyAll();
                    while (!running && !interrupted) {
                        try {
                            lock.wait();
                        } catch (Exception ex) {}
                    }
                }
            } 
        }
    }

    /**
     * class that does the actual dispatching of events,
     * used to be in native in MMAPI.
     */
    private class DataPump {
        private float currTempo;         
        private float tempoFactor;       
        private float inverseTempoFactor;
        private long ignoreTempoEventAt; 
        private int resolution;
        private float divisionType;
        private long checkPointMillis;   
        private long checkPointTick;     
        private int[] noteOnCache;       
        private Track[] tracks;
        private boolean[] trackDisabled; 
        private int[] trackReadPos;      
        private long lastTick;
        private boolean needReindex = false;
        private int currLoopCounter = 0;


        DataPump() {
            init();
        }

        synchronized void init() {
            ignoreTempoEventAt = -1;
            tempoFactor = 1.0f;
            inverseTempoFactor = 1.0f;
            noteOnCache = new int[128];
            tracks = null;
            trackDisabled = null;
        }

        synchronized void setTickPos(long tickPos) {
            long oldLastTick = tickPos;
            lastTick = tickPos;
            if (running) {
                notesOff(false);
            }
            if (running || tickPos > 0) {
                chaseEvents(oldLastTick, tickPos);
            } else {
                needReindex = true;
            }
            if (!hasCachedTempo()) {
                setTempoMPQ(getTempoCache().getTempoMPQAt(lastTick, currTempo));
                ignoreTempoEventAt = -1;
            }
            checkPointMillis = 0;
        }

        long getTickPos() {
            return lastTick;
        }

        boolean hasCachedTempo() {
            if (ignoreTempoEventAt != lastTick) {
                ignoreTempoEventAt = -1;
            }
            return ignoreTempoEventAt >= 0;
        }

        synchronized void setTempoMPQ(float tempoMPQ) {
            if (tempoMPQ > 0 && tempoMPQ != currTempo) {
                ignoreTempoEventAt = lastTick;
                this.currTempo = tempoMPQ;
                checkPointMillis = 0;
            }
        }

        float getTempoMPQ() {
            return currTempo;
        }

        synchronized void setTempoFactor(float factor) {
            if (factor > 0 && factor != this.tempoFactor) {
                tempoFactor = factor;
                inverseTempoFactor = 1.0f / factor;
                checkPointMillis = 0;
            }
        }

        float getTempoFactor() {
            return tempoFactor;
        }

        synchronized void muteSoloChanged() {
            boolean[] newDisabled = makeDisabledArray();
            if (running) {
                applyDisabledTracks(trackDisabled, newDisabled);
            }
            trackDisabled = newDisabled;
        }

        synchronized void setSequence(Sequence seq) {
            if (seq == null) {
                init();
                return;
            }
            tracks = seq.getTracks();
            muteSoloChanged();
            resolution = seq.getResolution();
            divisionType = seq.getDivisionType();
            trackReadPos = new int[tracks.length];
            checkPointMillis = 0;
            needReindex = true;
        }

        synchronized void resetLoopCount() {
            currLoopCounter = loopCount;
        }

        void clearNoteOnCache() {
            for (int i = 0; i < 128; i++) {
                noteOnCache[i] = 0;
            }
        }

        void notesOff(boolean doControllers) {
            int done = 0;
            for (int ch=0; ch<16; ch++) {
                int channelMask = (1<<ch);
                for (int i=0; i<128; i++) {
                    if ((noteOnCache[i] & channelMask) != 0) {
                        noteOnCache[i] ^= channelMask;
                        getTransmitterList().sendMessage((ShortMessage.NOTE_ON | ch) | (i<<8), -1);
                        done++;
                    }
                }
                /* all notes off */
                getTransmitterList().sendMessage((ShortMessage.CONTROL_CHANGE | ch) | (123<<8), -1);
                /* sustain off */
                getTransmitterList().sendMessage((ShortMessage.CONTROL_CHANGE | ch) | (64<<8), -1);
                if (doControllers) {
                    /* reset all controllers */
                    getTransmitterList().sendMessage((ShortMessage.CONTROL_CHANGE | ch) | (121<<8), -1);
                    done++;
                }
            }
        }

        private boolean[] makeDisabledArray() {
            if (tracks == null) {
                return null;
            }
            boolean[] newTrackDisabled = new boolean[tracks.length];
            boolean[] solo;
            boolean[] mute;
            synchronized(RealTimeSequencer.this) {
                mute = trackMuted;
                solo = trackSolo;
            }
            boolean hasSolo = false;
            if (solo != null) {
                for (int i = 0; i < solo.length; i++) {
                    if (solo[i]) {
                        hasSolo = true;
                        break;
                    }
                }
            }
            if (hasSolo) {
                for (int i = 0; i < newTrackDisabled.length; i++) {
                    newTrackDisabled[i] = (i >= solo.length) || (!solo[i]);
                }
            } else {
                for (int i = 0; i < newTrackDisabled.length; i++) {
                    newTrackDisabled[i] = (mute != null) && (i < mute.length) && (mute[i]);
                }
            }
            return newTrackDisabled;
        }

        /**
         * chase all events from beginning of Track
         * and send note off for those events that are active
         * in noteOnCache array.
         * It is possible, of course, to catch notes from other tracks,
         * but better than more complicated logic to detect
         * which notes are really from this track
         */
        private void sendNoteOffIfOn(Track track, long endTick) {
            int size = track.size();
            int done = 0;
            try {
                for (int i = 0; i < size; i++) {
                    MidiEvent event = track.get(i);
                    if (event.getTick() > endTick) break;
                    MidiMessage msg = event.getMessage();
                    int status = msg.getStatus();
                    int len = msg.getLength();
                    if (len == 3 && ((status & 0xF0) == ShortMessage.NOTE_ON)) {
                        int note = -1;
                        if (msg instanceof ShortMessage) {
                            ShortMessage smsg = (ShortMessage) msg;
                            if (smsg.getData2() > 0) {
                                note = smsg.getData1();
                            }
                        } else {
                            byte[] data = msg.getMessage();
                            if ((data[2] & 0x7F) > 0) {
                                note = data[1] & 0x7F;
                            }
                        }
                        if (note >= 0) {
                            int bit = 1<<(status & 0x0F);
                            if ((noteOnCache[note] & bit) != 0) {
                                getTransmitterList().sendMessage(status | (note<<8), -1);
                                noteOnCache[note] &= (0xFFFF ^ bit);
                                done++;
                            }
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException aioobe) {
            }
        }

        /**
         * Runtime application of mute/solo:
         * if a track is muted that was previously playing, send
         *    note off events for all currently playing notes.
         */
        private void applyDisabledTracks(boolean[] oldDisabled, boolean[] newDisabled) {
            byte[][] tempArray = null;
            synchronized(RealTimeSequencer.this) {
                for (int i = 0; i < newDisabled.length; i++) {
                    if (((oldDisabled == null)
                         || (i >= oldDisabled.length)
                         || !oldDisabled[i])
                        && newDisabled[i]) {

                        if (tracks.length > i) {
                            sendNoteOffIfOn(tracks[i], lastTick);
                        }
                    }
                    else if ((oldDisabled != null)
                             && (i < oldDisabled.length)
                             && oldDisabled[i]
                             && !newDisabled[i]) {
                        if (tempArray == null) {
                            tempArray = new byte[128][16];
                        }
                        chaseTrackEvents(i, 0, lastTick, true, tempArray);
                    }
                }
            }
        }

        /** go through all events from startTick to endTick
         * chase the controller state and program change state
         * and then set the end-states at once.
         *
         * needs to be called in synchronized state
         * @param tempArray an byte[128][16] to hold controller messages
         */
        private void chaseTrackEvents(int trackNum,
                                      long startTick,
                                      long endTick,
                                      boolean doReindex,
                                      byte[][] tempArray) {
            if (startTick > endTick) {
                startTick = 0;
            }
            byte[] progs = new byte[16];
            for (int ch = 0; ch < 16; ch++) {
                progs[ch] = -1;
                for (int co = 0; co < 128; co++) {
                    tempArray[co][ch] = -1;
                }
            }
            Track track = tracks[trackNum];
            int size = track.size();
            try {
                for (int i = 0; i < size; i++) {
                    MidiEvent event = track.get(i);
                    if (event.getTick() >= endTick) {
                        if (doReindex && (trackNum < trackReadPos.length)) {
                            trackReadPos[trackNum] = (i > 0)?(i-1):0;
                        }
                        break;
                    }
                    MidiMessage msg = event.getMessage();
                    int status = msg.getStatus();
                    int len = msg.getLength();
                    if (len == 3 && ((status & 0xF0) == ShortMessage.CONTROL_CHANGE)) {
                        if (msg instanceof ShortMessage) {
                            ShortMessage smsg = (ShortMessage) msg;
                            tempArray[smsg.getData1() & 0x7F][status & 0x0F] = (byte) smsg.getData2();
                        } else {
                            byte[] data = msg.getMessage();
                            tempArray[data[1] & 0x7F][status & 0x0F] = data[2];
                        }
                    }
                    if (len == 2 && ((status & 0xF0) == ShortMessage.PROGRAM_CHANGE)) {
                        if (msg instanceof ShortMessage) {
                            ShortMessage smsg = (ShortMessage) msg;
                            progs[status & 0x0F] = (byte) smsg.getData1();
                        } else {
                            byte[] data = msg.getMessage();
                            progs[status & 0x0F] = data[1];
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException aioobe) {
            }
            int numControllersSent = 0;
            for (int ch = 0; ch < 16; ch++) {
                for (int co = 0; co < 128; co++) {
                    byte controllerValue = tempArray[co][ch];
                    if (controllerValue >= 0) {
                        int packedMsg = (ShortMessage.CONTROL_CHANGE | ch) | (co<<8) | (controllerValue<<16);
                        getTransmitterList().sendMessage(packedMsg, -1);
                        numControllersSent++;
                    }
                }
                if (progs[ch] >= 0) {
                    getTransmitterList().sendMessage((ShortMessage.PROGRAM_CHANGE | ch) | (progs[ch]<<8), -1);
                }
                if (progs[ch] >= 0 || startTick == 0 || endTick == 0) {
                    getTransmitterList().sendMessage((ShortMessage.PITCH_BEND | ch) | (0x40 << 16), -1);
                    getTransmitterList().sendMessage((ShortMessage.CONTROL_CHANGE | ch) | (64 << 8), -1);
                }
            }
        }

        /**
         * chase controllers and program for all tracks.
         */
        synchronized void chaseEvents(long startTick, long endTick) {
            byte[][] tempArray = new byte[128][16];
            for (int t = 0; t < tracks.length; t++) {
                if ((trackDisabled == null)
                    || (trackDisabled.length <= t)
                    || (!trackDisabled[t])) {
                    chaseTrackEvents(t, startTick, endTick, true, tempArray);
                }
            }
        }


        private long getCurrentTimeMillis() {
            return System.nanoTime() / 1000000l;
        }

        private long millis2tick(long millis) {
            if (divisionType != Sequence.PPQ) {
                double dTick = ((((double) millis) * tempoFactor)
                                * ((double) divisionType)
                                * ((double) resolution))
                    / ((double) 1000);
                return (long) dTick;
            }
            return MidiUtils.microsec2ticks(millis * 1000,
                                            currTempo * inverseTempoFactor,
                                            resolution);
        }

        private long tick2millis(long tick) {
            if (divisionType != Sequence.PPQ) {
                double dMillis = ((((double) tick) * 1000) /
                                  (tempoFactor * ((double) divisionType) * ((double) resolution)));
                return (long) dMillis;
            }
            return MidiUtils.ticks2microsec(tick,
                                            currTempo * inverseTempoFactor,
                                            resolution) / 1000;
        }

        private void ReindexTrack(int trackNum, long tick) {
            if (trackNum < trackReadPos.length && trackNum < tracks.length) {
                trackReadPos[trackNum] = MidiUtils.tick2index(tracks[trackNum], tick);
            }
        }

        /* returns if changes are pending */
        private boolean dispatchMessage(int trackNum, MidiEvent event) {
            boolean changesPending = false;
            MidiMessage message = event.getMessage();
            int msgStatus = message.getStatus();
            int msgLen = message.getLength();
            if (msgStatus == MetaMessage.META && msgLen >= 2) {

                if (trackNum == 0) {
                    int newTempo = MidiUtils.getTempoMPQ(message);
                    if (newTempo > 0) {
                        if (event.getTick() != ignoreTempoEventAt) {
                            setTempoMPQ(newTempo); 
                            changesPending = true;
                        }
                        ignoreTempoEventAt = -1;
                    }
                }
                sendMetaEvents(message);

            } else {
                getTransmitterList().sendMessage(message, -1);

                switch (msgStatus & 0xF0) {
                case ShortMessage.NOTE_OFF: {
                    int note = ((ShortMessage) message).getData1() & 0x7F;
                    noteOnCache[note] &= (0xFFFF ^ (1<<(msgStatus & 0x0F)));
                    break;
                }

                case ShortMessage.NOTE_ON: {
                    ShortMessage smsg = (ShortMessage) message;
                    int note = smsg.getData1() & 0x7F;
                    int vel = smsg.getData2() & 0x7F;
                    if (vel > 0) {
                        noteOnCache[note] |= 1<<(msgStatus & 0x0F);
                    } else {
                        noteOnCache[note] &= (0xFFFF ^ (1<<(msgStatus & 0x0F)));
                    }
                    break;
                }

                case ShortMessage.CONTROL_CHANGE:
                    sendControllerEvents(message);
                    break;

                }
            }
            return changesPending;
        }

        /** the main pump method
         * @return true if end of sequence is reached
         */
        synchronized boolean pump() {
            long currMillis;
            long targetTick = lastTick;
            MidiEvent currEvent;
            boolean changesPending = false;
            boolean doLoop = false;
            boolean EOM = false;

            currMillis = getCurrentTimeMillis();
            int finishedTracks = 0;
            do {
                changesPending = false;

                if (needReindex) {
                    if (trackReadPos.length < tracks.length) {
                        trackReadPos = new int[tracks.length];
                    }
                    for (int t = 0; t < tracks.length; t++) {
                        ReindexTrack(t, targetTick);
                    }
                    needReindex = false;
                    checkPointMillis = 0;
                }

                if (checkPointMillis == 0) {
                    currMillis = getCurrentTimeMillis();
                    checkPointMillis = currMillis;
                    targetTick = lastTick;
                    checkPointTick = targetTick;
                } else {
                    targetTick = checkPointTick + millis2tick(currMillis - checkPointMillis);
                    if ((loopEnd != -1)
                        && ((loopCount > 0 && currLoopCounter > 0)
                            || (loopCount == LOOP_CONTINUOUSLY))) {
                        if (lastTick <= loopEnd && targetTick >= loopEnd) {
                            targetTick = loopEnd - 1;
                            doLoop = true;
                        }
                    }
                    lastTick = targetTick;
                }

                finishedTracks = 0;

                for (int t = 0; t < tracks.length; t++) {
                    try {
                        boolean disabled = trackDisabled[t];
                        Track thisTrack = tracks[t];
                        int readPos = trackReadPos[t];
                        int size = thisTrack.size();
                        while (!changesPending && (readPos < size)
                               && (currEvent = thisTrack.get(readPos)).getTick() <= targetTick) {

                            if ((readPos == size -1) &&  MidiUtils.isMetaEndOfTrack(currEvent.getMessage())) {
                                readPos = size;
                                break;
                            }
                            readPos++;
                            if (!disabled ||
                                ((t == 0) && (MidiUtils.isMetaTempo(currEvent.getMessage())))) {
                                changesPending = dispatchMessage(t, currEvent);
                            }
                        }
                        if (readPos >= size) {
                            finishedTracks++;
                        }
                        trackReadPos[t] = readPos;
                    } catch(Exception e) {
                        if (Printer.err) e.printStackTrace();
                        if (e instanceof ArrayIndexOutOfBoundsException) {
                            needReindex = true;
                            changesPending = true;
                        }
                    }
                    if (changesPending) {
                        break;
                    }
                }
                EOM = (finishedTracks == tracks.length);
                if (doLoop
                    || ( ((loopCount > 0 && currLoopCounter > 0)
                          || (loopCount == LOOP_CONTINUOUSLY))
                         && !changesPending
                         && (loopEnd == -1)
                         && EOM)) {

                    long oldCheckPointMillis = checkPointMillis;
                    long loopEndTick = loopEnd;
                    if (loopEndTick == -1) {
                        loopEndTick = lastTick;
                    }

                    if (loopCount != LOOP_CONTINUOUSLY) {
                        currLoopCounter--;
                    }
                    setTickPos(loopStart);


                    checkPointMillis = oldCheckPointMillis + tick2millis(loopEndTick - checkPointTick);
                    checkPointTick = loopStart;
                    needReindex = false;
                    changesPending = false;
                    doLoop = false;
                    EOM = false;
                }
            } while (changesPending);

            return EOM;
        }
    } 
}
