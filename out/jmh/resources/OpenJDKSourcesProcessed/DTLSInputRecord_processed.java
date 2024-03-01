/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.*;
import javax.crypto.BadPaddingException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLProtocolException;
import sun.security.ssl.SSLCipher.SSLReadCipher;

/**
 * DTLS {@code InputRecord} implementation for {@code SSLEngine}.
 */
final class DTLSInputRecord extends InputRecord implements DTLSRecord {
    private DTLSReassembler reassembler = null;
    private int             readEpoch;

    DTLSInputRecord(HandshakeHash handshakeHash) {
        super(handshakeHash, SSLReadCipher.nullDTlsReadCipher());
        this.readEpoch = 0;
    }

    @Override
    void changeReadCiphers(SSLReadCipher readCipher) {
        this.readCipher = readCipher;
        this.readEpoch++;
    }

    @Override
    public void close() throws IOException {
        if (!isClosed) {
            super.close();
        }
    }

    @Override
    boolean isEmpty() {
        return ((reassembler == null) || reassembler.isEmpty());
    }

    @Override
    int estimateFragmentSize(int packetSize) {
        if (packetSize > 0) {
            return readCipher.estimateFragmentSize(packetSize, headerSize);
        } else {
            return Record.maxDataSize;
        }
    }

    @Override
    void expectingFinishFlight() {
        if (reassembler != null) {
            reassembler.expectingFinishFlight();
        }
    }

    @Override
    void finishHandshake() {
        reassembler = null;
    }

    @Override
    Plaintext acquirePlaintext() throws SSLProtocolException {
        if (reassembler != null) {
            return reassembler.acquirePlaintext();
        }

        return null;
    }

     @Override
    Plaintext[] decode(ByteBuffer[] srcs, int srcsOffset,
            int srcsLength) throws IOException, BadPaddingException {
        if (srcs == null || srcs.length == 0 || srcsLength == 0) {
            Plaintext pt = acquirePlaintext();
            return pt == null ? new Plaintext[0] : new Plaintext[] { pt };
        } else if (srcsLength == 1) {
            return decode(srcs[srcsOffset]);
        } else {
            ByteBuffer packet = extract(srcs,
                    srcsOffset, srcsLength, DTLSRecord.headerSize);
            return decode(packet);
        }
    }

    Plaintext[] decode(ByteBuffer packet) throws SSLProtocolException {
        if (isClosed) {
            return null;
        }

        if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
            SSLLogger.fine("Raw read", packet);
        }

        int srcPos = packet.position();
        int srcLim = packet.limit();

        byte contentType = packet.get();                   
        byte majorVersion = packet.get();                  
        byte minorVersion = packet.get();                  
        byte[] recordEnS = new byte[8];                    
        packet.get(recordEnS);
        int recordEpoch = ((recordEnS[0] & 0xFF) << 8) |
                           (recordEnS[1] & 0xFF);          
        long recordSeq  = ((recordEnS[2] & 0xFFL) << 40) |
                          ((recordEnS[3] & 0xFFL) << 32) |
                          ((recordEnS[4] & 0xFFL) << 24) |
                          ((recordEnS[5] & 0xFFL) << 16) |
                          ((recordEnS[6] & 0xFFL) <<  8) |
                           (recordEnS[7] & 0xFFL);         

        int contentLen = ((packet.get() & 0xFF) << 8) |
                          (packet.get() & 0xFF);           

        if (SSLLogger.isOn && SSLLogger.isOn("record")) {
            SSLLogger.fine("READ: " +
                    ProtocolVersion.nameOf(majorVersion, minorVersion) +
                    " " + ContentType.nameOf(contentType) + ", length = " +
                    contentLen);
        }

        int recLim = Math.addExact(srcPos, DTLSRecord.headerSize + contentLen);

        if (this.readEpoch > recordEpoch) {
            packet.position(recLim);
            if (SSLLogger.isOn && SSLLogger.isOn("record")) {
                SSLLogger.fine("READ: discard this old record", recordEnS);
            }
            return null;
        }

        if (this.readEpoch < recordEpoch) {
            if ((contentType != ContentType.HANDSHAKE.id &&
                    contentType != ContentType.CHANGE_CIPHER_SPEC.id) ||
                (reassembler == null &&
                    contentType != ContentType.HANDSHAKE.id) ||
                (this.readEpoch < (recordEpoch - 1))) {

                packet.position(recLim);

                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine("Premature record (epoch), discard it.");
                }

                return null;
            }


            byte[] fragment = new byte[contentLen];
            packet.get(fragment);              
            RecordFragment buffered = new RecordFragment(fragment, contentType,
                    majorVersion, minorVersion,
                    recordEnS, recordEpoch, recordSeq, true);

            if (reassembler == null) {
                reassembler = new DTLSReassembler(recordEpoch);
            }
            reassembler.queueUpFragment(buffered);

            packet.position(recLim);

            Plaintext pt = reassembler.acquirePlaintext();
            return pt == null ? null : new Plaintext[] { pt };
        }

        packet.limit(recLim);
        packet.position(srcPos + DTLSRecord.headerSize);

        ByteBuffer plaintextFragment;
        try {
            Plaintext plaintext =
                    readCipher.decrypt(contentType, packet, recordEnS);
            plaintextFragment = plaintext.fragment;
            contentType = plaintext.contentType;
        } catch (GeneralSecurityException gse) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.fine("Discard invalid record: " + gse);
            }

            return null;
        } finally {
            packet.limit(srcLim);
            packet.position(recLim);
        }

        if (contentType != ContentType.CHANGE_CIPHER_SPEC.id &&
            contentType != ContentType.HANDSHAKE.id) {   
            if ((reassembler != null) &&
                    (reassembler.handshakeEpoch < recordEpoch)) {
                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine("Cleanup the handshake reassembler");
                }

                reassembler = null;
            }

            return new Plaintext[] {
                    new Plaintext(contentType, majorVersion, minorVersion,
                            recordEpoch, Authenticator.toLong(recordEnS),
                            plaintextFragment)};
        }

        if (contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
            if (reassembler == null) {
                reassembler = new DTLSReassembler(recordEpoch);
            }

            reassembler.queueUpChangeCipherSpec(
                    new RecordFragment(plaintextFragment, contentType,
                            majorVersion, minorVersion,
                            recordEnS, recordEpoch, recordSeq, false));
        } else {    
            while (plaintextFragment.remaining() > 0) {

                HandshakeFragment hsFrag = parseHandshakeMessage(
                    contentType, majorVersion, minorVersion,
                    recordEnS, recordEpoch, recordSeq, plaintextFragment);

                if (hsFrag == null) {
                    if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                        SSLLogger.fine(
                                "Invalid handshake message, discard it.");
                    }

                    return null;
                }

                if (reassembler == null) {
                    reassembler = new DTLSReassembler(recordEpoch);
                }

                reassembler.queueUpHandshake(hsFrag);
            }
        }

        if (reassembler != null) {
            Plaintext pt = reassembler.acquirePlaintext();
            return pt == null ? null : new Plaintext[] { pt };
        }

        if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
             SSLLogger.fine("The reassembler is not initialized yet.");
        }

        return null;
    }

    @Override
    int bytesInCompletePacket(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength) throws IOException {

        return bytesInCompletePacket(srcs[srcsOffset]);
    }

    private int bytesInCompletePacket(ByteBuffer packet) throws SSLException {

        if (packet.remaining() < headerSize) {
            return -1;
        }

        int pos = packet.position();

        byte contentType = packet.get(pos);
        if (ContentType.valueOf(contentType) == null) {
            throw new SSLException(
                    "Unrecognized SSL message, plaintext connection?");
        }

        byte majorVersion = packet.get(pos + 1);
        byte minorVersion = packet.get(pos + 2);
        if (!ProtocolVersion.isNegotiable(
                majorVersion, minorVersion, true, false)) {
            throw new SSLException("Unrecognized record version " +
                    ProtocolVersion.nameOf(majorVersion, minorVersion) +
                    " , plaintext connection?");
        }

        int fragLen = ((packet.get(pos + 11) & 0xFF) << 8) +
                       (packet.get(pos + 12) & 0xFF) + headerSize;
        if (fragLen > Record.maxFragmentSize) {
            throw new SSLException(
                    "Record overflow, fragment length (" + fragLen +
                    ") MUST not exceed " + Record.maxFragmentSize);
        }

        return fragLen;
    }

    private static HandshakeFragment parseHandshakeMessage(
            byte contentType, byte majorVersion, byte minorVersion,
            byte[] recordEnS, int recordEpoch, long recordSeq,
            ByteBuffer plaintextFragment) throws SSLProtocolException {

        int remaining = plaintextFragment.remaining();
        if (remaining < handshakeHeaderSize) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.fine("Discard invalid record: " +
                        "too small record to hold a handshake fragment");
            }

            return null;
        }

        byte handshakeType = plaintextFragment.get();       
        if (!SSLHandshake.isKnown(handshakeType)) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.fine("Discard invalid record: " +
                        "unknown handshake type size, Handshake.msg_type = " +
                        (handshakeType & 0xFF));
            }

            return null;
        }

        int messageLength =
                ((plaintextFragment.get() & 0xFF) << 16) |
                ((plaintextFragment.get() & 0xFF) << 8) |
                 (plaintextFragment.get() & 0xFF);          

        if (messageLength > SSLConfiguration.maxHandshakeMessageSize) {
            throw new SSLProtocolException(
                    "The size of the handshake message ("
                    + messageLength
                    + ") exceeds the maximum allowed size ("
                    + SSLConfiguration.maxHandshakeMessageSize
                    + ")");
        }

        int messageSeq =
                ((plaintextFragment.get() & 0xFF) << 8) |
                 (plaintextFragment.get() & 0xFF);          
        int fragmentOffset =
                ((plaintextFragment.get() & 0xFF) << 16) |
                ((plaintextFragment.get() & 0xFF) << 8) |
                 (plaintextFragment.get() & 0xFF);          
        int fragmentLength =
                ((plaintextFragment.get() & 0xFF) << 16) |
                ((plaintextFragment.get() & 0xFF) << 8) |
                 (plaintextFragment.get() & 0xFF);          
        if ((remaining - handshakeHeaderSize) < fragmentLength) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.fine("Discard invalid record: " +
                        "not a complete handshake fragment in the record");
            }

            return null;
        }

        byte[] fragment = new byte[fragmentLength];
        plaintextFragment.get(fragment);

        return new HandshakeFragment(fragment, contentType,
                majorVersion, minorVersion,
                recordEnS, recordEpoch, recordSeq,
                handshakeType, messageLength,
                messageSeq, fragmentOffset, fragmentLength);
    }

    private static class RecordFragment implements Comparable<RecordFragment> {
        boolean         isCiphertext;

        byte            contentType;
        byte            majorVersion;
        byte            minorVersion;
        int             recordEpoch;
        long            recordSeq;
        byte[]          recordEnS;
        byte[]          fragment;

        RecordFragment(ByteBuffer fragBuf, byte contentType,
                byte majorVersion, byte minorVersion, byte[] recordEnS,
                int recordEpoch, long recordSeq, boolean isCiphertext) {
            this((byte[])null, contentType, majorVersion, minorVersion,
                    recordEnS, recordEpoch, recordSeq, isCiphertext);

            this.fragment = new byte[fragBuf.remaining()];
            fragBuf.get(this.fragment);
        }

        RecordFragment(byte[] fragment, byte contentType,
                byte majorVersion, byte minorVersion, byte[] recordEnS,
                int recordEpoch, long recordSeq, boolean isCiphertext) {
            this.isCiphertext = isCiphertext;

            this.contentType = contentType;
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.recordEpoch = recordEpoch;
            this.recordSeq = recordSeq;
            this.recordEnS = recordEnS;
            this.fragment = fragment;       
        }

        @Override
        public int compareTo(RecordFragment o) {
            if (this.contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
                if (o.contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
                    return Integer.compare(this.recordEpoch, o.recordEpoch);
                } else if ((this.recordEpoch == o.recordEpoch) &&
                        (o.contentType == ContentType.HANDSHAKE.id)) {
                    return 1;
                }
            } else if (o.contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
                if ((this.recordEpoch == o.recordEpoch) &&
                        (this.contentType == ContentType.HANDSHAKE.id)) {
                    return -1;
                } else {
                    return compareToSequence(o.recordEpoch, o.recordSeq);
                }
            }

            return compareToSequence(o.recordEpoch, o.recordSeq);
        }

        int compareToSequence(int epoch, long seq) {
            if (this.recordEpoch > epoch) {
                return 1;
            } else if (this.recordEpoch == epoch) {
                return Long.compare(this.recordSeq, seq);
            } else {
                return -1;
            }
        }
    }

    private static final class HandshakeFragment extends RecordFragment {

        byte            handshakeType;     
        int             messageSeq;        
        int             messageLength;     
        int             fragmentOffset;    
        int             fragmentLength;    

        HandshakeFragment(byte[] fragment, byte contentType,
                byte majorVersion, byte minorVersion, byte[] recordEnS,
                int recordEpoch, long recordSeq,
                byte handshakeType, int messageLength,
                int messageSeq, int fragmentOffset, int fragmentLength) {

            super(fragment, contentType, majorVersion, minorVersion,
                    recordEnS, recordEpoch , recordSeq, false);

            this.handshakeType = handshakeType;
            this.messageSeq = messageSeq;
            this.messageLength = messageLength;
            this.fragmentOffset = fragmentOffset;
            this.fragmentLength = fragmentLength;
        }

        @Override
        public int compareTo(RecordFragment o) {
            if (o instanceof HandshakeFragment other) {
                if (this.messageSeq != other.messageSeq) {
                    return this.messageSeq - other.messageSeq;
                } else if (this.fragmentOffset != other.fragmentOffset) {
                    return this.fragmentOffset - other.fragmentOffset;
                } else if (this.fragmentLength == other.fragmentLength) {
                    return 0;
                }

                return compareToSequence(o.recordEpoch, o.recordSeq);
            }

            return super.compareTo(o);
        }
    }

    private static final class HoleDescriptor {
        int offset;             
        int limit;              

        HoleDescriptor(int offset, int limit) {
            this.offset = offset;
            this.limit = limit;
        }
    }

    private static final class HandshakeFlight implements Cloneable {
        static final byte HF_UNKNOWN = SSLHandshake.NOT_APPLICABLE.id;

        byte        handshakeType;      
        int         flightEpoch;        
        int         minMessageSeq;      

        int         maxMessageSeq;      
        int         maxRecordEpoch;     
        long        maxRecordSeq;       

        HashMap<Byte, List<HoleDescriptor>> holesMap;

        HashMap<Byte, Integer> messageSeqMap;

        HandshakeFlight() {
            this.handshakeType = HF_UNKNOWN;
            this.flightEpoch = 0;
            this.minMessageSeq = 0;

            this.maxMessageSeq = 0;
            this.maxRecordEpoch = 0;
            this.maxRecordSeq = -1;

            this.holesMap = new HashMap<>(5);
            this.messageSeqMap = new HashMap<>(5);
        }

        boolean isRetransmitOf(HandshakeFlight hs) {
            return (hs != null) &&
                   (this.handshakeType == hs.handshakeType) &&
                   (this.minMessageSeq == hs.minMessageSeq);
        }

        @Override
        public Object clone() {
            HandshakeFlight hf = new HandshakeFlight();

            hf.handshakeType = this.handshakeType;
            hf.flightEpoch = this.flightEpoch;
            hf.minMessageSeq = this.minMessageSeq;

            hf.maxMessageSeq = this.maxMessageSeq;
            hf.maxRecordEpoch = this.maxRecordEpoch;
            hf.maxRecordSeq = this.maxRecordSeq;

            hf.holesMap = new HashMap<>(this.holesMap);
            hf.messageSeqMap = new HashMap<>(this.messageSeqMap);

            return hf;
        }
    }

    final class DTLSReassembler {
        final int handshakeEpoch;

        TreeSet<RecordFragment> bufferedFragments = new TreeSet<>();

        HandshakeFlight handshakeFlight = new HandshakeFlight();

        HandshakeFlight precedingFlight = null;

        int         nextRecordEpoch;        
        long        nextRecordSeq = 0;      

        boolean     expectCCSFlight = false;

        boolean     flightIsReady = false;
        boolean     needToCheckFlight = false;

        DTLSReassembler(int handshakeEpoch) {
            this.handshakeEpoch = handshakeEpoch;
            this.nextRecordEpoch = handshakeEpoch;

            this.handshakeFlight.flightEpoch = handshakeEpoch;
        }

        void expectingFinishFlight() {
            expectCCSFlight = true;
        }

        void queueUpHandshake(HandshakeFragment hsf) throws SSLProtocolException {
            if (!isDesirable(hsf)) {
                return;
            }

            cleanUpRetransmit(hsf);

            boolean isMinimalFlightMessage = false;
            if (handshakeFlight.minMessageSeq == hsf.messageSeq) {
                isMinimalFlightMessage = true;
            } else if ((precedingFlight != null) &&
                    (precedingFlight.minMessageSeq == hsf.messageSeq)) {
                isMinimalFlightMessage = true;
            }

            if (isMinimalFlightMessage && (hsf.fragmentOffset == 0) &&
                    (hsf.handshakeType != SSLHandshake.FINISHED.id)) {

                handshakeFlight.handshakeType = hsf.handshakeType;
                handshakeFlight.flightEpoch = hsf.recordEpoch;
                handshakeFlight.minMessageSeq = hsf.messageSeq;
            }

            if (hsf.handshakeType == SSLHandshake.FINISHED.id) {
                handshakeFlight.maxMessageSeq = hsf.messageSeq;
                handshakeFlight.maxRecordEpoch = hsf.recordEpoch;
                handshakeFlight.maxRecordSeq = hsf.recordSeq;
            } else {
                if (handshakeFlight.maxMessageSeq < hsf.messageSeq) {
                    handshakeFlight.maxMessageSeq = hsf.messageSeq;
                }

                int n = (hsf.recordEpoch - handshakeFlight.maxRecordEpoch);
                if (n > 0) {
                    handshakeFlight.maxRecordEpoch = hsf.recordEpoch;
                    handshakeFlight.maxRecordSeq = hsf.recordSeq;
                } else if (n == 0) {
                    if (handshakeFlight.maxRecordSeq < hsf.recordSeq) {
                        handshakeFlight.maxRecordSeq = hsf.recordSeq;
                    }
                }   
            }

            boolean fragmented = (hsf.fragmentOffset) != 0 ||
                    (hsf.fragmentLength != hsf.messageLength);

            List<HoleDescriptor> holes =
                    handshakeFlight.holesMap.get(hsf.handshakeType);
            if (holes == null) {
                if (!fragmented) {
                    holes = Collections.emptyList();
                } else {
                    holes = new LinkedList<>();
                    holes.add(new HoleDescriptor(0, hsf.messageLength));
                }
                handshakeFlight.holesMap.put(hsf.handshakeType, holes);
                handshakeFlight.messageSeqMap.put(hsf.handshakeType, hsf.messageSeq);
            } else if (holes.isEmpty()) {
                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine("Have got the full message, discard it.");
                }

                return;
            }

            if (fragmented) {
                int fragmentLimit = hsf.fragmentOffset + hsf.fragmentLength;
                for (int i = 0; i < holes.size(); i++) {

                    HoleDescriptor hole = holes.get(i);
                    if ((hole.limit <= hsf.fragmentOffset) ||
                        (hole.offset >= fragmentLimit)) {
                        continue;
                    }

                    if (hole.offset > hsf.fragmentOffset || hole.limit < fragmentLimit) {

                        if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                            SSLLogger.fine("Discard invalid record: " +
                                "handshake fragment ranges are overlapping");
                        }

                        return;
                    }

                    holes.remove(i);

                    if (hsf.fragmentOffset > hole.offset) {
                        holes.add(new HoleDescriptor(
                                hole.offset, hsf.fragmentOffset));
                    }

                    if (fragmentLimit < hole.limit) {
                        holes.add(new HoleDescriptor(
                                fragmentLimit, hole.limit));
                    }

                    break;
                }
            }

            if (hsf.handshakeType == SSLHandshake.FINISHED.id) {
                bufferedFragments.add(hsf);
            } else {
                bufferFragment(hsf);
            }
        }

        void queueUpChangeCipherSpec(RecordFragment rf)
                throws SSLProtocolException {
            if (!isDesirable(rf)) {
                return;
            }

            cleanUpRetransmit(rf);

            if (expectCCSFlight) {
                handshakeFlight.handshakeType = HandshakeFlight.HF_UNKNOWN;
                handshakeFlight.flightEpoch = rf.recordEpoch;
            }

            if (handshakeFlight.maxRecordSeq < rf.recordSeq) {
                handshakeFlight.maxRecordSeq = rf.recordSeq;
            }

            bufferFragment(rf);
        }

        void queueUpFragment(RecordFragment rf) throws SSLProtocolException {
            if (!isDesirable(rf)) {
                return;
            }

            cleanUpRetransmit(rf);

            bufferFragment(rf);
        }

        private void bufferFragment(RecordFragment rf) {
            bufferedFragments.add(rf);

            if (flightIsReady) {
                flightIsReady = false;
            }

            if (!needToCheckFlight) {
                needToCheckFlight = true;
            }
        }

        private void cleanUpRetransmit(RecordFragment rf) {
            boolean isNewFlight = false;
            if (precedingFlight != null) {
                if (precedingFlight.flightEpoch < rf.recordEpoch) {
                    isNewFlight = true;
                } else {
                    if (rf instanceof HandshakeFragment hsf) {
                        if (precedingFlight.maxMessageSeq  < hsf.messageSeq) {
                            isNewFlight = true;
                        }
                    } else if (
                        rf.contentType != ContentType.CHANGE_CIPHER_SPEC.id) {

                        if (precedingFlight.maxRecordEpoch < rf.recordEpoch) {
                            isNewFlight = true;
                        }
                    }
                }
            }

            if (!isNewFlight) {
                return;
            }

            for (Iterator<RecordFragment> it = bufferedFragments.iterator();
                    it.hasNext();) {

                RecordFragment frag = it.next();
                boolean isOld = false;
                if (frag.recordEpoch < precedingFlight.maxRecordEpoch) {
                    isOld = true;
                } else if (frag.recordEpoch == precedingFlight.maxRecordEpoch) {
                    if (frag.recordSeq <= precedingFlight.maxRecordSeq) {
                        isOld = true;
                    }
                }

                if (!isOld && (frag instanceof HandshakeFragment hsf)) {
                    isOld = (hsf.messageSeq <= precedingFlight.maxMessageSeq);
                }

                if (isOld) {
                    it.remove();
                } else {
                    break;
                }
            }

            precedingFlight = null;
        }

        private boolean isDesirable(RecordFragment rf) throws SSLProtocolException {
            int previousEpoch = nextRecordEpoch - 1;
            if (rf.recordEpoch < previousEpoch) {
                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine(
                            "Too old epoch to use this record, discard it.");
                }

                return false;
            }

            if (rf.recordEpoch == previousEpoch) {
                boolean isDesired = true;
                if (precedingFlight == null) {
                    isDesired = false;
                } else {
                    if (rf instanceof HandshakeFragment hsf) {
                        if (precedingFlight.minMessageSeq > hsf.messageSeq) {
                            isDesired = false;
                        }
                    } else if (
                        rf.contentType == ContentType.CHANGE_CIPHER_SPEC.id) {

                        if (precedingFlight.flightEpoch != rf.recordEpoch) {
                            isDesired = false;
                        }
                    } else {        
                        if ((rf.recordEpoch < precedingFlight.maxRecordEpoch) ||
                            (rf.recordEpoch == precedingFlight.maxRecordEpoch &&
                                rf.recordSeq <= precedingFlight.maxRecordSeq)) {
                            isDesired = false;
                        }
                    }
                }

                if (!isDesired) {
                    if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                        SSLLogger.fine(
                                "Too old retransmission to use, discard it.");
                    }

                    return false;
                }
            } else if ((rf.recordEpoch == nextRecordEpoch) &&
                    (nextRecordSeq > rf.recordSeq)) {

                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine(
                            "Lagging behind record (sequence), discard it.");
                }

                return false;
            }

            if (rf.recordEpoch == handshakeEpoch &&
                    rf instanceof HandshakeFragment hsf &&
                    handshakeFlight.holesMap.containsKey(hsf.handshakeType)) {
                Integer cachedMsgSeq = handshakeFlight.messageSeqMap.get(
                        hsf.handshakeType);
                if (cachedMsgSeq != null && cachedMsgSeq != hsf.messageSeq) {
                    throw new SSLProtocolException(
                            "Two message sequence numbers are used for the "
                          + "same handshake message ("
                          + SSLHandshake.nameOf(hsf.handshakeType)
                          + ")");
                }
            }

            return true;
        }

        private boolean isEmpty() {
            return (bufferedFragments.isEmpty() ||
                    (!flightIsReady && !needToCheckFlight) ||
                    (needToCheckFlight && !flightIsReady()));
        }

        Plaintext acquirePlaintext() throws SSLProtocolException {
            if (bufferedFragments.isEmpty()) {
                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine("No received handshake messages");
                }
                return null;
            }

            if (!flightIsReady && needToCheckFlight) {
                flightIsReady = flightIsReady();

                if (flightIsReady) {
                    if (handshakeFlight.isRetransmitOf(precedingFlight)) {
                        bufferedFragments.clear();

                        resetHandshakeFlight(precedingFlight);

                        if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                            SSLLogger.fine("Received a retransmission flight.");
                        }

                        return Plaintext.PLAINTEXT_NULL;
                    }
                }

                needToCheckFlight = false;
            }

            if (!flightIsReady) {
                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine(
                            "The handshake flight is not ready to use: " +
                            handshakeFlight.handshakeType);
                }
                return null;
            }

            RecordFragment rFrag = bufferedFragments.first();
            Plaintext plaintext;
            if (!rFrag.isCiphertext) {
                plaintext = acquireHandshakeMessage();

                if (bufferedFragments.isEmpty()) {
                    handshakeFlight.holesMap.clear();   

                    precedingFlight = (HandshakeFlight)handshakeFlight.clone();

                    resetHandshakeFlight(precedingFlight);

                    if (expectCCSFlight &&
                            (precedingFlight.handshakeType ==
                                    HandshakeFlight.HF_UNKNOWN)) {
                        expectCCSFlight = false;
                    }
                }
            } else {
                plaintext = acquireCachedMessage();
            }

            return plaintext;
        }

        private void resetHandshakeFlight(HandshakeFlight prev) {
            handshakeFlight.handshakeType = HandshakeFlight.HF_UNKNOWN;
            handshakeFlight.flightEpoch = prev.maxRecordEpoch;
            if (prev.flightEpoch != prev.maxRecordEpoch) {
                handshakeFlight.minMessageSeq = 0;
            } else {
                handshakeFlight.minMessageSeq = prev.maxMessageSeq + 1;
            }

            handshakeFlight.maxMessageSeq = 0;
            handshakeFlight.maxRecordEpoch = handshakeFlight.flightEpoch;

            handshakeFlight.maxRecordSeq = prev.maxRecordSeq + 1;

            handshakeFlight.holesMap.clear();

            handshakeFlight.messageSeqMap.clear();

            flightIsReady = false;
            needToCheckFlight = false;
        }

        private Plaintext acquireCachedMessage() throws SSLProtocolException {
            RecordFragment rFrag = bufferedFragments.first();
            if (readEpoch != rFrag.recordEpoch) {
                if (readEpoch > rFrag.recordEpoch) {
                    if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                        SSLLogger.fine(
                                "Discard old buffered ciphertext fragments.");
                    }
                    bufferedFragments.remove(rFrag);    
                }

                if (flightIsReady) {
                    flightIsReady = false;
                }

                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine(
                            "Not yet ready to decrypt the cached fragments.");
                }
                return null;
            }

            bufferedFragments.remove(rFrag);    

            ByteBuffer fragment = ByteBuffer.wrap(rFrag.fragment);
            ByteBuffer plaintextFragment;
            try {
                Plaintext plaintext = readCipher.decrypt(
                        rFrag.contentType, fragment, rFrag.recordEnS);
                plaintextFragment = plaintext.fragment;
                rFrag.contentType = plaintext.contentType;
            } catch (GeneralSecurityException gse) {
                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine("Discard invalid record: ", gse);
                }

                return null;
            }

            if (rFrag.contentType == ContentType.HANDSHAKE.id) {
                while (plaintextFragment.remaining() > 0) {
                    HandshakeFragment hsFrag = parseHandshakeMessage(
                            rFrag.contentType,
                            rFrag.majorVersion, rFrag.minorVersion,
                            rFrag.recordEnS, rFrag.recordEpoch, rFrag.recordSeq,
                            plaintextFragment);

                    if (hsFrag == null) {
                        if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                            SSLLogger.fine(
                                    "Invalid handshake fragment, discard it",
                                    plaintextFragment);
                        }
                        return null;
                    }

                    queueUpHandshake(hsFrag);
                    if (hsFrag.handshakeType != SSLHandshake.FINISHED.id) {
                        flightIsReady = false;
                        needToCheckFlight = true;
                    }
                }

                return acquirePlaintext();
            } else {
                return new Plaintext(rFrag.contentType,
                        rFrag.majorVersion, rFrag.minorVersion,
                        rFrag.recordEpoch,
                        Authenticator.toLong(rFrag.recordEnS),
                        plaintextFragment);
            }
        }

        private Plaintext acquireHandshakeMessage() {

            RecordFragment rFrag = bufferedFragments.first();
            if (rFrag.contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
                this.nextRecordEpoch = rFrag.recordEpoch + 1;

                this.nextRecordSeq = 0;

                bufferedFragments.remove(rFrag);
                return new Plaintext(rFrag.contentType,
                        rFrag.majorVersion, rFrag.minorVersion,
                        rFrag.recordEpoch,
                        Authenticator.toLong(rFrag.recordEnS),
                        ByteBuffer.wrap(rFrag.fragment));
            } else {    
                HandshakeFragment hsFrag = (HandshakeFragment)rFrag;
                if ((hsFrag.messageLength == hsFrag.fragmentLength) &&
                    (hsFrag.fragmentOffset == 0)) {     

                    bufferedFragments.remove(rFrag);    

                    this.nextRecordSeq = hsFrag.recordSeq + 1;

                    byte[] recordFrag = new byte[hsFrag.messageLength + 4];
                    Plaintext plaintext = new Plaintext(
                            hsFrag.contentType,
                            hsFrag.majorVersion, hsFrag.minorVersion,
                            hsFrag.recordEpoch,
                            Authenticator.toLong(hsFrag.recordEnS),
                            ByteBuffer.wrap(recordFrag));

                    recordFrag[0] = hsFrag.handshakeType;
                    recordFrag[1] =
                            (byte)((hsFrag.messageLength >>> 16) & 0xFF);
                    recordFrag[2] =
                            (byte)((hsFrag.messageLength >>> 8) & 0xFF);
                    recordFrag[3] = (byte)(hsFrag.messageLength & 0xFF);

                    System.arraycopy(hsFrag.fragment, 0,
                            recordFrag, 4, hsFrag.fragmentLength);

                    handshakeHashing(hsFrag, plaintext);

                    return plaintext;
                } else {                
                    byte[] recordFrag = new byte[hsFrag.messageLength + 4];
                    Plaintext plaintext = new Plaintext(
                            hsFrag.contentType,
                            hsFrag.majorVersion, hsFrag.minorVersion,
                            hsFrag.recordEpoch,
                            Authenticator.toLong(hsFrag.recordEnS),
                            ByteBuffer.wrap(recordFrag));

                    recordFrag[0] = hsFrag.handshakeType;
                    recordFrag[1] =
                            (byte)((hsFrag.messageLength >>> 16) & 0xFF);
                    recordFrag[2] =
                            (byte)((hsFrag.messageLength >>> 8) & 0xFF);
                    recordFrag[3] = (byte)(hsFrag.messageLength & 0xFF);

                    int msgSeq = hsFrag.messageSeq;
                    long maxRecodeSN = hsFrag.recordSeq;
                    HandshakeFragment hmFrag = hsFrag;
                    do {
                        System.arraycopy(hmFrag.fragment, 0,
                                recordFrag, hmFrag.fragmentOffset + 4,
                                hmFrag.fragmentLength);
                        bufferedFragments.remove(rFrag);

                        if (maxRecodeSN < hmFrag.recordSeq) {
                            maxRecodeSN = hmFrag.recordSeq;
                        }


                        if (!bufferedFragments.isEmpty()) {
                            rFrag = bufferedFragments.first();
                            if (rFrag.contentType != ContentType.HANDSHAKE.id) {
                                break;
                            } else {
                                hmFrag = (HandshakeFragment)rFrag;
                            }
                        }
                    } while (!bufferedFragments.isEmpty() &&
                            (msgSeq == hmFrag.messageSeq));

                    handshakeHashing(hsFrag, plaintext);

                    this.nextRecordSeq = maxRecodeSN + 1;

                    return plaintext;
                }
            }
        }

        boolean flightIsReady() {

            byte flightType = handshakeFlight.handshakeType;
            if (flightType == HandshakeFlight.HF_UNKNOWN) {
                if (expectCCSFlight) {
                    boolean isReady = hasFinishedMessage(bufferedFragments);
                    if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                        SSLLogger.fine(
                            "Has the final flight been received? " + isReady);
                    }

                    return isReady;
                }

                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine("No flight is received yet.");
                }

                return false;
            }

            if ((flightType == SSLHandshake.CLIENT_HELLO.id) ||
                (flightType == SSLHandshake.HELLO_REQUEST.id) ||
                (flightType == SSLHandshake.HELLO_VERIFY_REQUEST.id)) {

                boolean isReady = hasCompleted(flightType);
                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine(
                            "Is the handshake message completed? " + isReady);
                }

                return isReady;
            }

            if (flightType == SSLHandshake.SERVER_HELLO.id) {
                if (!hasCompleted(flightType)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                        SSLLogger.fine(
                            "The ServerHello message is not completed yet.");
                    }

                    return false;
                }

                if (hasFinishedMessage(bufferedFragments)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                        SSLLogger.fine("It's an abbreviated handshake.");
                    }

                    return true;
                }

                List<HoleDescriptor> holes = handshakeFlight.holesMap.get(
                        SSLHandshake.SERVER_HELLO_DONE.id);
                if ((holes == null) || !holes.isEmpty()) {
                    if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                        SSLLogger.fine(
                                "Not yet got the ServerHelloDone message");
                    }

                    return false;
                }

                boolean isReady = hasCompleted(bufferedFragments,
                            handshakeFlight.minMessageSeq,
                            handshakeFlight.maxMessageSeq);
                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine(
                            "Is the ServerHello flight (message " +
                            handshakeFlight.minMessageSeq + "-" +
                            handshakeFlight.maxMessageSeq +
                            ") completed? " + isReady);
                }

                return isReady;
            }

            if ((flightType == SSLHandshake.CERTIFICATE.id) ||
                (flightType == SSLHandshake.CLIENT_KEY_EXCHANGE.id)) {

                if (!hasCompleted(flightType)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                        SSLLogger.fine(
                            "The ClientKeyExchange or client Certificate " +
                            "message is not completed yet.");
                    }

                    return false;
                }

                if (flightType == SSLHandshake.CERTIFICATE.id) {
                    if (needClientVerify(bufferedFragments) &&
                        !hasCompleted(SSLHandshake.CERTIFICATE_VERIFY.id)) {

                        if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                            SSLLogger.fine(
                                "Not yet have the CertificateVerify message");
                        }

                        return false;
                    }
                }

                if (!hasFinishedMessage(bufferedFragments)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                        SSLLogger.fine(
                            "Not yet have the ChangeCipherSpec and " +
                            "Finished messages");
                    }

                    return false;
                }

                boolean isReady = hasCompleted(bufferedFragments,
                            handshakeFlight.minMessageSeq,
                            handshakeFlight.maxMessageSeq);
                if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                    SSLLogger.fine(
                            "Is the ClientKeyExchange flight (message " +
                            handshakeFlight.minMessageSeq + "-" +
                            handshakeFlight.maxMessageSeq +
                            ") completed? " + isReady);
                }

                return isReady;
            }

            if (SSLLogger.isOn && SSLLogger.isOn("verbose")) {
                SSLLogger.fine("Need to receive more handshake messages");
            }

            return false;
        }

        private boolean hasFinishedMessage(Set<RecordFragment> fragments) {

            boolean hasCCS = false;
            boolean hasFin = false;
            for (RecordFragment fragment : fragments) {
                if (fragment.contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
                    if (hasFin) {
                        return true;
                    }
                    hasCCS = true;
                } else if (fragment.contentType == ContentType.HANDSHAKE.id) {
                    if (fragment.isCiphertext) {
                        if (hasCCS) {
                            return true;
                        }
                        hasFin = true;
                    }
                }
            }

            return false;
        }

        private boolean needClientVerify(Set<RecordFragment> fragments) {

            for (RecordFragment rFrag : fragments) {
                if ((rFrag.contentType != ContentType.HANDSHAKE.id) ||
                        rFrag.isCiphertext) {
                    break;
                }

                HandshakeFragment hsFrag = (HandshakeFragment)rFrag;
                if (hsFrag.handshakeType != SSLHandshake.CERTIFICATE.id) {
                    continue;
                }

                return (rFrag.fragment != null) &&
                   (rFrag.fragment.length > DTLSRecord.minCertPlaintextSize);
            }

            return false;
        }

        private boolean hasCompleted(byte handshakeType) {
            List<HoleDescriptor> holes =
                    handshakeFlight.holesMap.get(handshakeType);
            if (holes == null) {
                return false;
            }

            return holes.isEmpty();  
        }

        private boolean hasCompleted(
                Set<RecordFragment> fragments,
                int presentMsgSeq, int endMsgSeq) {

            for (RecordFragment rFrag : fragments) {
                if ((rFrag.contentType != ContentType.HANDSHAKE.id) ||
                        rFrag.isCiphertext) {
                    break;
                }

                HandshakeFragment hsFrag = (HandshakeFragment)rFrag;
                if (hsFrag.messageSeq == presentMsgSeq) {
                    continue;
                } else if (hsFrag.messageSeq == (presentMsgSeq + 1)) {
                    if (!hasCompleted(hsFrag.handshakeType)) {
                        return false;
                    }

                    presentMsgSeq = hsFrag.messageSeq;
                } else {
                    break;
                }
            }

            return (presentMsgSeq >= endMsgSeq);
        }

        private void handshakeHashing(
                HandshakeFragment hsFrag, Plaintext plaintext) {
            byte hsType = hsFrag.handshakeType;
            if (!handshakeHash.isHashable(hsType)) {
                return;
            }

            plaintext.fragment.position(4);     
            byte[] temporary = new byte[plaintext.fragment.remaining() + 12];

            temporary[0] = hsFrag.handshakeType;

            temporary[1] = (byte)((hsFrag.messageLength >> 16) & 0xFF);
            temporary[2] = (byte)((hsFrag.messageLength >> 8) & 0xFF);
            temporary[3] = (byte)(hsFrag.messageLength & 0xFF);

            temporary[4] = (byte)((hsFrag.messageSeq >> 8) & 0xFF);
            temporary[5] = (byte)(hsFrag.messageSeq & 0xFF);

            temporary[6] = 0;
            temporary[7] = 0;
            temporary[8] = 0;

            temporary[9] = temporary[1];
            temporary[10] = temporary[2];
            temporary[11] = temporary[3];

            plaintext.fragment.get(temporary,
                    12, plaintext.fragment.remaining());
            handshakeHash.receive(temporary);
            plaintext.fragment.position(0);     
        }
    }
}

