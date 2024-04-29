/*
 * @notice
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2022 Elasticsearch B.V.
 */
package org.elasticsearch.index.codec.postings;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.CompetitiveImpactAccumulator;
import org.apache.lucene.codecs.PushPostingsWriterBase;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BitUtil;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.core.IOUtils;
import org.elasticsearch.index.codec.postings.ES812PostingsFormat.IntBlockTermState;

import java.io.IOException;

import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.BLOCK_SIZE;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.DOC_CODEC;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.MAX_SKIP_LEVELS;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.PAY_CODEC;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.POS_CODEC;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.TERMS_CODEC;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.VERSION_CURRENT;

/**
 * Concrete class that writes docId(maybe frq,pos,offset,payloads) list with postings format.
 *
 * <p>Postings list for each term will be stored separately.
 *
 * @see ES812SkipWriter for details about skipping setting and postings layout.
 */
final class ES812PostingsWriter extends PushPostingsWriterBase {

    IndexOutput docOut;
    IndexOutput posOut;
    IndexOutput payOut;

    static final IntBlockTermState emptyState = new IntBlockTermState();
    IntBlockTermState lastState;

    private long docStartFP;
    private long posStartFP;
    private long payStartFP;

    final long[] docDeltaBuffer;
    final long[] freqBuffer;
    private int docBufferUpto;

    final long[] posDeltaBuffer;
    final long[] payloadLengthBuffer;
    final long[] offsetStartDeltaBuffer;
    final long[] offsetLengthBuffer;
    private int posBufferUpto;

    private byte[] payloadBytes;
    private int payloadByteUpto;

    private int lastBlockDocID;
    private long lastBlockPosFP;
    private long lastBlockPayFP;
    private int lastBlockPosBufferUpto;
    private int lastBlockPayloadByteUpto;

    private int lastDocID;
    private int lastPosition;
    private int lastStartOffset;
    private int docCount;

    private final PForUtil pforUtil;
    private final ES812SkipWriter skipWriter;

    private boolean fieldHasNorms;
    private NumericDocValues norms;
    private final CompetitiveImpactAccumulator competitiveFreqNormAccumulator = new CompetitiveImpactAccumulator();

    /** Creates a postings writer */
    ES812PostingsWriter(SegmentWriteState state) throws IOException {

        String docFileName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, ES812PostingsFormat.DOC_EXTENSION);
        docOut = state.directory.createOutput(docFileName, state.context);
        IndexOutput posOut = null;
        IndexOutput payOut = null;
        boolean success = false;
        try {
            CodecUtil.writeIndexHeader(docOut, DOC_CODEC, VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
            pforUtil = new PForUtil();
            if (state.fieldInfos.hasProx()) {
                posDeltaBuffer = new long[BLOCK_SIZE];
                String posFileName = IndexFileNames.segmentFileName(
                    state.segmentInfo.name,
                    state.segmentSuffix,
                    ES812PostingsFormat.POS_EXTENSION
                );
                posOut = state.directory.createOutput(posFileName, state.context);
                CodecUtil.writeIndexHeader(posOut, POS_CODEC, VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);

                if (state.fieldInfos.hasPayloads()) {
                    payloadBytes = new byte[128];
                    payloadLengthBuffer = new long[BLOCK_SIZE];
                } else {
                    payloadBytes = null;
                    payloadLengthBuffer = null;
                }

                if (state.fieldInfos.hasOffsets()) {
                    offsetStartDeltaBuffer = new long[BLOCK_SIZE];
                    offsetLengthBuffer = new long[BLOCK_SIZE];
                } else {
                    offsetStartDeltaBuffer = null;
                    offsetLengthBuffer = null;
                }

                if (state.fieldInfos.hasPayloads() || state.fieldInfos.hasOffsets()) {
                    String payFileName = IndexFileNames.segmentFileName(
                        state.segmentInfo.name,
                        state.segmentSuffix,
                        ES812PostingsFormat.PAY_EXTENSION
                    );
                    payOut = state.directory.createOutput(payFileName, state.context);
                    CodecUtil.writeIndexHeader(payOut, PAY_CODEC, VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
                }
            } else {
                posDeltaBuffer = null;
                payloadLengthBuffer = null;
                offsetStartDeltaBuffer = null;
                offsetLengthBuffer = null;
                payloadBytes = null;
            }
            this.payOut = payOut;
            this.posOut = posOut;
            success = true;
        } finally {
            if (success == false) {
                IOUtils.closeWhileHandlingException(docOut, posOut, payOut);
            }
        }

        docDeltaBuffer = new long[BLOCK_SIZE];
        freqBuffer = new long[BLOCK_SIZE];

        skipWriter = new ES812SkipWriter(MAX_SKIP_LEVELS, BLOCK_SIZE, state.segmentInfo.maxDoc(), docOut, posOut, payOut);
    }

    @Override
    public IntBlockTermState newTermState() {
        return new IntBlockTermState();
    }

    @Override
    public void init(IndexOutput termsOut, SegmentWriteState state) throws IOException {
        CodecUtil.writeIndexHeader(termsOut, TERMS_CODEC, VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
        termsOut.writeVInt(BLOCK_SIZE);
    }

    @Override
    public void setField(FieldInfo fieldInfo) {
        super.setField(fieldInfo);
        skipWriter.setField(writePositions, writeOffsets, writePayloads);
        lastState = emptyState;
        fieldHasNorms = fieldInfo.hasNorms();
    }

    @Override
    public void startTerm(NumericDocValues norms) {
        docStartFP = docOut.getFilePointer();
        if (writePositions) {
            posStartFP = posOut.getFilePointer();
            if (writePayloads || writeOffsets) {
                payStartFP = payOut.getFilePointer();
            }
        }
        lastDocID = 0;
        lastBlockDocID = -1;
        skipWriter.resetSkip();
        this.norms = norms;
        competitiveFreqNormAccumulator.clear();
    }

    @Override
    public void startDoc(int docID, int termDocFreq) throws IOException {
        if (lastBlockDocID != -1 && docBufferUpto == 0) {
            skipWriter.bufferSkip(
                lastBlockDocID,
                competitiveFreqNormAccumulator,
                docCount,
                lastBlockPosFP,
                lastBlockPayFP,
                lastBlockPosBufferUpto,
                lastBlockPayloadByteUpto
            );
            competitiveFreqNormAccumulator.clear();
        }

        final int docDelta = docID - lastDocID;

        if (docID < 0 || (docCount > 0 && docDelta <= 0)) {
            throw new CorruptIndexException("docs out of order (" + docID + " <= " + lastDocID + " )", docOut);
        }

        docDeltaBuffer[docBufferUpto] = docDelta;
        if (writeFreqs) {
            freqBuffer[docBufferUpto] = termDocFreq;
        }

        docBufferUpto++;
        docCount++;

        if (docBufferUpto == BLOCK_SIZE) {
            pforUtil.encode(docDeltaBuffer, docOut);
            if (writeFreqs) {
                pforUtil.encode(freqBuffer, docOut);
            }
        }

        lastDocID = docID;
        lastPosition = 0;
        lastStartOffset = 0;

        long norm;
        if (fieldHasNorms) {
            boolean found = norms.advanceExact(docID);
            if (found == false) {
                norm = 1L;
            } else {
                norm = norms.longValue();
                assert norm != 0 : docID;
            }
        } else {
            norm = 1L;
        }

        competitiveFreqNormAccumulator.add(writeFreqs ? termDocFreq : 1, norm);
    }

    @Override
    public void addPosition(int position, BytesRef payload, int startOffset, int endOffset) throws IOException {
        if (position > IndexWriter.MAX_POSITION) {
            throw new CorruptIndexException(
                "position=" + position + " is too large (> IndexWriter.MAX_POSITION=" + IndexWriter.MAX_POSITION + ")",
                docOut
            );
        }
        if (position < 0) {
            throw new CorruptIndexException("position=" + position + " is < 0", docOut);
        }
        posDeltaBuffer[posBufferUpto] = position - lastPosition;
        if (writePayloads) {
            if (payload == null || payload.length == 0) {
                payloadLengthBuffer[posBufferUpto] = 0;
            } else {
                payloadLengthBuffer[posBufferUpto] = payload.length;
                if (payloadByteUpto + payload.length > payloadBytes.length) {
                    payloadBytes = ArrayUtil.grow(payloadBytes, payloadByteUpto + payload.length);
                }
                System.arraycopy(payload.bytes, payload.offset, payloadBytes, payloadByteUpto, payload.length);
                payloadByteUpto += payload.length;
            }
        }

        if (writeOffsets) {
            assert startOffset >= lastStartOffset;
            assert endOffset >= startOffset;
            offsetStartDeltaBuffer[posBufferUpto] = startOffset - lastStartOffset;
            offsetLengthBuffer[posBufferUpto] = endOffset - startOffset;
            lastStartOffset = startOffset;
        }

        posBufferUpto++;
        lastPosition = position;
        if (posBufferUpto == BLOCK_SIZE) {
            pforUtil.encode(posDeltaBuffer, posOut);

            if (writePayloads) {
                pforUtil.encode(payloadLengthBuffer, payOut);
                payOut.writeVInt(payloadByteUpto);
                payOut.writeBytes(payloadBytes, 0, payloadByteUpto);
                payloadByteUpto = 0;
            }
            if (writeOffsets) {
                pforUtil.encode(offsetStartDeltaBuffer, payOut);
                pforUtil.encode(offsetLengthBuffer, payOut);
            }
            posBufferUpto = 0;
        }
    }

    @Override
    public void finishDoc() throws IOException {
        if (docBufferUpto == BLOCK_SIZE) {
            lastBlockDocID = lastDocID;
            if (posOut != null) {
                if (payOut != null) {
                    lastBlockPayFP = payOut.getFilePointer();
                }
                lastBlockPosFP = posOut.getFilePointer();
                lastBlockPosBufferUpto = posBufferUpto;
                lastBlockPayloadByteUpto = payloadByteUpto;
            }
            docBufferUpto = 0;
        }
    }

    /** Called when we are done adding docs to this term */
    @Override
    public void finishTerm(BlockTermState _state) throws IOException {
        IntBlockTermState state = (IntBlockTermState) _state;
        assert state.docFreq > 0;

        assert state.docFreq == docCount : state.docFreq + " vs " + docCount;

        final int singletonDocID;
        if (state.docFreq == 1) {
            singletonDocID = (int) docDeltaBuffer[0];
        } else {
            singletonDocID = -1;
            for (int i = 0; i < docBufferUpto; i++) {
                final int docDelta = (int) docDeltaBuffer[i];
                final int freq = (int) freqBuffer[i];
                if (writeFreqs == false) {
                    docOut.writeVInt(docDelta);
                } else if (freq == 1) {
                    docOut.writeVInt((docDelta << 1) | 1);
                } else {
                    docOut.writeVInt(docDelta << 1);
                    docOut.writeVInt(freq);
                }
            }
        }

        final long lastPosBlockOffset;

        if (writePositions) {
            assert state.totalTermFreq != -1;
            if (state.totalTermFreq > BLOCK_SIZE) {
                lastPosBlockOffset = posOut.getFilePointer() - posStartFP;
            } else {
                lastPosBlockOffset = -1;
            }
            if (posBufferUpto > 0) {

                int lastPayloadLength = -1; 
                int lastOffsetLength = -1; 
                int payloadBytesReadUpto = 0;
                for (int i = 0; i < posBufferUpto; i++) {
                    final int posDelta = (int) posDeltaBuffer[i];
                    if (writePayloads) {
                        final int payloadLength = (int) payloadLengthBuffer[i];
                        if (payloadLength != lastPayloadLength) {
                            lastPayloadLength = payloadLength;
                            posOut.writeVInt((posDelta << 1) | 1);
                            posOut.writeVInt(payloadLength);
                        } else {
                            posOut.writeVInt(posDelta << 1);
                        }

                        if (payloadLength != 0) {
                            posOut.writeBytes(payloadBytes, payloadBytesReadUpto, payloadLength);
                            payloadBytesReadUpto += payloadLength;
                        }
                    } else {
                        posOut.writeVInt(posDelta);
                    }

                    if (writeOffsets) {
                        int delta = (int) offsetStartDeltaBuffer[i];
                        int length = (int) offsetLengthBuffer[i];
                        if (length == lastOffsetLength) {
                            posOut.writeVInt(delta << 1);
                        } else {
                            posOut.writeVInt(delta << 1 | 1);
                            posOut.writeVInt(length);
                            lastOffsetLength = length;
                        }
                    }
                }

                if (writePayloads) {
                    assert payloadBytesReadUpto == payloadByteUpto;
                    payloadByteUpto = 0;
                }
            }
        } else {
            lastPosBlockOffset = -1;
        }

        long skipOffset;
        if (docCount > BLOCK_SIZE) {
            skipOffset = skipWriter.writeSkip(docOut) - docStartFP;
        } else {
            skipOffset = -1;
        }

        state.docStartFP = docStartFP;
        state.posStartFP = posStartFP;
        state.payStartFP = payStartFP;
        state.singletonDocID = singletonDocID;
        state.skipOffset = skipOffset;
        state.lastPosBlockOffset = lastPosBlockOffset;
        docBufferUpto = 0;
        posBufferUpto = 0;
        lastDocID = 0;
        docCount = 0;
    }

    @Override
    public void encodeTerm(DataOutput out, FieldInfo fieldInfo, BlockTermState _state, boolean absolute) throws IOException {
        IntBlockTermState state = (IntBlockTermState) _state;
        if (absolute) {
            lastState = emptyState;
            assert lastState.docStartFP == 0;
        }

        if (lastState.singletonDocID != -1 && state.singletonDocID != -1 && state.docStartFP == lastState.docStartFP) {
            final long delta = (long) state.singletonDocID - lastState.singletonDocID;
            out.writeVLong((BitUtil.zigZagEncode(delta) << 1) | 0x01);
        } else {
            out.writeVLong((state.docStartFP - lastState.docStartFP) << 1);
            if (state.singletonDocID != -1) {
                out.writeVInt(state.singletonDocID);
            }
        }

        if (writePositions) {
            out.writeVLong(state.posStartFP - lastState.posStartFP);
            if (writePayloads || writeOffsets) {
                out.writeVLong(state.payStartFP - lastState.payStartFP);
            }
        }
        if (writePositions) {
            if (state.lastPosBlockOffset != -1) {
                out.writeVLong(state.lastPosBlockOffset);
            }
        }
        if (state.skipOffset != -1) {
            out.writeVLong(state.skipOffset);
        }
        lastState = state;
    }

    @Override
    public void close() throws IOException {
        boolean success = false;
        try {
            if (docOut != null) {
                CodecUtil.writeFooter(docOut);
            }
            if (posOut != null) {
                CodecUtil.writeFooter(posOut);
            }
            if (payOut != null) {
                CodecUtil.writeFooter(payOut);
            }
            success = true;
        } finally {
            if (success) {
                IOUtils.close(docOut, posOut, payOut);
            } else {
                IOUtils.closeWhileHandlingException(docOut, posOut, payOut);
            }
            docOut = posOut = payOut = null;
        }
    }
}
