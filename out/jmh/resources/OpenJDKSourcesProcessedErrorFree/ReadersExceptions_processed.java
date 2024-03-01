/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import static java.util.ServiceLoader.load;

/**
 * @test
 * @bug 7058662 7058666 7058672 8130305
 * @author Sergey Bylokhov
 */
public final class ReadersExceptions {

    static byte[] wrongAIFFCh =
            {0x46, 0x4f, 0x52, 0x4d, 
             0, 0, 0, 0, 
             0, 0, 0, 0, 
             0x43, 0x4f, 0x4d, 0x4d, 
             0, 0, 0, 100, 
             0, 0, 
             0, 0, 0, 0, 
             0, 10  
                    , 0, 0, 0, 0};
    static byte[] wrongAIFFSSL =
            {0x46, 0x4f, 0x52, 0x4d, 
             0, 0, 0, 0, 
             0, 0, 0, 0, 
             0x43, 0x4f, 0x4d, 0x4d, 
             0, 0, 0, 100, 
             0, 10, 
             0, 0, 0, 0, 
             0, 0  
                    , 0, 0, 0, 0};
    static byte[] wrongAIFFSSH =
            {0x46, 0x4f, 0x52, 0x4d, 
             0, 0, 0, 0, 
             0, 0, 0, 0, 
             0x43, 0x4f, 0x4d, 0x4d, 
             0, 0, 0, 100, 
             0, 10, 
             0, 0, 0, 0, 
             0, 33  
                    , 0, 0, 0, 0};
    static byte[] wrongAUCh =
            {0x2e, 0x73, 0x6e, 0x64,
             0, 0, 0, 24, 
             0, 0, 0, 0, 
             0, 0, 0, 1, 
             0, 0, 0, 1, 
             0, 0, 0, 0 
            };
    static byte[] wrongAUSR =
            {0x2e, 0x73, 0x6e, 0x64,
             0, 0, 0, 24, 
             0, 0, 0, 0, 
             0, 0, 0, 1, 
             0, 0, 0, 0, 
             0, 0, 0, 1 
            };
    static byte[] wrongAUEmptyHeader =
            {0x2e, 0x73, 0x6e, 0x64,
             0, 0, 0, 0, 
             0, 0, 0, 0, 
             0, 0, 0, 1, 
             0, 0, 0, 1, 
             0, 0, 0, 1 
            };
    static byte[] wrongAUSmallHeader =
            {0x2e, 0x73, 0x6e, 0x64,
             0, 0, 0, 7, 
             0, 0, 0, 0, 
             0, 0, 0, 1, 
             0, 0, 0, 1, 
             0, 0, 0, 1 
            };
    static byte[] wrongAUFrameSizeOverflowNegativeResult =
            {0x2e, 0x73, 0x6e, 0x64,
             0, 0, 0, 24, 
             0, 0, 0, 0, 
             0, 0, 0, 5, 
             0, 0, 0, 1, 
             0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF 
            };
    static byte[] wrongAUFrameSizeOverflowPositiveResult =
            {0x2e, 0x73, 0x6e, 0x64,
             0, 0, 0, 24, 
             0, 0, 0, 0, 
             0, 0, 0, 4, 
             0, 0, 0, 1, 
             0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF 
            };
    static byte[] wrongWAVCh =
            {0x52, 0x49, 0x46, 0x46, 
             1, 1, 1, 1, 
             0x57, 0x41, 0x56, 0x45, 
             0x66, 0x6d, 0x74, 0x20, 
             3, 0, 0, 0, 
             1, 0, 
             0, 0, 
             0, 0, 0, 0, 
             0, 0, 0, 0, 
             0, 0, 
             1, 0, 
             0x64, 0x61, 0x74, 0x61, 
             0, 0, 0, 0, 
            };
    static byte[] wrongWAVSSB =
            {0x52, 0x49, 0x46, 0x46, 
             1, 1, 1, 1, 
             0x57, 0x41, 0x56, 0x45, 
             0x66, 0x6d, 0x74, 0x20, 
             3, 0, 0, 0, 
             1, 0, 
             1, 0, 
             0, 0, 0, 0, 
             0, 0, 0, 0, 
             0, 0, 
             0, 0, 
             0x64, 0x61, 0x74, 0x61, 
             0, 0, 0, 0, 
            };

    static byte[][] data = {
            wrongAIFFCh, wrongAIFFSSL, wrongAIFFSSH, wrongAUCh, wrongAUSR,
            wrongAUEmptyHeader, wrongAUSmallHeader,
            wrongAUFrameSizeOverflowNegativeResult,
            wrongAUFrameSizeOverflowPositiveResult, wrongWAVCh, wrongWAVSSB
    };

    public static void main(final String[] args) throws IOException {
        for (final byte[] bytes : data) {
            testAS(bytes);
            testAFR(bytes);
        }
    }

    private static void testAS(final byte[] buffer) throws IOException {
        final InputStream is = new ByteArrayInputStream(buffer);
        try {
            AudioSystem.getAudioFileFormat(is);
        } catch (UnsupportedAudioFileException ignored) {
            return;
        }
        throw new RuntimeException("Test Failed");
    }

    private static void testAFR(final byte[] buffer) throws IOException {
        final InputStream is = new ByteArrayInputStream(buffer);
        for (final AudioFileReader afr : load(AudioFileReader.class)) {
            for (int i = 0; i < 10; ++i) {
                try {
                    afr.getAudioFileFormat(is);
                    throw new RuntimeException("UAFE expected");
                } catch (final UnsupportedAudioFileException ignored) {
                }
            }
        }
    }
}
