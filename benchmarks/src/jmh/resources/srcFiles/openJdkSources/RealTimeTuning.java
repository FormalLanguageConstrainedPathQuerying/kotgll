/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 @summary Test RealTime-tunings using SoftReciver.send method
 @modules java.desktop/com.sun.media.sound
*/

import java.io.IOException;

import javax.sound.midi.*;
import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class RealTimeTuning {

    private static class PitchSpy {
        public float pitch = 0;

        public Soundbank getSoundBank() {
            ModelOscillator osc = new ModelOscillator() {
                public float getAttenuation() {
                    return 0;
                }

                public int getChannels() {
                    return 0;
                }

                public ModelOscillatorStream open(float samplerate) {
                    return new ModelOscillatorStream() {
                        public void close() throws IOException {
                            pitch = 0;
                        }

                        public void noteOff(int velocity) {
                            pitch = 0;
                        }

                        public void noteOn(MidiChannel channel,
                                VoiceStatus voice, int noteNumber, int velocity) {
                            pitch = noteNumber * 100;
                        }

                        public int read(float[][] buffer, int offset, int len)
                                throws IOException {
                            return len;
                        }

                        public void setPitch(float ipitch) {
                            pitch = ipitch;
                        }
                    };
                }
            };
            ModelPerformer performer = new ModelPerformer();
            performer.getOscillators().add(osc);
            SimpleInstrument testinstrument = new SimpleInstrument();
            testinstrument.setPatch(new Patch(0, 0));
            testinstrument.add(performer);
            SimpleSoundbank testsoundbank = new SimpleSoundbank();
            testsoundbank.addInstrument(testinstrument);
            return testsoundbank;
        }
    }

    public static void sendTuningChange(Receiver recv, int channel,
            int tuningpreset, int tuningbank) throws InvalidMidiDataException {
        ShortMessage sm1 = new ShortMessage();
        sm1.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x64, 04);
        ShortMessage sm2 = new ShortMessage();
        sm2.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x65, 00);

        ShortMessage sm3 = new ShortMessage();
        sm3.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x06, tuningbank);
        ShortMessage sm4 = new ShortMessage();
        sm4.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x60, 0x7F);
        ShortMessage sm5 = new ShortMessage();
        sm5.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x61, 0x7F);

        ShortMessage sm6 = new ShortMessage();
        sm6.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x64, 03);
        ShortMessage sm7 = new ShortMessage();
        sm7.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x65, 00);

        ShortMessage sm8 = new ShortMessage();
        sm8
                .setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x06,
                        tuningpreset);
        ShortMessage sm9 = new ShortMessage();
        sm9.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x60, 0x7F);
        ShortMessage sm10 = new ShortMessage();
        sm10.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x61, 0x7F);

        recv.send(sm1, -1);
        recv.send(sm2, -1);
        recv.send(sm3, -1);
        recv.send(sm4, -1);
        recv.send(sm5, -1);
        recv.send(sm6, -1);
        recv.send(sm7, -1);
        recv.send(sm8, -1);
        recv.send(sm9, -1);
        recv.send(sm10, -1);

    }

    private static void assertTrue(boolean value) throws Exception {
        if (!value)
            throw new RuntimeException("assertTrue fails!");
    }

    public static void testTunings(int[] msg, int tuningProgram,
            int tuningBank, int targetNote, float targetPitch, boolean realtime)
            throws Exception {
        AudioSynthesizer synth = new SoftSynthesizer();
        AudioInputStream stream = synth.openStream(null, null);
        Receiver recv = synth.getReceiver();
        MidiChannel channel = synth.getChannels()[0];
        byte[] buff = new byte[2048];

        PitchSpy pitchspy = new PitchSpy();

        synth.unloadAllInstruments(synth.getDefaultSoundbank());
        synth.loadAllInstruments(pitchspy.getSoundBank());

        SysexMessage sysex = null;

        if (msg != null) {
            byte[] bmsg = new byte[msg.length];
            for (int i = 0; i < bmsg.length; i++)
                bmsg[i] = (byte) msg[i];
            sysex = new SysexMessage();
            sysex.setMessage(bmsg, bmsg.length);
            if (targetPitch == 0) {
                targetPitch = (float) new SoftTuning(bmsg)
                        .getTuning(targetNote);
                assertTrue(Math.abs(targetPitch - targetNote * 100.0) > 0.001);
            }
        }

        if (tuningProgram != -1)
            sendTuningChange(recv, 0, tuningProgram, tuningBank);

        channel.noteOn(targetNote, 64);
        stream.read(buff, 0, buff.length);
        assertTrue(Math.abs(pitchspy.pitch - (targetNote * 100.0)) < 0.001);

        if (sysex != null)
            recv.send(sysex, -1);
        stream.read(buff, 0, buff.length);
        if (realtime)
            assertTrue(Math.abs(pitchspy.pitch - targetPitch) < 0.001);
        else
            assertTrue(Math.abs(pitchspy.pitch - (targetNote * 100.0)) < 0.001);

        channel.noteOn(targetNote, 0);
        stream.read(buff, 0, buff.length);
        assertTrue(Math.abs(pitchspy.pitch - 0.0) < 0.001);

        channel.noteOn(targetNote, 64);
        stream.read(buff, 0, buff.length);
        assertTrue(Math.abs(pitchspy.pitch - targetPitch) < 0.001);

        channel.noteOn(targetNote, 0);
        stream.read(buff, 0, buff.length);
        assertTrue(Math.abs(pitchspy.pitch - 0.0) < 0.001);

        stream.close();
    }

    public static void main(String[] args) throws Exception {
        testTunings(null, -1, -1, 60, 6000, false);

        int[] msg;
        msg = new int[] { 0xf0, 0x7f, 0x7f, 0x08, 0x02, 0x10, 0x02, 36, 36, 64,
                0, 60, 70, 0, 0, 0xf7 };
        testTunings(msg, 0x10, 0, 60, 7000, true);

        msg = new int[] { 0xf0, 0x7e, 0x7f, 0x08, 0x07, 0x05, 0x07, 0x02, 36,
                36, 64, 0, 60, 80, 0, 0, 0xf7 };
        testTunings(msg, 0x07, 0x05, 60, 8000, false);

        msg = new int[] { 0xf0, 0x7f, 0x7f, 0x08, 0x07, 0x05, 0x07, 0x02, 36,
                36, 64, 0, 60, 80, 0, 0, 0xf7 };
        testTunings(msg, 0x07, 0x05, 60, 8000, true);

        msg = new int[] { 0xf0, 0x7e, 0x7f, 0x08, 0x08, 0x03, 0x7f, 0x7f, 5,
                10, 15, 20, 25, 30, 35, 40, 45, 50, 51, 52, 0xf7 };
        testTunings(msg, -1, -1, 60, 0, false);

        msg = new int[] { 0xf0, 0x7f, 0x7f, 0x08, 0x08, 0x03, 0x7f, 0x7f, 5,
                10, 15, 20, 25, 30, 35, 40, 45, 50, 51, 52, 0xf7 };
        testTunings(msg, -1, -1, 60, 0, true);

        msg = new int[] { 0xf0, 0x7e, 0x7f, 0x08, 0x09, 0x03, 0x7f, 0x7f, 5,
                10, 15, 20, 25, 30, 35, 40, 45, 50, 51, 52, 5, 10, 15, 20, 25,
                30, 35, 40, 45, 50, 51, 52, 0xf7 };
        testTunings(msg, -1, -1, 60, 0, false);

        msg = new int[] { 0xf0, 0x7f, 0x7f, 0x08, 0x09, 0x03, 0x7f, 0x7f, 5,
                10, 15, 20, 25, 30, 35, 40, 45, 50, 51, 52, 5, 10, 15, 20, 25,
                30, 35, 40, 45, 50, 51, 52, 0xf7 };
        testTunings(msg, -1, -1, 60, 0, true);

    }
}
