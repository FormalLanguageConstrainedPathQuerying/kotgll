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
 * Modifications copyright (C) 2021 Elasticsearch B.V.
 */
package org.elasticsearch.xpack.lucene.bwc.codecs.lucene50;

import org.apache.lucene.backward_codecs.store.EndiannessReverserUtil;
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
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.core.IOUtils;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.BWCLucene50PostingsFormat.IntBlockTermState;

import java.io.IOException;
import java.util.Arrays;

import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.BWCLucene50PostingsFormat.BLOCK_SIZE;
import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.BWCLucene50PostingsFormat.DOC_CODEC;
import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.BWCLucene50PostingsFormat.MAX_SKIP_LEVELS;
import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.BWCLucene50PostingsFormat.PAY_CODEC;
import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.BWCLucene50PostingsFormat.POS_CODEC;
import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.BWCLucene50PostingsFormat.TERMS_CODEC;
import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.BWCLucene50PostingsFormat.VERSION_CURRENT;
import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.BWCLucene50PostingsFormat.VERSION_START;
import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.ForUtil.MAX_DATA_SIZE;
import static org.elasticsearch.xpack.lucene.bwc.codecs.lucene50.ForUtil.MAX_ENCODED_SIZE;

/**
 * Concrete class that reads docId(maybe frq,pos,offset,payloads) list with postings format.
 */
public final class Lucene50PostingsReader extends PostingsReaderBase {

    private final IndexInput docIn;
    private final IndexInput posIn;
    private final IndexInput payIn;

    final ForUtil forUtil;
    private int version;

    /** Sole constructor. */
    public Lucene50PostingsReader(SegmentReadState state) throws IOException {
        boolean success = false;
        IndexInput docIn = null;
        IndexInput posIn = null;
        IndexInput payIn = null;


        String docName = IndexFileNames.segmentFileName(
            state.segmentInfo.name,
            state.segmentSuffix,
            BWCLucene50PostingsFormat.DOC_EXTENSION
        );
        try {
            docIn = EndiannessReverserUtil.openInput(state.directory, docName, state.context);
            version = CodecUtil.checkIndexHeader(
                docIn,
                DOC_CODEC,
                VERSION_START,
                VERSION_CURRENT,
                state.segmentInfo.getId(),
                state.segmentSuffix
            );
            forUtil = new ForUtil(docIn);
            CodecUtil.retrieveChecksum(docIn);

            if (state.fieldInfos.hasProx()) {
                String proxName = IndexFileNames.segmentFileName(
                    state.segmentInfo.name,
                    state.segmentSuffix,
                    BWCLucene50PostingsFormat.POS_EXTENSION
                );
                posIn = EndiannessReverserUtil.openInput(state.directory, proxName, state.context);
                CodecUtil.checkIndexHeader(posIn, POS_CODEC, version, version, state.segmentInfo.getId(), state.segmentSuffix);
                CodecUtil.retrieveChecksum(posIn);

                if (state.fieldInfos.hasPayloads() || state.fieldInfos.hasOffsets()) {
                    String payName = IndexFileNames.segmentFileName(
                        state.segmentInfo.name,
                        state.segmentSuffix,
                        BWCLucene50PostingsFormat.PAY_EXTENSION
                    );
                    payIn = EndiannessReverserUtil.openInput(state.directory, payName, state.context);
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
    static void readVIntBlock(IndexInput docIn, int[] docBuffer, int[] freqBuffer, int num, boolean indexHasFreq) throws IOException {
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

        termState.docStartFP += in.readVLong();
        if (fieldHasPositions) {
            termState.posStartFP += in.readVLong();
            if (fieldHasOffsets || fieldHasPayloads) {
                termState.payStartFP += in.readVLong();
            }
        }
        if (termState.docFreq == 1) {
            termState.singletonDocID = in.readVInt();
        } else {
            termState.singletonDocID = -1;
        }
        if (fieldHasPositions) {
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
        if (state.docFreq <= BLOCK_SIZE || version < BWCLucene50PostingsFormat.VERSION_IMPACT_SKIP_DATA) {
            return new SlowImpactsEnum(postings(fieldInfo, state, null, flags));
        }

        final boolean indexHasPositions = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
        final boolean indexHasOffsets = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
        final boolean indexHasPayloads = fieldInfo.hasPayloads();

        if (indexHasPositions
            && PostingsEnum.featureRequested(flags, PostingsEnum.POSITIONS)
            && (indexHasOffsets == false || PostingsEnum.featureRequested(flags, PostingsEnum.OFFSETS) == false)
            && (indexHasPayloads == false || PostingsEnum.featureRequested(flags, PostingsEnum.PAYLOADS) == false)) {
            return new BlockImpactsPostingsEnum(fieldInfo, (IntBlockTermState) state);
        }

        return new BlockImpactsEverythingEnum(fieldInfo, (IntBlockTermState) state, flags);
    }

    final class BlockDocsEnum extends PostingsEnum {
        private final byte[] encoded;

        private final int[] docDeltaBuffer = new int[MAX_DATA_SIZE];
        private final int[] freqBuffer = new int[MAX_DATA_SIZE];

        private int docBufferUpto;

        private Lucene50SkipReader skipper;
        private boolean skipped;

        final IndexInput startDocIn;

        IndexInput docIn;
        final boolean indexHasFreq;
        final boolean indexHasPos;
        final boolean indexHasOffsets;
        final boolean indexHasPayloads;

        private int docFreq; 
        private long totalTermFreq; 
        private int docUpto; 
        private int doc; 
        private int accum; 

        private long docTermStartFP;

        private long skipOffset;

        private int nextSkipDoc;

        private boolean needsFreq; 
        private boolean isFreqsRead;
        private int singletonDocID; 

        BlockDocsEnum(FieldInfo fieldInfo) throws IOException {
            this.startDocIn = Lucene50PostingsReader.this.docIn;
            this.docIn = null;
            indexHasFreq = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
            indexHasPos = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
            indexHasOffsets = fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
            indexHasPayloads = fieldInfo.hasPayloads();
            encoded = new byte[MAX_ENCODED_SIZE];
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
                Arrays.fill(freqBuffer, 1);
            }
            accum = 0;
            docUpto = 0;
            nextSkipDoc = BLOCK_SIZE - 1; 
            docBufferUpto = BLOCK_SIZE;
            skipped = false;
            return this;
        }

        @Override
        public int freq() throws IOException {
            if (isFreqsRead == false) {
                forUtil.readBlock(docIn, encoded, freqBuffer); 
                isFreqsRead = true;
            }
            return freqBuffer[docBufferUpto - 1];
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
                forUtil.skipBlock(docIn);
                isFreqsRead = true;
            }

            final int left = docFreq - docUpto;
            assert left > 0;

            if (left >= BLOCK_SIZE) {
                forUtil.readBlock(docIn, encoded, docDeltaBuffer);

                if (indexHasFreq) {
                    if (needsFreq) {
                        isFreqsRead = false;
                    } else {
                        forUtil.skipBlock(docIn); 
                    }
                }
            } else if (docFreq == 1) {
                docDeltaBuffer[0] = singletonDocID;
                freqBuffer[0] = (int) totalTermFreq;
            } else {
                readVIntBlock(docIn, docDeltaBuffer, freqBuffer, left, indexHasFreq);
            }
            docBufferUpto = 0;
        }

        @Override
        public int nextDoc() throws IOException {
            if (docUpto == docFreq) {
                return doc = NO_MORE_DOCS;
            }
            if (docBufferUpto == BLOCK_SIZE) {
                refillDocs(); 
            }

            accum += docDeltaBuffer[docBufferUpto];
            docUpto++;

            doc = accum;
            docBufferUpto++;
            return doc;
        }

        @Override
        public int advance(int target) throws IOException {
            if (docFreq > BLOCK_SIZE && target > nextSkipDoc) {

                if (skipper == null) {
                    skipper = new Lucene50SkipReader(
                        version,
                        docIn.clone(),
                        MAX_SKIP_LEVELS,
                        indexHasPos,
                        indexHasOffsets,
                        indexHasPayloads
                    );
                }

                if (skipped == false) {
                    assert skipOffset != -1;
                    skipper.init(docTermStartFP + skipOffset, docTermStartFP, 0, 0, docFreq);
                    skipped = true;
                }

                final int newDocUpto = skipper.skipTo(target) + 1;

                if (newDocUpto > docUpto) {
                    assert newDocUpto % BLOCK_SIZE == 0 : "got " + newDocUpto;
                    docUpto = newDocUpto;

                    docBufferUpto = BLOCK_SIZE;
                    accum = skipper.getDoc(); 
                    docIn.seek(skipper.getDocPointer()); 
                    isFreqsRead = true;
                }
                nextSkipDoc = skipper.getNextSkipDoc();
            }
            if (docUpto == docFreq) {
                return doc = NO_MORE_DOCS;
            }
            if (docBufferUpto == BLOCK_SIZE) {
                refillDocs();
            }

            while (true) {
                accum += docDeltaBuffer[docBufferUpto];
                docUpto++;

                if (accum >= target) {
                    break;
                }
                docBufferUpto++;
                if (docUpto == docFreq) {
                    return doc = NO_MORE_DOCS;
                }
            }

            docBufferUpto++;
            return doc = accum;
        }

        @Override
        public long cost() {
            return docFreq;
        }
    }

    final class EverythingEnum extends PostingsEnum {

        private final byte[] encoded;

        private final int[] docDeltaBuffer = new int[MAX_DATA_SIZE];
        private final int[] freqBuffer = new int[MAX_DATA_SIZE];
        private final int[] posDeltaBuffer = new int[MAX_DATA_SIZE];

        private final int[] payloadLengthBuffer;
        private final int[] offsetStartDeltaBuffer;
        private final int[] offsetLengthBuffer;

        private byte[] payloadBytes;
        private int payloadByteUpto;
        private int payloadLength;

        private int lastStartOffset;
        private int startOffset;
        private int endOffset;

        private int docBufferUpto;
        private int posBufferUpto;

        private Lucene50SkipReader skipper;
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
        private int docUpto; 
        private int doc; 
        private int accum; 
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

            this.startDocIn = Lucene50PostingsReader.this.docIn;
            this.docIn = null;
            this.posIn = Lucene50PostingsReader.this.posIn.clone();
            if (indexHasOffsets || indexHasPayloads) {
                this.payIn = Lucene50PostingsReader.this.payIn.clone();
            } else {
                this.payIn = null;
            }
            encoded = new byte[MAX_ENCODED_SIZE];
            if (indexHasOffsets) {
                offsetStartDeltaBuffer = new int[MAX_DATA_SIZE];
                offsetLengthBuffer = new int[MAX_DATA_SIZE];
            } else {
                offsetStartDeltaBuffer = null;
                offsetLengthBuffer = null;
                startOffset = -1;
                endOffset = -1;
            }

            if (indexHasPayloads) {
                payloadLengthBuffer = new int[MAX_DATA_SIZE];
                payloadBytes = new byte[128];
                payload = new BytesRef();
            } else {
                payloadLengthBuffer = null;
                payloadBytes = null;
                payload = null;
            }
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
            docUpto = 0;
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
            final int left = docFreq - docUpto;
            assert left > 0;

            if (left >= BLOCK_SIZE) {
                forUtil.readBlock(docIn, encoded, docDeltaBuffer);
                forUtil.readBlock(docIn, encoded, freqBuffer);
            } else if (docFreq == 1) {
                docDeltaBuffer[0] = singletonDocID;
                freqBuffer[0] = (int) totalTermFreq;
            } else {
                readVIntBlock(docIn, docDeltaBuffer, freqBuffer, left, true);
            }
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
                forUtil.readBlock(posIn, encoded, posDeltaBuffer);

                if (indexHasPayloads) {
                    if (needsPayloads) {
                        forUtil.readBlock(payIn, encoded, payloadLengthBuffer);
                        int numBytes = payIn.readVInt();

                        if (numBytes > payloadBytes.length) {
                            payloadBytes = ArrayUtil.grow(payloadBytes, numBytes);
                        }
                        payIn.readBytes(payloadBytes, 0, numBytes);
                    } else {
                        forUtil.skipBlock(payIn); 
                        int numBytes = payIn.readVInt(); 
                        payIn.seek(payIn.getFilePointer() + numBytes); 
                    }
                    payloadByteUpto = 0;
                }

                if (indexHasOffsets) {
                    if (needsOffsets) {
                        forUtil.readBlock(payIn, encoded, offsetStartDeltaBuffer);
                        forUtil.readBlock(payIn, encoded, offsetLengthBuffer);
                    } else {
                        forUtil.skipBlock(payIn); 
                        forUtil.skipBlock(payIn); 
                    }
                }
            }
        }

        @Override
        public int nextDoc() throws IOException {
            if (docUpto == docFreq) {
                return doc = NO_MORE_DOCS;
            }
            if (docBufferUpto == BLOCK_SIZE) {
                refillDocs();
            }

            accum += docDeltaBuffer[docBufferUpto];
            freq = freqBuffer[docBufferUpto];
            posPendingCount += freq;
            docBufferUpto++;
            docUpto++;

            doc = accum;
            position = 0;
            lastStartOffset = 0;
            return doc;
        }

        @Override
        public int advance(int target) throws IOException {

            if (target > nextSkipDoc) {
                if (skipper == null) {
                    skipper = new Lucene50SkipReader(version, docIn.clone(), MAX_SKIP_LEVELS, true, indexHasOffsets, indexHasPayloads);
                }

                if (skipped == false) {
                    assert skipOffset != -1;
                    skipper.init(docTermStartFP + skipOffset, docTermStartFP, posTermStartFP, payTermStartFP, docFreq);
                    skipped = true;
                }

                final int newDocUpto = skipper.skipTo(target) + 1;

                if (newDocUpto > docUpto) {
                    assert newDocUpto % BLOCK_SIZE == 0 : "got " + newDocUpto;
                    docUpto = newDocUpto;

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
            if (docUpto == docFreq) {
                return doc = NO_MORE_DOCS;
            }
            if (docBufferUpto == BLOCK_SIZE) {
                refillDocs();
            }

            while (true) {
                accum += docDeltaBuffer[docBufferUpto];
                freq = freqBuffer[docBufferUpto];
                posPendingCount += freq;
                docBufferUpto++;
                docUpto++;

                if (accum >= target) {
                    break;
                }
                if (docUpto == docFreq) {
                    return doc = NO_MORE_DOCS;
                }
            }

            position = 0;
            lastStartOffset = 0;
            return doc = accum;
        }

        private void skipPositions() throws IOException {
            int toSkip = posPendingCount - freq;

            final int leftInBlock = BLOCK_SIZE - posBufferUpto;
            if (toSkip < leftInBlock) {
                int end = posBufferUpto + toSkip;
                while (posBufferUpto < end) {
                    if (indexHasPayloads) {
                        payloadByteUpto += payloadLengthBuffer[posBufferUpto];
                    }
                    posBufferUpto++;
                }
            } else {
                toSkip -= leftInBlock;
                while (toSkip >= BLOCK_SIZE) {
                    assert posIn.getFilePointer() != lastPosBlockFP;
                    forUtil.skipBlock(posIn);

                    if (indexHasPayloads) {
                        forUtil.skipBlock(payIn);

                        int numBytes = payIn.readVInt();
                        payIn.seek(payIn.getFilePointer() + numBytes);
                    }

                    if (indexHasOffsets) {
                        forUtil.skipBlock(payIn);
                        forUtil.skipBlock(payIn);
                    }
                    toSkip -= BLOCK_SIZE;
                }
                refillPositions();
                payloadByteUpto = 0;
                posBufferUpto = 0;
                while (posBufferUpto < toSkip) {
                    if (indexHasPayloads) {
                        payloadByteUpto += payloadLengthBuffer[posBufferUpto];
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
            position += posDeltaBuffer[posBufferUpto];

            if (indexHasPayloads) {
                payloadLength = payloadLengthBuffer[posBufferUpto];
                payload.bytes = payloadBytes;
                payload.offset = payloadByteUpto;
                payload.length = payloadLength;
                payloadByteUpto += payloadLength;
            }

            if (indexHasOffsets) {
                startOffset = lastStartOffset + offsetStartDeltaBuffer[posBufferUpto];
                endOffset = startOffset + offsetLengthBuffer[posBufferUpto];
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

    final class BlockImpactsPostingsEnum extends ImpactsEnum {

        private final byte[] encoded;

        private final int[] docDeltaBuffer = new int[MAX_DATA_SIZE];
        private final int[] freqBuffer = new int[MAX_DATA_SIZE];
        private final int[] posDeltaBuffer = new int[MAX_DATA_SIZE];

        private int docBufferUpto;
        private int posBufferUpto;

        private final Lucene50ScoreSkipReader skipper;

        final IndexInput docIn;
        final IndexInput posIn;

        final boolean indexHasOffsets;
        final boolean indexHasPayloads;

        private int docFreq; 
        private long totalTermFreq; 
        private int docUpto; 
        private int doc; 
        private int accum; 
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

            this.docIn = Lucene50PostingsReader.this.docIn.clone();

            encoded = new byte[MAX_ENCODED_SIZE];

            this.posIn = Lucene50PostingsReader.this.posIn.clone();

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

            skipper = new Lucene50ScoreSkipReader(version, docIn.clone(), MAX_SKIP_LEVELS, true, indexHasOffsets, indexHasPayloads);
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
            assert left > 0;

            if (left >= BLOCK_SIZE) {
                forUtil.readBlock(docIn, encoded, docDeltaBuffer);
                forUtil.readBlock(docIn, encoded, freqBuffer);
            } else {
                readVIntBlock(docIn, docDeltaBuffer, freqBuffer, left, true);
            }
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
                forUtil.readBlock(posIn, encoded, posDeltaBuffer);
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
            if (docUpto == docFreq) {
                return doc = NO_MORE_DOCS;
            }
            if (docBufferUpto == BLOCK_SIZE) {
                if (seekTo >= 0) {
                    docIn.seek(seekTo);
                    seekTo = -1;
                }
                refillDocs();
            }

            while (true) {
                accum += docDeltaBuffer[docBufferUpto];
                freq = freqBuffer[docBufferUpto];
                posPendingCount += freq;
                docBufferUpto++;
                docUpto++;

                if (accum >= target) {
                    break;
                }
                if (docUpto == docFreq) {
                    return doc = NO_MORE_DOCS;
                }
            }
            position = 0;

            return doc = accum;
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
                    forUtil.skipBlock(posIn);
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
            position += posDeltaBuffer[posBufferUpto++];

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

        private final byte[] encoded;

        private final int[] docDeltaBuffer = new int[MAX_DATA_SIZE];
        private final int[] freqBuffer = new int[MAX_DATA_SIZE];
        private final int[] posDeltaBuffer = new int[MAX_DATA_SIZE];

        private final int[] payloadLengthBuffer;
        private final int[] offsetStartDeltaBuffer;
        private final int[] offsetLengthBuffer;

        private byte[] payloadBytes;
        private int payloadByteUpto;
        private int payloadLength;

        private int lastStartOffset;
        private int startOffset = -1;
        private int endOffset = -1;

        private int docBufferUpto;
        private int posBufferUpto;

        private final Lucene50ScoreSkipReader skipper;

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
        private int accum; 
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

            this.docIn = Lucene50PostingsReader.this.docIn.clone();

            encoded = new byte[MAX_ENCODED_SIZE];

            if (indexHasPos && needsPositions) {
                this.posIn = Lucene50PostingsReader.this.posIn.clone();
            } else {
                this.posIn = null;
            }

            if ((indexHasOffsets && needsOffsets) || (indexHasPayloads && needsPayloads)) {
                this.payIn = Lucene50PostingsReader.this.payIn.clone();
            } else {
                this.payIn = null;
            }

            if (indexHasOffsets) {
                offsetStartDeltaBuffer = new int[MAX_DATA_SIZE];
                offsetLengthBuffer = new int[MAX_DATA_SIZE];
            } else {
                offsetStartDeltaBuffer = null;
                offsetLengthBuffer = null;
                startOffset = -1;
                endOffset = -1;
            }

            if (indexHasPayloads) {
                payloadLengthBuffer = new int[MAX_DATA_SIZE];
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

            skipper = new Lucene50ScoreSkipReader(version, docIn.clone(), MAX_SKIP_LEVELS, indexHasPos, indexHasOffsets, indexHasPayloads);
            skipper.init(docTermStartFP + termState.skipOffset, docTermStartFP, posTermStartFP, payTermStartFP, docFreq);

            if (indexHasFreq == false) {
                Arrays.fill(freqBuffer, 1);
            }
        }

        @Override
        public int freq() throws IOException {
            if (indexHasFreq && (isFreqsRead == false)) {
                forUtil.readBlock(docIn, encoded, freqBuffer); 
                isFreqsRead = true;
            }
            return freqBuffer[docBufferUpto - 1];
        }

        @Override
        public int docID() {
            return doc;
        }

        private void refillDocs() throws IOException {
            if (indexHasFreq) {
                if (isFreqsRead == false) { 
                    if (indexHasPos && needsPositions && (posDocUpTo < docUpto)) {
                        forUtil.readBlock(docIn, encoded, freqBuffer); 
                    } else {
                        forUtil.skipBlock(docIn); 
                    }
                    isFreqsRead = true;
                }
                if (indexHasPos && needsPositions) {
                    while (posDocUpTo < docUpto) { 
                        posPendingCount += freqBuffer[docBufferUpto - (docUpto - posDocUpTo)];
                        posDocUpTo++;
                    }
                }
            }

            final int left = docFreq - docUpto;
            assert left > 0;

            if (left >= BLOCK_SIZE) {
                forUtil.readBlock(docIn, encoded, docDeltaBuffer);
                if (indexHasFreq) {
                    isFreqsRead = false; 
                }
            } else {
                readVIntBlock(docIn, docDeltaBuffer, freqBuffer, left, indexHasFreq);
            }
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
                forUtil.readBlock(posIn, encoded, posDeltaBuffer);

                if (indexHasPayloads && payIn != null) {
                    if (needsPayloads) {
                        forUtil.readBlock(payIn, encoded, payloadLengthBuffer);
                        int numBytes = payIn.readVInt();

                        if (numBytes > payloadBytes.length) {
                            payloadBytes = ArrayUtil.grow(payloadBytes, numBytes);
                        }
                        payIn.readBytes(payloadBytes, 0, numBytes);
                    } else {
                        forUtil.skipBlock(payIn); 
                        int numBytes = payIn.readVInt(); 
                        payIn.seek(payIn.getFilePointer() + numBytes); 
                    }
                    payloadByteUpto = 0;
                }

                if (indexHasOffsets && payIn != null) {
                    if (needsOffsets) {
                        forUtil.readBlock(payIn, encoded, offsetStartDeltaBuffer);
                        forUtil.readBlock(payIn, encoded, offsetLengthBuffer);
                    } else {
                        forUtil.skipBlock(payIn); 
                        forUtil.skipBlock(payIn); 
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
            if (docUpto == docFreq) {
                return doc = NO_MORE_DOCS;
            }
            if (docBufferUpto == BLOCK_SIZE) {
                if (seekTo >= 0) {
                    docIn.seek(seekTo);
                    seekTo = -1;
                    isFreqsRead = true; 
                }
                refillDocs();
            }

            while (true) {
                accum += docDeltaBuffer[docBufferUpto];
                docBufferUpto++;
                docUpto++;

                if (accum >= target) {
                    break;
                }
                if (docUpto == docFreq) {
                    return doc = NO_MORE_DOCS;
                }
            }
            position = 0;
            lastStartOffset = 0;

            return doc = accum;
        }

        private void skipPositions() throws IOException {
            int toSkip = posPendingCount - freqBuffer[docBufferUpto - 1];

            final int leftInBlock = BLOCK_SIZE - posBufferUpto;
            if (toSkip < leftInBlock) {
                int end = posBufferUpto + toSkip;
                while (posBufferUpto < end) {
                    if (indexHasPayloads) {
                        payloadByteUpto += payloadLengthBuffer[posBufferUpto];
                    }
                    posBufferUpto++;
                }
            } else {
                toSkip -= leftInBlock;
                while (toSkip >= BLOCK_SIZE) {
                    assert posIn.getFilePointer() != lastPosBlockFP;
                    forUtil.skipBlock(posIn);

                    if (indexHasPayloads && payIn != null) {
                        forUtil.skipBlock(payIn);

                        int numBytes = payIn.readVInt();
                        payIn.seek(payIn.getFilePointer() + numBytes);
                    }

                    if (indexHasOffsets && payIn != null) {
                        forUtil.skipBlock(payIn);
                        forUtil.skipBlock(payIn);
                    }
                    toSkip -= BLOCK_SIZE;
                }
                refillPositions();
                payloadByteUpto = 0;
                posBufferUpto = 0;
                while (posBufferUpto < toSkip) {
                    if (indexHasPayloads) {
                        payloadByteUpto += payloadLengthBuffer[posBufferUpto];
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
                forUtil.readBlock(docIn, encoded, freqBuffer); 
                isFreqsRead = true;
            }
            while (posDocUpTo < docUpto) { 
                posPendingCount += freqBuffer[docBufferUpto - (docUpto - posDocUpTo)];
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
                posPendingCount = freqBuffer[docBufferUpto - 1];
            }

            if (posBufferUpto == BLOCK_SIZE) {
                refillPositions();
                posBufferUpto = 0;
            }
            position += posDeltaBuffer[posBufferUpto];

            if (indexHasPayloads) {
                payloadLength = payloadLengthBuffer[posBufferUpto];
                payload.bytes = payloadBytes;
                payload.offset = payloadByteUpto;
                payload.length = payloadLength;
                payloadByteUpto += payloadLength;
            }

            if (indexHasOffsets && needsOffsets) {
                startOffset = lastStartOffset + offsetStartDeltaBuffer[posBufferUpto];
                endOffset = startOffset + offsetLengthBuffer[posBufferUpto];
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
