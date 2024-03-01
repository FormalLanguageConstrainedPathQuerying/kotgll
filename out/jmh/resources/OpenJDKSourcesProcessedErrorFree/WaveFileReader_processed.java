/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * WAVE file reader.
 *
 * @author Kara Kytle
 * @author Jan Borgersen
 * @author Florian Bomers
 */
public final class WaveFileReader extends SunFileReader {

    @Override
    StandardFileFormat getAudioFileFormatImpl(final InputStream stream)
            throws UnsupportedAudioFileException, IOException {


        int nread = 0;
        int fmt;
        int length = 0;
        int wav_type = 0;
        short channels;
        long sampleRate;
        long avgBytesPerSec;
        short blockAlign;
        int sampleSizeInBits;
        AudioFormat.Encoding encoding = null;

        DataInputStream dis = new DataInputStream( stream );

        int magic = dis.readInt();
        long /* unsigned int */ fileLength = rllong(dis) & 0xffffffffL;
        int waveMagic = dis.readInt();
        long totallength;
        if (fileLength <= 0) {
            fileLength = AudioSystem.NOT_SPECIFIED;
            totallength = AudioSystem.NOT_SPECIFIED;
        } else {
            totallength = fileLength + 8;
        }

        if ((magic != WaveFileFormat.RIFF_MAGIC) || (waveMagic != WaveFileFormat.WAVE_MAGIC)) {
            throw new UnsupportedAudioFileException("not a WAVE file");
        }

        while(true) {

            try {
                fmt = dis.readInt();
                nread += 4;
                if( fmt==WaveFileFormat.FMT_MAGIC ) {
                    break;
                } else {
                    length = rllong(dis);
                    nread += 4;
                    if (length % 2 > 0) length++;
                    nread += dis.skipBytes(length);
                }
            } catch (EOFException eof) {
                throw new UnsupportedAudioFileException("Not a valid WAV file");
            }
        }

        length = rllong(dis);
        nread += 4;

        int endLength = nread + length;


        wav_type = rlshort(dis); nread += 2;

        if (wav_type == WaveFileFormat.WAVE_FORMAT_PCM)
            encoding = AudioFormat.Encoding.PCM_SIGNED;  
        else if ( wav_type == WaveFileFormat.WAVE_FORMAT_ALAW )
            encoding = AudioFormat.Encoding.ALAW;
        else if ( wav_type == WaveFileFormat.WAVE_FORMAT_MULAW )
            encoding = AudioFormat.Encoding.ULAW;
        else {
            throw new UnsupportedAudioFileException("Not a supported WAV file");
        }
        channels = rlshort(dis); nread += 2;
        if (channels <= 0) {
            throw new UnsupportedAudioFileException("Invalid number of channels");
        }

        sampleRate = rllong(dis); nread += 4;

        avgBytesPerSec = rllong(dis); nread += 4;

        blockAlign = rlshort(dis); nread += 2;

        sampleSizeInBits = (int)rlshort(dis); nread += 2;
        if (sampleSizeInBits <= 0) {
            throw new UnsupportedAudioFileException("Invalid bitsPerSample");
        }

        if ((sampleSizeInBits==8) && encoding.equals(AudioFormat.Encoding.PCM_SIGNED))
            encoding = AudioFormat.Encoding.PCM_UNSIGNED;



        if (length % 2 != 0) length += 1;

        if (endLength > nread)
            nread += dis.skipBytes(endLength - nread);

        nread = 0;
        while(true) {
            try{
                int datahdr = dis.readInt();
                nread+=4;
                if (datahdr == WaveFileFormat.DATA_MAGIC) {
                    break;
                } else {
                    int thisLength = rllong(dis); nread += 4;
                    if (thisLength % 2 > 0) thisLength++;
                    nread += dis.skipBytes(thisLength);
                }
            } catch (EOFException eof) {
                throw new UnsupportedAudioFileException("Not a valid WAV file");
            }
        }
        long /* unsigned int */ dataLength = rllong(dis) & 0xffffffffL; nread += 4;

        final int frameSize = calculatePCMFrameSize(sampleSizeInBits, channels);
        AudioFormat format = new AudioFormat(encoding,
                                             (float)sampleRate,
                                             sampleSizeInBits, channels,
                                             frameSize,
                                             (float)sampleRate, false);

        long frameLength = dataLength / format.getFrameSize();
        return new WaveFileFormat(AudioFileFormat.Type.WAVE, totallength,
                                  format, frameLength);
    }
}
