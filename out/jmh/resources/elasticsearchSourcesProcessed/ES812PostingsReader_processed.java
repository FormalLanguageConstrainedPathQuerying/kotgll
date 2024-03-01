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
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.Impacts;
import org.apache.lucene.index.ImpactsEnum;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SlowImpactsEnum;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BitUtil;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.core.IOUtils;
import org.elasticsearch.index.codec.postings.ES812PostingsFormat.IntBlockTermState;

import java.io.IOException;
import java.util.Arrays;

import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.DOC_CODEC;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.MAX_SKIP_LEVELS;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.PAY_CODEC;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.POS_CODEC;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.TERMS_CODEC;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.VERSION_CURRENT;
import static org.elasticsearch.index.codec.postings.ES812PostingsFormat.VERSION_START;
import static org.elasticsearch.index.codec.postings.ForUtil.BLOCK_SIZE;

/**
 * Concrete class that reads docId(maybe frq,pos,offset,payloads) list with postings format.
 *
 */
final class ES812PostingsReader extends PostingsReaderBase {

    private final IndexInput docIn;
    private final IndexInput posIn;
    private final IndexInput payIn;

    private final int version;

    /** Sole constructor. */
    ES812PostingsReader(SegmentReadState state) throws IOException {
        boolean success = false;
        IndexInput docIn = null;
        IndexInput posIn = null;
        IndexInput payIn = null;


        String docName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, ES812PostingsFormat.DOC_EXTENSION);
        try {
            docIn = state.directory.openInput(docName, state.context);
            version = CodecUtil.checkIndexHeader(
                docIn,
                DOC_CODEC,
                VERSION_START,
                VERSION_CURRENT,
                state.segmentInfo.getId(),
                state.segmentSuffix
            );
            CodecUtil.retrieveChecksum(docIn);

            if (state.fieldInfos.hasProx()) {
                String proxName = IndexFileNames.segmentFileName(
                    state.segmentInfo.name,
                    state.segmentSuffix,
                    ES812PostingsFormat.POS_EXTENSION
                );
                posIn = state.directory.openInput(proxName, state.context);
                CodecUtil.checkIndexHeader(posIn, POS_CODEC, version, version, state.segmentInfo.getId(), state.segmentSuffix);
                CodecUtil.retrieveChecksum(posIn);

                if (state.fieldInfos.hasPayloads() || state.fieldInfos.hasOffsets()) {
                    String payName = IndexFileNames.segmentFileName(
                        state.segmentInfo.name,
                        state.segmentSuffix,
                        ES812PostingsFormat.PAY_EXTENSION
                    );
                    payIn = state.directory.openInput(payName, state.context);
                    CodecUtil.checkIndexHeader(payIn, PAY_CODEC, version, version, state.segmentInfo.getId(), state.segmentSuffix);
                    CodecUtil.retrieveChecksum(payIn);
                }
            }

            this.docIn = docIn;
            this.posIn = posIn;
            this.payIn = payIn;
            success = true;
        } finally {
            if (success == false) {
                IOUtils.closeWhileHandlingException(docIn, posIn, payIn);
            }
        }
    }

    @Override
    public void init(IndexInput termsIn, SegmentReadState state) throws IOException {
        CodecUtil.checkIndexHeader(termsIn, TERMS_CODEC, VERSION_START, VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
        final int indexBlockSize = termsIn.readVInt();
        if (indexBlockSize != BLOCK_SIZE) {
            throw new IllegalStateException("index-time BLOCK_SIZE (" + indexBlockSize + ") != read-time BLOCK_SIZE (" + BLOCK_SIZE + ")");
        }
    }

    /** Read values that have been written using variable-length encoding instead of bit-packing. */
    static void readVIntBlock(IndexInput docIn, long[] docBuffer, long[] freqBuffer, int num, boolean indexHasFreq) throws IOException {
        if (indexHasFreq) {
            for (int i = 0; i < num; i++) {
                final int code = docIn.readVInt();
                docBuffer[i] = code >>> 1;
                if ((code & 1) != 0) {
                    freqBuffer[i] = 1;
                } else {
                    freqBuffer[i] = docIn.readVInt();
                }
            }
        } else {
            for (int i = 0; i < num; i++) {
                docBuffer[i] = docIn.readVInt();
            }
        }
    }

    static void prefixSum(long[] buffer, int count, long base) {
        buffer[0] += base;
        for (int i = 1; i < count; ++i) {
            buffer[i] += buffer[i - 1];
        }
    }

    static int findFirstGreater(long[] buffer, int target, int from) {
        for (int i = from; i < BLOCK_SIZE; ++i) {
            if (buffer[i] >= target) {
                return i;
            }
        }
        return BLOCK_SIZE;
    }

    @Override
    public BlockTermState newTermState() {
        return new IntBlockTermState();
    }

    @Override
    public void close() throws IOException {
        IOUtils.close(docIn, posIn, payIn);
    }

    @Override
    public void decodeTerm(DataInput in, FieldInfo fieldInfo, BlockTermState _termState, boolean absolute) throws IOException {
        final IntBlockTermState termState = (IntBlockTermState) _termState;
        final boolean fieldHasPositions = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
        final boolean fieldHasOffsets = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
        final boolean fieldHasPayloads = fieldInfo.hasPayloads();

        if (absolute) {
            termState.docStartFP = 0;
            termState.posStartFP = 0;
            termState.payStartFP = 0;
        }

        final long l = in.readVLong();
        if ((l & 0x01) == 0) {
            termState.docStartFP += l >>> 1;
            if (termState.docFreq == 1) {
                termState.singletonDocID = in.readVInt();
            } else {
                termState.singletonDocID = -1;
            }
        } else {
            assert absolute == false;
            assert termState.singletonDocID != -1;
            termState.singletonDocID += (int) BitUtil.zigZagDecode(l >>> 1);
        }

        if (fieldHasPositions) {
            termState.posStartFP += in.readVLong();
            if (fieldHasOffsets || fieldHasPayloads) {
                termState.payStartFP += in.readVLong();
            }
            if (termState.totalTermFreq > BLOCK_SIZE) {
                termState.lastPosBlockOffset = in.readVLong();
            } else {
                termState.lastPosBlockOffset = -1;
            }
        }

        if (termState.docFreq > BLOCK_SIZE) {
            termState.skipOffset = in.readVLong();
        } else {
            termState.skipOffset = -1;
        }
    }

    @Override
    public PostingsEnum postings(FieldInfo fieldInfo, BlockTermState termState, PostingsEnum reuse, int flags) throws IOException {

        boolean indexHasPositions = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;

        if (indexHasPositions == false || PostingsEnum.featureRequested(flags, PostingsEnum.POSITIONS) == false) {
            BlockDocsEnum docsEnum;
            if (reuse instanceof BlockDocsEnum) {
                docsEnum = (BlockDocsEnum) reuse;
                if (docsEnum.canReuse(docIn, fieldInfo) == false) {
                    docsEnum = new BlockDocsEnum(fieldInfo);
                }
            } else {
                docsEnum = new BlockDocsEnum(fieldInfo);
            }
            return docsEnum.reset((IntBlockTermState) termState, flags);
        } else {
            EverythingEnum everythingEnum;
            if (reuse instanceof EverythingEnum) {
                everythingEnum = (EverythingEnum) reuse;
                if (everythingEnum.canReuse(docIn, fieldInfo) == false) {
                    everythingEnum = new EverythingEnum(fieldInfo);
                }
            } else {
                everythingEnum = new EverythingEnum(fieldInfo);
            }
            return everythingEnum.reset((IntBlockTermState) termState, flags);
        }
    }

    @Override
    public ImpactsEnum impacts(FieldInfo fieldInfo, BlockTermState state, int flags) throws IOException {
        if (state.docFreq <= BLOCK_SIZE) {
            return new SlowImpactsEnum(postings(fieldInfo, state, null, flags));
        }

        final boolean indexHasPositions = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
        final boolean indexHasOffsets = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
        final boolean indexHasPayloads = fieldInfo.hasPayloads();

        if (indexHasPositions == false || PostingsEnum.featureRequested(flags, PostingsEnum.POSITIONS) == false) {
            return new BlockImpactsDocsEnum(fieldInfo, (IntBlockTermState) state);
        }

        if (indexHasPositions
            && PostingsEnum.featureRequested(flags, PostingsEnum.POSITIONS)
            && (indexHasOffsets == false || PostingsEnum.featureRequested(flags, PostingsEnum.OFFSETS) == false)
            && (indexHasPayloads == false || PostingsEnum.featureRequested(flags, PostingsEnum.PAYLOADS) == false)) {
            return new BlockImpactsPostingsEnum(fieldInfo, (IntBlockTermState) state);
        }

        return new BlockImpactsEverythingEnum(fieldInfo, (IntBlockTermState) state, flags);
    }

    final class BlockDocsEnum extends PostingsEnum {

        final PForUtil pforUtil = new PForUtil(new ForUtil());

        private final long[] docBuffer = new long[BLOCK_SIZE + 1];
        private final long[] freqBuffer = new long[BLOCK_SIZE];

        private int docBufferUpto;

        private ES812SkipReader skipper;
        private boolean skipped;

        final IndexInput startDocIn;

        IndexInput docIn;
        final boolean indexHasFreq;
        final boolean indexHasPos;
        final boolean indexHasOffsets;
        final boolean indexHasPayloads;

        private int docFreq; 
        private long totalTermFreq; 
        private int blockUpto; 
        private int doc; 
        private long accum; 

        private long docTermStartFP;

        private long skipOffset;

        private int nextSkipDoc;

        private boolean needsFreq; 
        private boolean isFreqsRead;
        private int singletonDocID; 

        BlockDocsEnum(FieldInfo fieldInfo) throws IOException {
            this.startDocIn = ES812PostingsReader.this.docIn;
            this.docIn = null;
            indexHasFreq = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
            indexHasPos = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
            indexHasOffsets = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
            indexHasPayloads = fieldInfo.hasPayloads();
            docBuffer[BLOCK_SIZE] = NO_MORE_DOCS;
        }

        public boolean canReuse(IndexInput docIn, FieldInfo fieldInfo) {
            return docIn == startDocIn
                && indexHasFreq == (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0)
                && indexHasPos == (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0)
                && indexHasPayloads == fieldInfo.hasPayloads();
        }

        public PostingsEnum reset(IntBlockTermState termState, int flags) throws IOException {
            docFreq = termState.docFreq;
            totalTermFreq = indexHasFreq ? termState.totalTermFreq : docFreq;
            docTermStartFP = termState.docStartFP;
            skipOffset = termState.skipOffset;
            singletonDocID = termState.singletonDocID;
            if (docFreq > 1) {
                if (docIn == null) {
                    docIn = startDocIn.clone();
                }
                docIn.seek(docTermStartFP);
            }

            doc = -1;
            this.needsFreq = PostingsEnum.featureRequested(flags, PostingsEnum.FREQS);
            this.isFreqsRead = true;
            if (indexHasFreq == false || needsFreq == false) {
                for (int i = 0; i < ForUtil.BLOCK_SIZE; ++i) {
                    freqBuffer[i] = 1;
                }
            }
            accum = 0;
            blockUpto = 0;
            nextSkipDoc = BLOCK_SIZE - 1; 
            docBufferUpto = BLOCK_SIZE;
            skipped = false;
            return this;
        }

        @Override
        public int freq() throws IOException {
            if (isFreqsRead == false) {
                pforUtil.decode(docIn, freqBuffer); 
                isFreqsRead = true;
            }
            return (int) freqBuffer[docBufferUpto - 1];
        }

        @Override
        public int nextPosition() throws IOException {
            return -1;
        }

        @Override
        public int startOffset() throws IOException {
            return -1;
        }

        @Override
        public int endOffset() throws IOException {
            return -1;
        }

        @Override
        public BytesRef getPayload() throws IOException {
            return null;
        }

        @Override
        public int docID() {
            return doc;
        }

        private void refillDocs() throws IOException {
            if (isFreqsRead == false) {
                pforUtil.skip(docIn);
                isFreqsRead = true;
            }

            final int left = docFreq - blockUpto;
            assert left >= 0;

            if (left >= BLOCK_SIZE) {
                pforUtil.decodeAndPrefixSum(docIn, accum, docBuffer);

                if (indexHasFreq) {
                    if (needsFreq) {
                        isFreqsRead = false;
                    } else {
                        pforUtil.skip(docIn); 
                    }
                }
                blockUpto += BLOCK_SIZE;
            } else if (docFreq == 1) {
                docBuffer[0] = singletonDocID;
                freqBuffer[0] = totalTermFreq;
                docBuffer[1] = NO_MORE_DOCS;
                blockUpto++;
            } else {
                readVIntBlock(docIn, docBuffer, freqBuffer, left, indexHasFreq);
                prefixSum(docBuffer, left, accum);
                docBuffer[left] = NO_MORE_DOCS;
                blockUpto += left;
            }
            accum = docBuffer[BLOCK_SIZE - 1];
            docBufferUpto = 0;
            assert docBuffer[BLOCK_SIZE] == NO_MORE_DOCS;
        }

        @Override
        public int nextDoc() throws IOException {
            if (docBufferUpto == BLOCK_SIZE) {
                refillDocs(); 
            }

            doc = (int) docBuffer[docBufferUpto];
            docBufferUpto++;
            return doc;
        }

        @Override
        public int advance(int target) throws IOException {
            if (docFreq > BLOCK_SIZE && target > nextSkipDoc) {

                if (skipper == null) {
                    skipper = new ES812SkipReader(docIn.clone(), MAX_SKIP_LEVELS, indexHasPos, indexHasOffsets, indexHasPayloads);
                }

                if (skipped == false) {
                    assert skipOffset != -1;
                    skipper.init(docTermStartFP + skipOffset, docTermStartFP, 0, 0, docFreq);
                    skipped = true;
                }

                final int newDocUpto = skipper.skipTo(target) + 1;

                if (newDocUpto >= blockUpto) {
                    assert newDocUpto % BLOCK_SIZE == 0 : "got " + newDocUpto;
                    blockUpto = newDocUpto;

                    docBufferUpto = BLOCK_SIZE;
                    accum = skipper.getDoc(); 
                    docIn.seek(skipper.getDocPointer()); 
                    isFreqsRead = true;
                }
                nextSkipDoc = skipper.getNextSkipDoc();
            }
            if (docBufferUpto == BLOCK_SIZE) {
                refillDocs();
            }

            long doc;
            while (true) {
                doc = docBuffer[docBufferUpto];

                if (doc >= target) {
                    break;
                }
                ++docBufferUpto;
            }

            docBufferUpto++;
            return this.doc = (int) doc;
        }

        @Override
        public long cost() {
            return docFreq;
        }
    }

    final class EverythingEnum extends PostingsEnum {

        final PForUtil pforUtil = new PForUtil(new ForUtil());

        private final long[] docBuffer = new long[BLOCK_SIZE + 1];
        private final long[] freqBuffer = new long[BLOCK_SIZE + 1];
        private final long[] posDeltaBuffer = new long[BLOCK_SIZE];

        private final long[] payloadLengthBuffer;
        private final long[] offsetStartDeltaBuffer;
        private final long[] offsetLengthBuffer;

        private byte[] payloadBytes;
        private int payloadByteUpto;
        private int payloadLength;

        private int lastStartOffset;
        private int startOffset;
        private int endOffset;

        private int docBufferUpto;
        private int posBufferUpto;

        private ES812SkipReader skipper;
        private boolean skipped;

        final IndexInput startDocIn;

        IndexInput docIn;
        final IndexInput posIn;
        final IndexInput payIn;
        final BytesRef payload;

        final boolean indexHasOffsets;
        final boolean indexHasPayloads;

        private int docFreq; 
        private long totalTermFreq; 
        private int blockUpto; 
        private int doc; 
        private long accum; 
        private int freq; 
        private int position; 

        private int posPendingCount;

        private long posPendingFP;

        private long payPendingFP;

        private long docTermStartFP;

        private long posTermStartFP;

        private long payTermStartFP;

        private long lastPosBlockFP;

        private long skipOffset;

        private int nextSkipDoc;

        private boolean needsOffsets; 
        private boolean needsPayloads; 
        private int singletonDocID; 

        EverythingEnum(FieldInfo fieldInfo) throws IOException {
            indexHasOffsets = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
            indexHasPayloads = fieldInfo.hasPayloads();

            this.startDocIn = ES812PostingsReader.this.docIn;
            this.docIn = null;
            this.posIn = ES812PostingsReader.this.posIn.clone();
            if (indexHasOffsets || indexHasPayloads) {
                this.payIn = ES812PostingsReader.this.payIn.clone();
            } else {
                this.payIn = null;
            }
            if (indexHasOffsets) {
                offsetStartDeltaBuffer = new long[BLOCK_SIZE];
                offsetLengthBuffer = new long[BLOCK_SIZE];
            } else {
                offsetStartDeltaBuffer = null;
                offsetLengthBuffer = null;
                startOffset = -1;
                endOffset = -1;
            }

            if (indexHasPayloads) {
                payloadLengthBuffer = new long[BLOCK_SIZE];
                payloadBytes = new byte[128];
                payload = new BytesRef();
            } else {
                payloadLengthBuffer = null;
                payloadBytes = null;
                payload = null;
            }

            docBuffer[BLOCK_SIZE] = NO_MORE_DOCS;
        }

        public boolean canReuse(IndexInput docIn, FieldInfo fieldInfo) {
            return docIn == startDocIn
                && indexHasOffsets == (fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0)
                && indexHasPayloads == fieldInfo.hasPayloads();
        }

        public EverythingEnum reset(IntBlockTermState termState, int flags) throws IOException {
            docFreq = termState.docFreq;
            docTermStartFP = termState.docStartFP;
            posTermStartFP = termState.posStartFP;
            payTermStartFP = termState.payStartFP;
            skipOffset = termState.skipOffset;
            totalTermFreq = termState.totalTermFreq;
            singletonDocID = termState.singletonDocID;
            if (docFreq > 1) {
                if (docIn == null) {
                    docIn = startDocIn.clone();
                }
                docIn.seek(docTermStartFP);
            }
            posPendingFP = posTermStartFP;
            payPendingFP = payTermStartFP;
            posPendingCount = 0;
            if (termState.totalTermFreq < BLOCK_SIZE) {
                lastPosBlockFP = posTermStartFP;
            } else if (termState.totalTermFreq == BLOCK_SIZE) {
                lastPosBlockFP = -1;
            } else {
                lastPosBlockFP = posTermStartFP + termState.lastPosBlockOffset;
            }

            this.needsOffsets = PostingsEnum.featureRequested(flags, PostingsEnum.OFFSETS);
            this.needsPayloads = PostingsEnum.featureRequested(flags, PostingsEnum.PAYLOADS);

            doc = -1;
            accum = 0;
            blockUpto = 0;
            if (docFreq > BLOCK_SIZE) {
                nextSkipDoc = BLOCK_SIZE - 1; 
            } else {
                nextSkipDoc = NO_MORE_DOCS; 
            }
            docBufferUpto = BLOCK_SIZE;
            skipped = false;
            return this;
        }

        @Override
        public int freq() throws IOException {
            return freq;
        }

        @Override
        public int docID() {
            return doc;
        }

        private void refillDocs() throws IOException {
            final int left = docFreq - blockUpto;
            assert left >= 0;

            if (left >= BLOCK_SIZE) {
                pforUtil.decodeAndPrefixSum(docIn, accum, docBuffer);
                pforUtil.decode(docIn, freqBuffer);
                blockUpto += BLOCK_SIZE;
            } else if (docFreq == 1) {
                docBuffer[0] = singletonDocID;
                freqBuffer[0] = totalTermFreq;
                docBuffer[1] = NO_MORE_DOCS;
                blockUpto++;
            } else {
                readVIntBlock(docIn, docBuffer, freqBuffer, left, true);
                prefixSum(docBuffer, left, accum);
                docBuffer[left] = NO_MORE_DOCS;
                blockUpto += left;
            }
            accum = docBuffer[BLOCK_SIZE - 1];
            docBufferUpto = 0;
            assert docBuffer[BLOCK_SIZE] == NO_MORE_DOCS;
        }

        private void refillPositions() throws IOException {
            if (posIn.getFilePointer() == lastPosBlockFP) {
                final int count = (int) (totalTermFreq % BLOCK_SIZE);
                int payloadLength = 0;
                int offsetLength = 0;
                payloadByteUpto = 0;
                for (int i = 0; i < count; i++) {
                    int code = posIn.readVInt();
                    if (indexHasPayloads) {
                        if ((code & 1) != 0) {
                            payloadLength = posIn.readVInt();
                        }
                        payloadLengthBuffer[i] = payloadLength;
                        posDeltaBuffer[i] = code >>> 1;
                        if (payloadLength != 0) {
                            if (payloadByteUpto + payloadLength > payloadBytes.length) {
                                payloadBytes = ArrayUtil.grow(payloadBytes, payloadByteUpto + payloadLength);
                            }
                            posIn.readBytes(payloadBytes, payloadByteUpto, payloadLength);
                            payloadByteUpto += payloadLength;
                        }
                    } else {
                        posDeltaBuffer[i] = code;
                    }

                    if (indexHasOffsets) {
                        int deltaCode = posIn.readVInt();
                        if ((deltaCode & 1) != 0) {
                            offsetLength = posIn.readVInt();
                        }
                        offsetStartDeltaBuffer[i] = deltaCode >>> 1;
                        offsetLengthBuffer[i] = offsetLength;
                    }
                }
                payloadByteUpto = 0;
            } else {
                pforUtil.decode(posIn, posDeltaBuffer);

                if (indexHasPayloads) {
                    if (needsPayloads) {
                        pforUtil.decode(payIn, payloadLengthBuffer);
                        int numBytes = payIn.readVInt();

                        if (numBytes > payloadBytes.length) {
                            payloadBytes = ArrayUtil.growNoCopy(payloadBytes, numBytes);
                        }
                        payIn.readBytes(payloadBytes, 0, numBytes);
                    } else {
                        pforUtil.skip(payIn); 
                        int numBytes = payIn.readVInt(); 
                        payIn.seek(payIn.getFilePointer() + numBytes); 
                    }
                    payloadByteUpto = 0;
                }

                if (indexHasOffsets) {
                    if (needsOffsets) {
                        pforUtil.decode(payIn, offsetStartDeltaBuffer);
                        pforUtil.decode(payIn, offsetLengthBuffer);
                    } else {
                        pforUtil.skip(payIn); 
                        pforUtil.skip(payIn); 
                    }
                }
            }
        }

        @Override
        public int nextDoc() throws IOException {
            if (docBufferUpto == BLOCK_SIZE) {
                refillDocs();
            }

            doc = (int) docBuffer[docBufferUpto];
            freq = (int) freqBuffer[docBufferUpto];
            posPendingCount += freq;
            docBufferUpto++;

            position = 0;
            lastStartOffset = 0;
            return doc;
        }

        @Override
        public int advance(int target) throws IOException {
            if (target > nextSkipDoc) {
                if (skipper == null) {
                    skipper = new ES812SkipReader(docIn.clone(), MAX_SKIP_LEVELS, true, indexHasOffsets, indexHasPayloads);
                }

                if (skipped == false) {
                    assert skipOffset != -1;
                    skipper.init(docTermStartFP + skipOffset, docTermStartFP, posTermStartFP, payTermStartFP, docFreq);
                    skipped = true;
                }

                final int newDocUpto = skipper.skipTo(target) + 1;

                if (newDocUpto > blockUpto - BLOCK_SIZE + docBufferUpto) {
                    assert newDocUpto % BLOCK_SIZE == 0 : "got " + newDocUpto;
                    blockUpto = newDocUpto;

                    docBufferUpto = BLOCK_SIZE;
                    accum = skipper.getDoc();
                    docIn.seek(skipper.getDocPointer());
                    posPendingFP = skipper.getPosPointer();
                    payPendingFP = skipper.getPayPointer();
                    posPendingCount = skipper.getPosBufferUpto();
                    lastStartOffset = 0; 
                    payloadByteUpto = skipper.getPayloadByteUpto();
                }
                nextSkipDoc = skipper.getNextSkipDoc();
            }
            if (docBufferUpto == BLOCK_SIZE) {
                refillDocs();
            }

            long doc;
            while (true) {
                doc = docBuffer[docBufferUpto];
                freq = (int) freqBuffer[docBufferUpto];
                posPendingCount += freq;
                docBufferUpto++;

                if (doc >= target) {
                    break;
                }
            }

            position = 0;
            lastStartOffset = 0;
            return this.doc = (int) doc;
        }

        private void skipPositions() throws IOException {
            int toSkip = posPendingCount - freq;

            final int leftInBlock = BLOCK_SIZE - posBufferUpto;
            if (toSkip < leftInBlock) {
                int end = posBufferUpto + toSkip;
                while (posBufferUpto < end) {
                    if (indexHasPayloads) {
                        payloadByteUpto += (int) payloadLengthBuffer[posBufferUpto];
                    }
                    posBufferUpto++;
                }
            } else {
                toSkip -= leftInBlock;
                while (toSkip >= BLOCK_SIZE) {
                    assert posIn.getFilePointer() != lastPosBlockFP;
                    pforUtil.skip(posIn);

                    if (indexHasPayloads) {
                        pforUtil.skip(payIn);

                        int numBytes = payIn.readVInt();
                        payIn.seek(payIn.getFilePointer() + numBytes);
                    }

                    if (indexHasOffsets) {
                        pforUtil.skip(payIn);
                        pforUtil.skip(payIn);
                    }
                    toSkip -= BLOCK_SIZE;
                }
                refillPositions();
                payloadByteUpto = 0;
                posBufferUpto = 0;
                while (posBufferUpto < toSkip) {
                    if (indexHasPayloads) {
                        payloadByteUpto += (int) payloadLengthBuffer[posBufferUpto];
                    }
                    posBufferUpto++;
                }
            }

            position = 0;
            lastStartOffset = 0;
        }

        @Override
        public int nextPosition() throws IOException {
            assert posPendingCount > 0;

            if (posPendingFP != -1) {
                posIn.seek(posPendingFP);
                posPendingFP = -1;

                if (payPendingFP != -1 && payIn != null) {
                    payIn.seek(payPendingFP);
                    payPendingFP = -1;
                }

                posBufferUpto = BLOCK_SIZE;
            }

            if (posPendingCount > freq) {
                skipPositions();
                posPendingCount = freq;
            }

            if (posBufferUpto == BLOCK_SIZE) {
                refillPositions();
                posBufferUpto = 0;
            }
            position += (int) posDeltaBuffer[posBufferUpto];

            if (indexHasPayloads) {
                payloadLength = (int) payloadLengthBuffer[posBufferUpto];
                payload.bytes = payloadBytes;
                payload.offset = payloadByteUpto;
                payload.length = payloadLength;
                payloadByteUpto += payloadLength;
            }

            if (indexHasOffsets) {
                startOffset = lastStartOffset + (int) offsetStartDeltaBuffer[posBufferUpto];
                endOffset = startOffset + (int) offsetLengthBuffer[posBufferUpto];
                lastStartOffset = startOffset;
            }

            posBufferUpto++;
            posPendingCount--;
            return position;
        }

        @Override
        public int startOffset() {
            return startOffset;
        }

        @Override
        public int endOffset() {
            return endOffset;
        }

        @Override
        public BytesRef getPayload() {
            if (payloadLength == 0) {
                return null;
            } else {
                return payload;
            }
        }

        @Override
        public long cost() {
            return docFreq;
        }
    }

    final class BlockImpactsDocsEnum extends ImpactsEnum {

        final PForUtil pforUtil = new PForUtil(new ForUtil());

        private final long[] docBuffer = new long[BLOCK_SIZE + 1];
        private final long[] freqBuffer = new long[BLOCK_SIZE];

        private int docBufferUpto;

        private final ES812ScoreSkipReader skipper;

        final IndexInput docIn;

        final boolean indexHasFreqs;

        private int docFreq; 
        private int blockUpto; 
        private int doc; 
        private long accum; 

        private int nextSkipDoc = -1;

        private long seekTo = -1;

        private boolean isFreqsRead;

        BlockImpactsDocsEnum(FieldInfo fieldInfo, IntBlockTermState termState) throws IOException {
            indexHasFreqs = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
            final boolean indexHasPositions = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
            final boolean indexHasOffsets = fieldInfo.getIndexOptions()
                .compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
            final boolean indexHasPayloads = fieldInfo.hasPayloads();

            this.docIn = ES812PostingsReader.this.docIn.clone();

            docFreq = termState.docFreq;
            docIn.seek(termState.docStartFP);

            doc = -1;
            accum = 0;
            blockUpto = 0;
            docBufferUpto = BLOCK_SIZE;

            skipper = new ES812ScoreSkipReader(docIn.clone(), MAX_SKIP_LEVELS, indexHasPositions, indexHasOffsets, indexHasPayloads);
            skipper.init(
                termState.docStartFP + termState.skipOffset,
                termState.docStartFP,
                termState.posStartFP,
                termState.payStartFP,
                docFreq
            );

            docBuffer[BLOCK_SIZE] = NO_MORE_DOCS;
            this.isFreqsRead = true;
            if (indexHasFreqs == false) {
                Arrays.fill(freqBuffer, 1L);
            }
        }

        @Override
        public int freq() throws IOException {
            if (isFreqsRead == false) {
                pforUtil.decode(docIn, freqBuffer); 
                isFreqsRead = true;
            }
            return (int) freqBuffer[docBufferUpto - 1];
        }

        @Override
        public int docID() {
            return doc;
        }

        private void refillDocs() throws IOException {
            if (isFreqsRead == false) {
                pforUtil.skip(docIn);
                isFreqsRead = true;
            }

            final int left = docFreq - blockUpto;
            assert left >= 0;

            if (left >= BLOCK_SIZE) {
                pforUtil.decodeAndPrefixSum(docIn, accum, docBuffer);
                if (indexHasFreqs) {
                    isFreqsRead = false;
                }
                blockUpto += BLOCK_SIZE;
            } else {
                readVIntBlock(docIn, docBuffer, freqBuffer, left, indexHasFreqs);
                prefixSum(docBuffer, left, accum);
                docBuffer[left] = NO_MORE_DOCS;
                blockUpto += left;
            }
            accum = docBuffer[BLOCK_SIZE - 1];
            docBufferUpto = 0;
            assert docBuffer[BLOCK_SIZE] == NO_MORE_DOCS;
        }

        @Override
        public void advanceShallow(int target) throws IOException {
            if (target > nextSkipDoc) {
                final int newDocUpto = skipper.skipTo(target) + 1;

                if (newDocUpto >= blockUpto) {
                    assert newDocUpto % BLOCK_SIZE == 0 : "got " + newDocUpto;
                    blockUpto = newDocUpto;

                    docBufferUpto = BLOCK_SIZE;
                    accum = skipper.getDoc();
                    seekTo = skipper.getDocPointer(); 
                }
                nextSkipDoc = skipper.getNextSkipDoc();
            }
            assert nextSkipDoc >= target;
        }

        @Override
        public Impacts getImpacts() throws IOException {
            advanceShallow(doc);
            return skipper.getImpacts();
        }

        @Override
        public int nextDoc() throws IOException {
            if (docBufferUpto == BLOCK_SIZE) {
                if (seekTo >= 0) {
                    docIn.seek(seekTo);
                    isFreqsRead = true; 
                    seekTo = -1;
                }
                refillDocs();
            }
            return this.doc = (int) docBuffer[docBufferUpto++];
        }

        @Override
        public int advance(int target) throws IOException {
            if (target > nextSkipDoc) {
                advanceShallow(target);
            }
            if (docBufferUpto == BLOCK_SIZE) {
                if (seekTo >= 0) {
                    docIn.seek(seekTo);
                    isFreqsRead = true; 
                    seekTo = -1;
                }
                refillDocs();
            }

            int next = findFirstGreater(docBuffer, target, docBufferUpto);
            this.doc = (int) docBuffer[next];
            docBufferUpto = next + 1;
            return doc;
        }

        @Override
        public int nextPosition() throws IOException {
            return -1;
        }

        @Override
        public int startOffset() {
            return -1;
        }

        @Override
        public int endOffset() {
            return -1;
        }

        @Override
        public BytesRef getPayload() {
            return null;
        }

        @Override
        public long cost() {
            return docFreq;
        }
    }

    final class BlockImpactsPostingsEnum extends ImpactsEnum {

        final PForUtil pforUtil = new PForUtil(new ForUtil());

        private final long[] docBuffer = new long[BLOCK_SIZE];
        private final long[] freqBuffer = new long[BLOCK_SIZE];
        private final long[] posDeltaBuffer = new long[BLOCK_SIZE];

        private int docBufferUpto;
        private int posBufferUpto;

        private final ES812ScoreSkipReader skipper;

        final IndexInput docIn;
        final IndexInput posIn;

        final boolean indexHasOffsets;
        final boolean indexHasPayloads;

        private int docFreq; 
        private long totalTermFreq; 
        private int docUpto; 
        private int doc; 
        private long accum; 
        private int freq; 
        private int position; 

        private int posPendingCount;

        private long posPendingFP;

        private long docTermStartFP;

        private long posTermStartFP;

        private long payTermStartFP;

        private long lastPosBlockFP;

        private int nextSkipDoc = -1;

        private long seekTo = -1;

        BlockImpactsPostingsEnum(FieldInfo fieldInfo, IntBlockTermState termState) throws IOException {
            indexHasOffsets = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
            indexHasPayloads = fieldInfo.hasPayloads();

            this.docIn = ES812PostingsReader.this.docIn.clone();

            this.posIn = ES812PostingsReader.this.posIn.clone();

            docFreq = termState.docFreq;
            docTermStartFP = termState.docStartFP;
            posTermStartFP = termState.posStartFP;
            payTermStartFP = termState.payStartFP;
            totalTermFreq = termState.totalTermFreq;
            docIn.seek(docTermStartFP);
            posPendingFP = posTermStartFP;
            posPendingCount = 0;
            if (termState.totalTermFreq < BLOCK_SIZE) {
                lastPosBlockFP = posTermStartFP;
            } else if (termState.totalTermFreq == BLOCK_SIZE) {
                lastPosBlockFP = -1;
            } else {
                lastPosBlockFP = posTermStartFP + termState.lastPosBlockOffset;
            }

            doc = -1;
            accum = 0;
            docUpto = 0;
            docBufferUpto = BLOCK_SIZE;

            skipper = new ES812ScoreSkipReader(docIn.clone(), MAX_SKIP_LEVELS, true, indexHasOffsets, indexHasPayloads);
            skipper.init(docTermStartFP + termState.skipOffset, docTermStartFP, posTermStartFP, payTermStartFP, docFreq);
        }

        @Override
        public int freq() throws IOException {
            return freq;
        }

        @Override
        public int docID() {
            return doc;
        }

        private void refillDocs() throws IOException {
            final int left = docFreq - docUpto;
            assert left >= 0;

            if (left >= BLOCK_SIZE) {
                pforUtil.decodeAndPrefixSum(docIn, accum, docBuffer);
                pforUtil.decode(docIn, freqBuffer);
            } else {
                readVIntBlock(docIn, docBuffer, freqBuffer, left, true);
                prefixSum(docBuffer, left, accum);
                docBuffer[left] = NO_MORE_DOCS;
            }
            accum = docBuffer[BLOCK_SIZE - 1];
            docBufferUpto = 0;
        }

        private void refillPositions() throws IOException {
            if (posIn.getFilePointer() == lastPosBlockFP) {
                final int count = (int) (totalTermFreq % BLOCK_SIZE);
                int payloadLength = 0;
                for (int i = 0; i < count; i++) {
                    int code = posIn.readVInt();
                    if (indexHasPayloads) {
                        if ((code & 1) != 0) {
                            payloadLength = posIn.readVInt();
                        }
                        posDeltaBuffer[i] = code >>> 1;
                        if (payloadLength != 0) {
                            posIn.seek(posIn.getFilePointer() + payloadLength);
                        }
                    } else {
                        posDeltaBuffer[i] = code;
                    }
                    if (indexHasOffsets) {
                        if ((posIn.readVInt() & 1) != 0) {
                            posIn.readVInt();
                        }
                    }
                }
            } else {
                pforUtil.decode(posIn, posDeltaBuffer);
            }
        }

        @Override
        public void advanceShallow(int target) throws IOException {
            if (target > nextSkipDoc) {
                final int newDocUpto = skipper.skipTo(target) + 1;

                if (newDocUpto > docUpto) {
                    assert newDocUpto % BLOCK_SIZE == 0 : "got " + newDocUpto;
                    docUpto = newDocUpto;

                    docBufferUpto = BLOCK_SIZE;
                    accum = skipper.getDoc();
                    posPendingFP = skipper.getPosPointer();
                    posPendingCount = skipper.getPosBufferUpto();
                    seekTo = skipper.getDocPointer(); 
                }
                nextSkipDoc = skipper.getNextSkipDoc();
            }
            assert nextSkipDoc >= target;
        }

        @Override
        public Impacts getImpacts() throws IOException {
            advanceShallow(doc);
            return skipper.getImpacts();
        }

        @Override
        public int nextDoc() throws IOException {
            return advance(doc + 1);
        }

        @Override
        public int advance(int target) throws IOException {
            if (target > nextSkipDoc) {
                advanceShallow(target);
            }
            if (docBufferUpto == BLOCK_SIZE) {
                if (seekTo >= 0) {
                    docIn.seek(seekTo);
                    seekTo = -1;
                }
                refillDocs();
            }

            int next = findFirstGreater(docBuffer, target, docBufferUpto);
            if (next == BLOCK_SIZE) {
                return doc = NO_MORE_DOCS;
            }
            this.doc = (int) docBuffer[next];
            this.freq = (int) freqBuffer[next];
            for (int i = docBufferUpto; i <= next; ++i) {
                posPendingCount += (int) freqBuffer[i];
            }
            docUpto += next - docBufferUpto + 1;
            docBufferUpto = next + 1;
            position = 0;
            return doc;
        }

        private void skipPositions() throws IOException {
            int toSkip = posPendingCount - freq;

            final int leftInBlock = BLOCK_SIZE - posBufferUpto;
            if (toSkip < leftInBlock) {
                posBufferUpto += toSkip;
            } else {
                toSkip -= leftInBlock;
                while (toSkip >= BLOCK_SIZE) {
                    assert posIn.getFilePointer() != lastPosBlockFP;
                    pforUtil.skip(posIn);
                    toSkip -= BLOCK_SIZE;
                }
                refillPositions();
                posBufferUpto = toSkip;
            }

            position = 0;
        }

        @Override
        public int nextPosition() throws IOException {
            assert posPendingCount > 0;

            if (posPendingFP != -1) {
                posIn.seek(posPendingFP);
                posPendingFP = -1;

                posBufferUpto = BLOCK_SIZE;
            }

            if (posPendingCount > freq) {
                skipPositions();
                posPendingCount = freq;
            }

            if (posBufferUpto == BLOCK_SIZE) {
                refillPositions();
                posBufferUpto = 0;
            }
            position += (int) posDeltaBuffer[posBufferUpto++];

            posPendingCount--;
            return position;
        }

        @Override
        public int startOffset() {
            return -1;
        }

        @Override
        public int endOffset() {
            return -1;
        }

        @Override
        public BytesRef getPayload() {
            return null;
        }

        @Override
        public long cost() {
            return docFreq;
        }
    }

    final class BlockImpactsEverythingEnum extends ImpactsEnum {

        final PForUtil pforUtil = new PForUtil(new ForUtil());

        private final long[] docBuffer = new long[BLOCK_SIZE];
        private final long[] freqBuffer = new long[BLOCK_SIZE];
        private final long[] posDeltaBuffer = new long[BLOCK_SIZE];

        private final long[] payloadLengthBuffer;
        private final long[] offsetStartDeltaBuffer;
        private final long[] offsetLengthBuffer;

        private byte[] payloadBytes;
        private int payloadByteUpto;
        private int payloadLength;

        private int lastStartOffset;
        private int startOffset = -1;
        private int endOffset = -1;

        private int docBufferUpto;
        private int posBufferUpto;

        private final ES812ScoreSkipReader skipper;

        final IndexInput docIn;
        final IndexInput posIn;
        final IndexInput payIn;
        final BytesRef payload;

        final boolean indexHasFreq;
        final boolean indexHasPos;
        final boolean indexHasOffsets;
        final boolean indexHasPayloads;

        private int docFreq; 
        private long totalTermFreq; 
        private int docUpto; 
        private int posDocUpTo; 
        private int doc; 
        private long accum; 
        private int position; 

        private int posPendingCount;

        private long posPendingFP;

        private long payPendingFP;

        private long docTermStartFP;

        private long posTermStartFP;

        private long payTermStartFP;

        private long lastPosBlockFP;

        private int nextSkipDoc = -1;

        private final boolean needsPositions;
        private final boolean needsOffsets; 
        private final boolean needsPayloads; 

        private boolean isFreqsRead; 

        private long seekTo = -1;

        BlockImpactsEverythingEnum(FieldInfo fieldInfo, IntBlockTermState termState, int flags) throws IOException {
            indexHasFreq = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
            indexHasPos = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
            indexHasOffsets = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
            indexHasPayloads = fieldInfo.hasPayloads();

            needsPositions = PostingsEnum.featureRequested(flags, PostingsEnum.POSITIONS);
            needsOffsets = PostingsEnum.featureRequested(flags, PostingsEnum.OFFSETS);
            needsPayloads = PostingsEnum.featureRequested(flags, PostingsEnum.PAYLOADS);

            this.docIn = ES812PostingsReader.this.docIn.clone();

            if (indexHasPos && needsPositions) {
                this.posIn = ES812PostingsReader.this.posIn.clone();
            } else {
                this.posIn = null;
            }

            if ((indexHasOffsets && needsOffsets) || (indexHasPayloads && needsPayloads)) {
                this.payIn = ES812PostingsReader.this.payIn.clone();
            } else {
                this.payIn = null;
            }

            if (indexHasOffsets) {
                offsetStartDeltaBuffer = new long[BLOCK_SIZE];
                offsetLengthBuffer = new long[BLOCK_SIZE];
            } else {
                offsetStartDeltaBuffer = null;
                offsetLengthBuffer = null;
                startOffset = -1;
                endOffset = -1;
            }

            if (indexHasPayloads) {
                payloadLengthBuffer = new long[BLOCK_SIZE];
                payloadBytes = new byte[128];
                payload = new BytesRef();
            } else {
                payloadLengthBuffer = null;
                payloadBytes = null;
                payload = null;
            }

            docFreq = termState.docFreq;
            docTermStartFP = termState.docStartFP;
            posTermStartFP = termState.posStartFP;
            payTermStartFP = termState.payStartFP;
            totalTermFreq = termState.totalTermFreq;
            docIn.seek(docTermStartFP);
            posPendingFP = posTermStartFP;
            payPendingFP = payTermStartFP;
            posPendingCount = 0;
            if (termState.totalTermFreq < BLOCK_SIZE) {
                lastPosBlockFP = posTermStartFP;
            } else if (termState.totalTermFreq == BLOCK_SIZE) {
                lastPosBlockFP = -1;
            } else {
                lastPosBlockFP = posTermStartFP + termState.lastPosBlockOffset;
            }

            doc = -1;
            accum = 0;
            docUpto = 0;
            posDocUpTo = 0;
            isFreqsRead = true;
            docBufferUpto = BLOCK_SIZE;

            skipper = new ES812ScoreSkipReader(docIn.clone(), MAX_SKIP_LEVELS, indexHasPos, indexHasOffsets, indexHasPayloads);
            skipper.init(docTermStartFP + termState.skipOffset, docTermStartFP, posTermStartFP, payTermStartFP, docFreq);

            if (indexHasFreq == false) {
                for (int i = 0; i < ForUtil.BLOCK_SIZE; ++i) {
                    freqBuffer[i] = 1;
                }
            }
        }

        @Override
        public int freq() throws IOException {
            if (indexHasFreq && (isFreqsRead == false)) {
                pforUtil.decode(docIn, freqBuffer); 
                isFreqsRead = true;
            }
            return (int) freqBuffer[docBufferUpto - 1];
        }

        @Override
        public int docID() {
            return doc;
        }

        private void refillDocs() throws IOException {
            if (indexHasFreq) {
                if (isFreqsRead == false) { 
                    if (indexHasPos && needsPositions && (posDocUpTo < docUpto)) {
                        pforUtil.decode(docIn, freqBuffer); 
                    } else {
                        pforUtil.skip(docIn); 
                    }
                    isFreqsRead = true;
                }
                if (indexHasPos && needsPositions) {
                    while (posDocUpTo < docUpto) { 
                        posPendingCount += (int) freqBuffer[docBufferUpto - (docUpto - posDocUpTo)];
                        posDocUpTo++;
                    }
                }
            }

            final int left = docFreq - docUpto;
            assert left >= 0;

            if (left >= BLOCK_SIZE) {
                pforUtil.decodeAndPrefixSum(docIn, accum, docBuffer);
                if (indexHasFreq) {
                    isFreqsRead = false; 
                }
            } else {
                readVIntBlock(docIn, docBuffer, freqBuffer, left, indexHasFreq);
                prefixSum(docBuffer, left, accum);
                docBuffer[left] = NO_MORE_DOCS;
            }
            accum = docBuffer[BLOCK_SIZE - 1];
            docBufferUpto = 0;
        }

        private void refillPositions() throws IOException {
            if (posIn.getFilePointer() == lastPosBlockFP) {
                final int count = (int) (totalTermFreq % BLOCK_SIZE);
                int payloadLength = 0;
                int offsetLength = 0;
                payloadByteUpto = 0;
                for (int i = 0; i < count; i++) {
                    int code = posIn.readVInt();
                    if (indexHasPayloads) {
                        if ((code & 1) != 0) {
                            payloadLength = posIn.readVInt();
                        }
                        payloadLengthBuffer[i] = payloadLength;
                        posDeltaBuffer[i] = code >>> 1;
                        if (payloadLength != 0) {
                            if (payloadByteUpto + payloadLength > payloadBytes.length) {
                                payloadBytes = ArrayUtil.grow(payloadBytes, payloadByteUpto + payloadLength);
                            }
                            posIn.readBytes(payloadBytes, payloadByteUpto, payloadLength);
                            payloadByteUpto += payloadLength;
                        }
                    } else {
                        posDeltaBuffer[i] = code;
                    }

                    if (indexHasOffsets) {
                        int deltaCode = posIn.readVInt();
                        if ((deltaCode & 1) != 0) {
                            offsetLength = posIn.readVInt();
                        }
                        offsetStartDeltaBuffer[i] = deltaCode >>> 1;
                        offsetLengthBuffer[i] = offsetLength;
                    }
                }
                payloadByteUpto = 0;
            } else {
                pforUtil.decode(posIn, posDeltaBuffer);

                if (indexHasPayloads && payIn != null) {
                    if (needsPayloads) {
                        pforUtil.decode(payIn, payloadLengthBuffer);
                        int numBytes = payIn.readVInt();

                        if (numBytes > payloadBytes.length) {
                            payloadBytes = ArrayUtil.growNoCopy(payloadBytes, numBytes);
                        }
                        payIn.readBytes(payloadBytes, 0, numBytes);
                    } else {
                        pforUtil.skip(payIn); 
                        int numBytes = payIn.readVInt(); 
                        payIn.seek(payIn.getFilePointer() + numBytes); 
                    }
                    payloadByteUpto = 0;
                }

                if (indexHasOffsets && payIn != null) {
                    if (needsOffsets) {
                        pforUtil.decode(payIn, offsetStartDeltaBuffer);
                        pforUtil.decode(payIn, offsetLengthBuffer);
                    } else {
                        pforUtil.skip(payIn); 
                        pforUtil.skip(payIn); 
                    }
                }
            }
        }

        @Override
        public void advanceShallow(int target) throws IOException {
            if (target > nextSkipDoc) {
                final int newDocUpto = skipper.skipTo(target) + 1;

                if (newDocUpto > docUpto) {
                    assert newDocUpto % BLOCK_SIZE == 0 : "got " + newDocUpto;
                    docUpto = newDocUpto;
                    posDocUpTo = docUpto;

                    docBufferUpto = BLOCK_SIZE;
                    accum = skipper.getDoc();
                    posPendingFP = skipper.getPosPointer();
                    payPendingFP = skipper.getPayPointer();
                    posPendingCount = skipper.getPosBufferUpto();
                    lastStartOffset = 0; 
                    payloadByteUpto = skipper.getPayloadByteUpto(); 
                    seekTo = skipper.getDocPointer(); 
                }
                nextSkipDoc = skipper.getNextSkipDoc();
            }
            assert nextSkipDoc >= target;
        }

        @Override
        public Impacts getImpacts() throws IOException {
            advanceShallow(doc);
            return skipper.getImpacts();
        }

        @Override
        public int nextDoc() throws IOException {
            return advance(doc + 1);
        }

        @Override
        public int advance(int target) throws IOException {
            if (target > nextSkipDoc) {
                advanceShallow(target);
            }
            if (docBufferUpto == BLOCK_SIZE) {
                if (seekTo >= 0) {
                    docIn.seek(seekTo);
                    seekTo = -1;
                    isFreqsRead = true; 
                }
                refillDocs();
            }

            long doc;
            while (true) {
                doc = docBuffer[docBufferUpto];
                docBufferUpto++;
                docUpto++;

                if (doc >= target) {
                    break;
                }

                if (docBufferUpto == BLOCK_SIZE) {
                    return this.doc = NO_MORE_DOCS;
                }
            }
            position = 0;
            lastStartOffset = 0;

            return this.doc = (int) doc;
        }

        private void skipPositions() throws IOException {
            int toSkip = posPendingCount - (int) freqBuffer[docBufferUpto - 1];

            final int leftInBlock = BLOCK_SIZE - posBufferUpto;
            if (toSkip < leftInBlock) {
                int end = posBufferUpto + toSkip;
                while (posBufferUpto < end) {
                    if (indexHasPayloads) {
                        payloadByteUpto += (int) payloadLengthBuffer[posBufferUpto];
                    }
                    posBufferUpto++;
                }
            } else {
                toSkip -= leftInBlock;
                while (toSkip >= BLOCK_SIZE) {
                    assert posIn.getFilePointer() != lastPosBlockFP;
                    pforUtil.skip(posIn);

                    if (indexHasPayloads && payIn != null) {
                        pforUtil.skip(payIn);

                        int numBytes = payIn.readVInt();
                        payIn.seek(payIn.getFilePointer() + numBytes);
                    }

                    if (indexHasOffsets && payIn != null) {
                        pforUtil.skip(payIn);
                        pforUtil.skip(payIn);
                    }
                    toSkip -= BLOCK_SIZE;
                }
                refillPositions();
                payloadByteUpto = 0;
                posBufferUpto = 0;
                while (posBufferUpto < toSkip) {
                    if (indexHasPayloads) {
                        payloadByteUpto += (int) payloadLengthBuffer[posBufferUpto];
                    }
                    posBufferUpto++;
                }
            }

            position = 0;
            lastStartOffset = 0;
        }

        @Override
        public int nextPosition() throws IOException {
            if (indexHasPos == false || needsPositions == false) {
                return -1;
            }

            if (isFreqsRead == false) {
                pforUtil.decode(docIn, freqBuffer); 
                isFreqsRead = true;
            }
            while (posDocUpTo < docUpto) { 
                posPendingCount += (int) freqBuffer[docBufferUpto - (docUpto - posDocUpTo)];
                posDocUpTo++;
            }

            assert posPendingCount > 0;

            if (posPendingFP != -1) {
                posIn.seek(posPendingFP);
                posPendingFP = -1;

                if (payPendingFP != -1 && payIn != null) {
                    payIn.seek(payPendingFP);
                    payPendingFP = -1;
                }

                posBufferUpto = BLOCK_SIZE;
            }

            if (posPendingCount > freqBuffer[docBufferUpto - 1]) {
                skipPositions();
                posPendingCount = (int) freqBuffer[docBufferUpto - 1];
            }

            if (posBufferUpto == BLOCK_SIZE) {
                refillPositions();
                posBufferUpto = 0;
            }
            position += (int) posDeltaBuffer[posBufferUpto];

            if (indexHasPayloads) {
                payloadLength = (int) payloadLengthBuffer[posBufferUpto];
                payload.bytes = payloadBytes;
                payload.offset = payloadByteUpto;
                payload.length = payloadLength;
                payloadByteUpto += payloadLength;
            }

            if (indexHasOffsets && needsOffsets) {
                startOffset = lastStartOffset + (int) offsetStartDeltaBuffer[posBufferUpto];
                endOffset = startOffset + (int) offsetLengthBuffer[posBufferUpto];
                lastStartOffset = startOffset;
            }

            posBufferUpto++;
            posPendingCount--;
            return position;
        }

        @Override
        public int startOffset() {
            return startOffset;
        }

        @Override
        public int endOffset() {
            return endOffset;
        }

        @Override
        public BytesRef getPayload() {
            if (payloadLength == 0) {
                return null;
            } else {
                return payload;
            }
        }

        @Override
        public long cost() {
            return docFreq;
        }
    }

    @Override
    public void checkIntegrity() throws IOException {
        if (docIn != null) {
            CodecUtil.checksumEntireFile(docIn);
        }
        if (posIn != null) {
            CodecUtil.checksumEntireFile(posIn);
        }
        if (payIn != null) {
            CodecUtil.checksumEntireFile(payIn);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(positions=" + (posIn != null) + ",payloads=" + (payIn != null) + ")";
    }
}
