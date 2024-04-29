/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.BadPaddingException;
import sun.security.ssl.SSLCipher.SSLReadCipher;

/**
 * {@code InputRecord} takes care of the management of SSL/TLS/DTLS input
 * records, including buffering, decryption, handshake messages marshal, etc.
 *
 * @author David Brownell
 */
abstract class InputRecord implements Record, Closeable {
    SSLReadCipher       readCipher;
    TransportContext    tc;

    final HandshakeHash handshakeHash;
    volatile boolean    isClosed;

    ProtocolVersion     helloVersion;

    int                 fragmentSize;

    final ReentrantLock recordLock = new ReentrantLock();

    InputRecord(HandshakeHash handshakeHash, SSLReadCipher readCipher) {
        this.readCipher = readCipher;
        this.helloVersion = ProtocolVersion.TLS10;
        this.handshakeHash = handshakeHash;
        this.isClosed = false;
        this.fragmentSize = Record.maxDataSize;
    }

    void setHelloVersion(ProtocolVersion helloVersion) {
        this.helloVersion = helloVersion;
    }

    boolean seqNumIsHuge() {
        return (readCipher.authenticator != null) &&
                        readCipher.authenticator.seqNumIsHuge();
    }

    boolean isEmpty() {
        return false;
    }

    void expectingFinishFlight() {
    }

    void finishHandshake() {
    }

    /**
     * Prevent any more data from being read into this record,
     * and flag the record as holding no data.
     */
    @Override
    public void close() throws IOException {
        recordLock.lock();
        try {
            if (!isClosed) {
                isClosed = true;
                readCipher.dispose();
            }
        } finally {
            recordLock.unlock();
        }
    }

    boolean isClosed() {
        return isClosed;
    }

    void changeReadCiphers(SSLReadCipher readCipher) {

        /*
         * Dispose of any intermediate state in the underlying cipher.
         * For PKCS11 ciphers, this will release any attached sessions,
         * and thus make finalization faster.
         *
         * Since MAC's doFinal() is called for every SSL/TLS packet, it's
         * not necessary to do the same with MAC's.
         */
        this.readCipher.dispose();

        this.readCipher = readCipher;
    }

    void changeFragmentSize(int fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    /*
     * Check if there is enough inbound data in the ByteBuffer to make
     * an inbound packet.
     *
     * @return -1 if there are not enough bytes to tell (small header),
     */
    int bytesInCompletePacket(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength) throws IOException {

        throw new UnsupportedOperationException("Not supported yet.");
    }

    int bytesInCompletePacket() throws IOException {
        throw new UnsupportedOperationException();
    }

    void setReceiverStream(InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    Plaintext acquirePlaintext()
            throws IOException {
        throw new UnsupportedOperationException();
    }

    abstract Plaintext[] decode(ByteBuffer[] srcs, int srcsOffset,
            int srcsLength) throws IOException, BadPaddingException;

    void setDeliverStream(OutputStream outputStream) {
        throw new UnsupportedOperationException();
    }

    int estimateFragmentSize(int packetSize) {
        throw new UnsupportedOperationException();
    }


    static ByteBuffer convertToClientHello(ByteBuffer packet) {
        int srcPos = packet.position();

        byte firstByte = packet.get();
        byte secondByte = packet.get();
        int recordLen = (((firstByte & 0x7F) << 8) | (secondByte & 0xFF)) + 2;

        packet.position(srcPos + 3);        

        byte majorVersion = packet.get();
        byte minorVersion = packet.get();

        int cipherSpecLen = ((packet.get() & 0xFF) << 8) +
                             (packet.get() & 0xFF);
        int sessionIdLen  = ((packet.get() & 0xFF) << 8) +
                             (packet.get() & 0xFF);
        int nonceLen      = ((packet.get() & 0xFF) << 8) +
                             (packet.get() & 0xFF);

        int requiredSize = 48 + sessionIdLen + ((cipherSpecLen * 2 ) / 3);
        byte[] converted = new byte[requiredSize];

        /*
         * Build the first part of the V3 record header from the V2 one
         * that's now buffered up.  (Lengths are fixed up later).
         */
        converted[0] = ContentType.HANDSHAKE.id;
        converted[1] = majorVersion;
        converted[2] = minorVersion;

        /*
         * Store the generic V3 handshake header:  4 bytes
         */
        converted[5] = 1;    

        /*
         * ClientHello header starts with SSL version
         */
        converted[9] = majorVersion;
        converted[10] = minorVersion;
        int pointer = 11;

        /*
         * Copy Random value/nonce ... if less than the 32 bytes of
         * a V3 "Random", right justify and zero pad to the left.  Else
         * just take the last 32 bytes.
         */
        int offset = srcPos + 11 + cipherSpecLen + sessionIdLen;

        if (nonceLen < 32) {
            for (int i = 0; i < (32 - nonceLen); i++) {
                converted[pointer++] = 0;
            }
            packet.position(offset);
            packet.get(converted, pointer, nonceLen);

            pointer += nonceLen;
        } else {
            packet.position(offset + nonceLen - 32);
            packet.get(converted, pointer, 32);

            pointer += 32;
        }

        /*
         * Copy session ID (only one byte length!)
         */
        offset -= sessionIdLen;
        converted[pointer++] = (byte)(sessionIdLen & 0xFF);
        packet.position(offset);
        packet.get(converted, pointer, sessionIdLen);

        /*
         * Copy and translate cipher suites ... V2 specs with first byte zero
         * are really V3 specs (in the last 2 bytes), just copy those and drop
         * the other ones.  Preference order remains unchanged.
         *
         * Example:  Netscape Navigator 3.0 (exportable) says:
         *
         * 0/3,     SSL_RSA_EXPORT_WITH_RC4_40_MD5
         * 0/6,     SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5
         *
         * Microsoft Internet Explorer 3.0 (exportable) supports only
         *
         * 0/3,     SSL_RSA_EXPORT_WITH_RC4_40_MD5
         */
        int j;

        offset -= cipherSpecLen;
        packet.position(offset);

        j = pointer + 2;
        for (int i = 0; i < cipherSpecLen; i += 3) {
            if (packet.get() != 0) {
                packet.get();           
                packet.get();           
                continue;
            }

            converted[j++] = packet.get();
            converted[j++] = packet.get();
        }

        j -= pointer + 2;
        converted[pointer++] = (byte)((j >>> 8) & 0xFF);
        converted[pointer++] = (byte)(j & 0xFF);
        pointer += j;

        /*
         * Append compression methods (default/null only)
         */
        converted[pointer++] = 1;
        converted[pointer++] = 0;      

        /*
         * Fill in lengths of the messages we synthesized (nested:
         * V3 handshake message within V3 record).
         */
        int fragLen = pointer - 5;                      
        converted[3] = (byte)((fragLen >>> 8) & 0xFF);
        converted[4] = (byte)(fragLen & 0xFF);

        /*
         * Handshake.length, length of ClientHello message
         */
        fragLen = pointer - 9;                          
        converted[6] = (byte)((fragLen >>> 16) & 0xFF);
        converted[7] = (byte)((fragLen >>> 8) & 0xFF);
        converted[8] = (byte)(fragLen & 0xFF);

        packet.position(srcPos + recordLen);

        return ByteBuffer.wrap(converted, 5, pointer - 5);  
    }

    static ByteBuffer extract(
            ByteBuffer[] buffers, int offset, int length, int headerSize) {

        boolean hasFullHeader = false;
        int contentLen = -1;
        for (int i = offset, j = 0;
                i < (offset + length) && j < headerSize; i++) {
            int remains = buffers[i].remaining();
            int pos = buffers[i].position();
            for (int k = 0; k < remains && j < headerSize; j++, k++) {
                byte b = buffers[i].get(pos + k);
                if (j == (headerSize - 2)) {
                    contentLen = ((b & 0xFF) << 8);
                } else if (j == (headerSize -1)) {
                    contentLen |= (b & 0xFF);
                    hasFullHeader = true;
                    break;
                }
            }
        }

        if (!hasFullHeader) {
            throw new BufferUnderflowException();
        }

        int packetLen = headerSize + contentLen;
        int remains = 0;
        for (int i = offset; i < offset + length; i++) {
            remains += buffers[i].remaining();
            if (remains >= packetLen) {
                break;
            }
        }

        if (remains < packetLen) {
            throw new BufferUnderflowException();
        }

        byte[] packet = new byte[packetLen];
        int packetOffset = 0;
        int packetSpaces = packetLen;
        for (int i = offset; i < offset + length; i++) {
            if (buffers[i].hasRemaining()) {
                int len = Math.min(packetSpaces, buffers[i].remaining());
                buffers[i].get(packet, packetOffset, len);
                packetOffset += len;
                packetSpaces -= len;
            }

            if (packetSpaces <= 0) {
                break;
            }
        }

        return ByteBuffer.wrap(packet);
    }
}
