/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 4932841
 * @key intermittent sound
 * @summary Sequencer's recording feature does not work
 */
public class Recording {

    public static boolean failed = false;
    public static boolean passed = false;
    private static Sequencer seq = null;

    public static void main(String[] args) throws Exception {
        try {
            seq = MidiSystem.getSequencer();

            Sequence sequence = createSequence(10, 120, 240);

            seq.setSequence(sequence);
            out("Set Sequence to Sequencer. Tempo="+seq.getTempoInBPM());

            Track track = sequence.createTrack();
            int oldSize = track.size();
            seq.recordEnable(track, -1);

            seq.open();

            failed = true;
            Receiver rec = seq.getReceiver();

            seq.startRecording();

            failed = false;

            if (!seq.isRecording()) {
                failed = true;
                throw new Exception("Sequencer did not start recording!");
            }
            if (!seq.isRunning()) {
                failed = true;
                throw new Exception("Sequencer started recording, but is not running!");
            }

            ShortMessage msg = new ShortMessage();
            msg.setMessage(0xC0, 80, 00);
            rec.send(msg, 5l * 1000l * 1000l);

            Thread.sleep(1000);

            msg = new ShortMessage();
            msg.setMessage(0xC0, 81, 00);
            long secondEventTick = seq.getTickPosition();
            rec.send(msg, -1);

            seq.stopRecording();
            if (seq.isRecording()) {
                failed = true;
                throw new Exception("Stopped recording, but Sequencer is still recording!");
            }
            if (!seq.isRunning()) {
                failed = true;
                throw new Exception("Stopped recording, but Sequencer but is not running anymore!");
            }

            seq.stop();
            if (seq.isRunning()) {
                failed = true;
                throw new Exception("Stopped Sequencer, but it is still running!");
            }

            int newSize = track.size();
            int addedEventCount = newSize - oldSize;

            out("Added "+addedEventCount+" events to recording track.");
            if (addedEventCount != 2) {
                failed = true;
                throw new Exception("Did not add 2 events!");
            }

            MidiEvent ev = track.get(0);
            msg = (ShortMessage) ev.getMessage();
            out("The first recorded event is at tick position: "+ev.getTick());
            if (Math.abs(ev.getTick() - secondEventTick) > 1000) {
                out(" -> but expected something like: "+secondEventTick+"! FAILED.");
                failed = true;
            }

            ev = track.get(1);
            msg = (ShortMessage) ev.getMessage();
            out("The 2nd recorded event is at tick position: "+ev.getTick());
            out(" -> sequence's tick length is "+seq.getTickLength());
            if (Math.abs(ev.getTick() - (sequence.getTickLength() / 2)) > 1000) {
                out(" -> but expected something like: "+(seq.getTickLength()/2)+"! FAILED.");
                failed = true;
            }

            passed = true;
        } catch (Exception e) {
            out(e.toString());
            if (!failed) out("Test not failed.");
        }
        if (seq != null) {
            seq.close();
        }

        if (failed) {
            throw new Exception("Test FAILED!");
        }
        else if (passed) {
            out("Test Passed.");
        }
    }

    /**
     * Create a new Sequence for testing.
     */
    private static Sequence createSequence(int lengthInSeconds, int tempoInBPM,
                                           int resolution) {
        Sequence sequence = null;
        long lengthInMicroseconds = lengthInSeconds * 1000000;
        boolean createTempoEvent = true;
        if (tempoInBPM == 0) {
            tempoInBPM = 120;
            createTempoEvent = false;
            System.out.print("Creating sequence: "+lengthInSeconds+"sec, "
                             +"resolution="+resolution+" ticks/beat...");
        } else {
            System.out.print("Creating sequence: "+lengthInSeconds+"sec, "
                             +tempoInBPM+" beats/min, "
                             +"resolution="+resolution+" ticks/beat...");
        }
        long lengthInTicks = (lengthInMicroseconds * tempoInBPM * resolution) / 60000000l;
        try {
            sequence = new Sequence(Sequence.PPQ, resolution);
            Track track = sequence.createTrack();
            if (createTempoEvent) {
                int tempoInMPQ = (int) (60000000l / tempoInBPM);
                MetaMessage tm = new MetaMessage();
                byte[] msg = new byte[3];
                msg[0] = (byte) (tempoInMPQ >> 16);
                msg[1] = (byte) ((tempoInMPQ >> 8) & 0xFF);
                msg[2] = (byte) (tempoInMPQ & 0xFF);

                tm.setMessage(0x51 /* Meta Tempo */, msg, msg.length);
                track.add(new MidiEvent(tm, 0));
            }
            ShortMessage mm = new ShortMessage();
            mm.setMessage(0xF6, 0, 0);
            MidiEvent me = new MidiEvent(mm, lengthInTicks);
            track.add(me);
        } catch (InvalidMidiDataException e) {
            out(e);
        }
        out("OK");

        return sequence;
    }

    private static void out(Throwable t) {
        t.printStackTrace(System.out);
    }

    private static void out(String message) {
        System.out.println(message);
    }
}
