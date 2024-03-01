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
package org.elasticsearch.xpack.lucene.bwc.codecs.lucene40.blocktree;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.TermsEnum.SeekStatus;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.FST;

import java.io.IOException;
import java.util.Arrays;

final class SegmentTermsEnumFrame {
    final int ord;

    boolean hasTerms;
    boolean hasTermsOrig;
    boolean isFloor;

    FST.Arc<BytesRef> arc;


    long fp;
    long fpOrig;
    long fpEnd;
    long totalSuffixBytes; 

    byte[] suffixBytes = new byte[128];
    final ByteArrayDataInput suffixesReader = new ByteArrayDataInput();

    byte[] suffixLengthBytes;
    final ByteArrayDataInput suffixLengthsReader;

    byte[] statBytes = new byte[64];
    int statsSingletonRunLength = 0;
    final ByteArrayDataInput statsReader = new ByteArrayDataInput();

    byte[] floorData = new byte[32];
    final ByteArrayDataInput floorDataReader = new ByteArrayDataInput();

    int prefix;

    int entCount;

    int nextEnt;

    boolean isLastInFloor;

    boolean isLeafBlock;

    long lastSubFP;

    int nextFloorLabel;
    int numFollowFloorBlocks;

    int metaDataUpto;

    final BlockTermState state;

    byte[] bytes = new byte[32];
    final ByteArrayDataInput bytesReader = new ByteArrayDataInput();

    private final SegmentTermsEnum ste;
    private final int version;

    SegmentTermsEnumFrame(SegmentTermsEnum ste, int ord) throws IOException {
        this.ste = ste;
        this.ord = ord;
        this.state = ste.fr.parent.postingsReader.newTermState();
        this.state.totalTermFreq = -1;
        this.version = ste.fr.parent.version;
        if (version >= Lucene40BlockTreeTermsReader.VERSION_COMPRESSED_SUFFIXES) {
            suffixLengthBytes = new byte[32];
            suffixLengthsReader = new ByteArrayDataInput();
        } else {
            suffixLengthBytes = null;
            suffixLengthsReader = suffixesReader;
        }
    }

    public void setFloorData(ByteArrayDataInput in, BytesRef source) {
        final int numBytes = source.length - (in.getPosition() - source.offset);
        if (numBytes > floorData.length) {
            floorData = new byte[ArrayUtil.oversize(numBytes, 1)];
        }
        System.arraycopy(source.bytes, source.offset + in.getPosition(), floorData, 0, numBytes);
        floorDataReader.reset(floorData, 0, numBytes);
        numFollowFloorBlocks = floorDataReader.readVInt();
        nextFloorLabel = floorDataReader.readByte() & 0xff;
    }

    public int getTermBlockOrd() {
        return isLeafBlock ? nextEnt : state.termBlockOrd;
    }

    void loadNextFloorBlock() throws IOException {
        assert arc == null || isFloor : "arc=" + arc + " isFloor=" + isFloor;
        fp = fpEnd;
        nextEnt = -1;
        loadBlock();
    }

    /* Does initial decode of next block of terms; this
    doesn't actually decode the docFreq, totalTermFreq,
    postings details (frq/prx offset, etc.) metadata;
    it just loads them as byte[] blobs which are then
    decoded on-demand if the metadata is ever requested
    for any term in this block.  This enables terms-only
    intensive consumes (eg certain MTQs, respelling) to
    not pay the price of decoding metadata they won't
    use. */
    void loadBlock() throws IOException {

        ste.initIndexInput();

        if (nextEnt != -1) {
            return;
        }

        ste.in.seek(fp);
        int code = ste.in.readVInt();
        entCount = code >>> 1;
        assert entCount > 0;
        isLastInFloor = (code & 1) != 0;

        assert arc == null || (isLastInFloor || isFloor)
            : "fp=" + fp + " arc=" + arc + " isFloor=" + isFloor + " isLastInFloor=" + isLastInFloor;


        final long startSuffixFP = ste.in.getFilePointer();
        if (version >= Lucene40BlockTreeTermsReader.VERSION_COMPRESSED_SUFFIXES) {
            final long codeL = ste.in.readVLong();
            isLeafBlock = (codeL & 0x04) != 0;
            final int numSuffixBytes = (int) (codeL >>> 3);
            if (suffixBytes.length < numSuffixBytes) {
                suffixBytes = new byte[ArrayUtil.oversize(numSuffixBytes, 1)];
            }
            try {
                compressionAlg = CompressionAlgorithm.byCode((int) codeL & 0x03);
            } catch (IllegalArgumentException e) {
                throw new CorruptIndexException(e.getMessage(), ste.in, e);
            }
            compressionAlg.read(ste.in, suffixBytes, numSuffixBytes);
            suffixesReader.reset(suffixBytes, 0, numSuffixBytes);

            int numSuffixLengthBytes = ste.in.readVInt();
            final boolean allEqual = (numSuffixLengthBytes & 0x01) != 0;
            numSuffixLengthBytes >>>= 1;
            if (suffixLengthBytes.length < numSuffixLengthBytes) {
                suffixLengthBytes = new byte[ArrayUtil.oversize(numSuffixLengthBytes, 1)];
            }
            if (allEqual) {
                Arrays.fill(suffixLengthBytes, 0, numSuffixLengthBytes, ste.in.readByte());
            } else {
                ste.in.readBytes(suffixLengthBytes, 0, numSuffixLengthBytes);
            }
            suffixLengthsReader.reset(suffixLengthBytes, 0, numSuffixLengthBytes);
        } else {
            code = ste.in.readVInt();
            isLeafBlock = (code & 1) != 0;
            int numBytes = code >>> 1;
            if (suffixBytes.length < numBytes) {
                suffixBytes = new byte[ArrayUtil.oversize(numBytes, 1)];
            }
            ste.in.readBytes(suffixBytes, 0, numBytes);
            suffixesReader.reset(suffixBytes, 0, numBytes);
        }
        totalSuffixBytes = ste.in.getFilePointer() - startSuffixFP;

        int numBytes = ste.in.readVInt();
        if (statBytes.length < numBytes) {
            statBytes = new byte[ArrayUtil.oversize(numBytes, 1)];
        }
        ste.in.readBytes(statBytes, 0, numBytes);
        statsReader.reset(statBytes, 0, numBytes);
        statsSingletonRunLength = 0;
        metaDataUpto = 0;

        state.termBlockOrd = 0;
        nextEnt = 0;
        lastSubFP = -1;

        numBytes = ste.in.readVInt();
        if (bytes.length < numBytes) {
            bytes = new byte[ArrayUtil.oversize(numBytes, 1)];
        }
        ste.in.readBytes(bytes, 0, numBytes);
        bytesReader.reset(bytes, 0, numBytes);

        fpEnd = ste.in.getFilePointer();
    }

    void rewind() {

        fp = fpOrig;
        nextEnt = -1;
        hasTerms = hasTermsOrig;
        if (isFloor) {
            floorDataReader.rewind();
            numFollowFloorBlocks = floorDataReader.readVInt();
            assert numFollowFloorBlocks > 0;
            nextFloorLabel = floorDataReader.readByte() & 0xff;
        }
    }

    public boolean next() throws IOException {
        if (isLeafBlock) {
            nextLeaf();
            return false;
        } else {
            return nextNonLeaf();
        }
    }

    public void nextLeaf() {
        assert nextEnt != -1 && nextEnt < entCount : "nextEnt=" + nextEnt + " entCount=" + entCount + " fp=" + fp;
        nextEnt++;
        suffix = suffixLengthsReader.readVInt();
        startBytePos = suffixesReader.getPosition();
        ste.term.setLength(prefix + suffix);
        ste.term.grow(ste.term.length());
        suffixesReader.readBytes(ste.term.bytes(), prefix, suffix);
        ste.termExists = true;
    }

    public boolean nextNonLeaf() throws IOException {
        while (true) {
            if (nextEnt == entCount) {
                assert arc == null || (isFloor && isLastInFloor == false) : "isFloor=" + isFloor + " isLastInFloor=" + isLastInFloor;
                loadNextFloorBlock();
                if (isLeafBlock) {
                    nextLeaf();
                    return false;
                } else {
                    continue;
                }
            }

            assert nextEnt != -1 && nextEnt < entCount : "nextEnt=" + nextEnt + " entCount=" + entCount + " fp=" + fp;
            nextEnt++;
            final int code = suffixLengthsReader.readVInt();
            suffix = code >>> 1;
            startBytePos = suffixesReader.getPosition();
            ste.term.setLength(prefix + suffix);
            ste.term.grow(ste.term.length());
            suffixesReader.readBytes(ste.term.bytes(), prefix, suffix);
            if ((code & 1) == 0) {
                ste.termExists = true;
                subCode = 0;
                state.termBlockOrd++;
                return false;
            } else {
                ste.termExists = false;
                subCode = suffixLengthsReader.readVLong();
                lastSubFP = fp - subCode;
                return true;
            }
        }
    }

    public void scanToFloorFrame(BytesRef target) {

        if (isFloor == false || target.length <= prefix) {
            return;
        }

        final int targetLabel = target.bytes[target.offset + prefix] & 0xFF;


        if (targetLabel < nextFloorLabel) {
            return;
        }

        assert numFollowFloorBlocks != 0;

        long newFP = fpOrig;
        while (true) {
            final long code = floorDataReader.readVLong();
            newFP = fpOrig + (code >>> 1);
            hasTerms = (code & 1) != 0;

            isLastInFloor = numFollowFloorBlocks == 1;
            numFollowFloorBlocks--;

            if (isLastInFloor) {
                nextFloorLabel = 256;
                break;
            } else {
                nextFloorLabel = floorDataReader.readByte() & 0xff;
                if (targetLabel < nextFloorLabel) {
                    break;
                }
            }
        }

        if (newFP != fp) {
            nextEnt = -1;
            fp = newFP;
        } else {
        }
    }

    public void decodeMetaData() throws IOException {


        final int limit = getTermBlockOrd();
        boolean absolute = metaDataUpto == 0;
        assert limit > 0;

        while (metaDataUpto < limit) {



            if (version >= Lucene40BlockTreeTermsReader.VERSION_COMPRESSED_SUFFIXES) {
                if (statsSingletonRunLength > 0) {
                    state.docFreq = 1;
                    state.totalTermFreq = 1;
                    statsSingletonRunLength--;
                } else {
                    int token = statsReader.readVInt();
                    if ((token & 1) == 1) {
                        state.docFreq = 1;
                        state.totalTermFreq = 1;
                        statsSingletonRunLength = token >>> 1;
                    } else {
                        state.docFreq = token >>> 1;
                        if (ste.fr.fieldInfo.getIndexOptions() == IndexOptions.DOCS) {
                            state.totalTermFreq = state.docFreq;
                        } else {
                            state.totalTermFreq = state.docFreq + statsReader.readVLong();
                        }
                    }
                }
            } else {
                assert statsSingletonRunLength == 0;
                state.docFreq = statsReader.readVInt();
                if (ste.fr.fieldInfo.getIndexOptions() == IndexOptions.DOCS) {
                    state.totalTermFreq = state.docFreq; 
                } else {
                    state.totalTermFreq = state.docFreq + statsReader.readVLong();
                }
            }

            ste.fr.parent.postingsReader.decodeTerm(bytesReader, ste.fr.fieldInfo, state, absolute);

            metaDataUpto++;
            absolute = false;
        }
        state.termBlockOrd = metaDataUpto;
    }

    private boolean prefixMatches(BytesRef target) {
        for (int bytePos = 0; bytePos < prefix; bytePos++) {
            if (target.bytes[target.offset + bytePos] != ste.term.byteAt(bytePos)) {
                return false;
            }
        }

        return true;
    }

    public void scanToSubBlock(long subFP) {
        assert isLeafBlock == false;
        if (lastSubFP == subFP) {
            return;
        }
        assert subFP < fp : "fp=" + fp + " subFP=" + subFP;
        final long targetSubCode = fp - subFP;
        while (true) {
            assert nextEnt < entCount;
            nextEnt++;
            final int code = suffixLengthsReader.readVInt();
            suffixesReader.skipBytes(code >>> 1);
            if ((code & 1) != 0) {
                final long subCode = suffixLengthsReader.readVLong();
                if (targetSubCode == subCode) {
                    lastSubFP = subFP;
                    return;
                }
            } else {
                state.termBlockOrd++;
            }
        }
    }

    public SeekStatus scanToTerm(BytesRef target, boolean exactOnly) throws IOException {
        return isLeafBlock ? scanToTermLeaf(target, exactOnly) : scanToTermNonLeaf(target, exactOnly);
    }

    private int startBytePos;
    private int suffix;
    private long subCode;
    CompressionAlgorithm compressionAlg = CompressionAlgorithm.NO_COMPRESSION;

    /*
    @SuppressWarnings("unused")
    static String brToString(BytesRef b) {
    try {
      return b.utf8ToString() + " " + b;
    } catch (Throwable t) {
      return b.toString();
    }
    }
    */

    public SeekStatus scanToTermLeaf(BytesRef target, boolean exactOnly) throws IOException {


        assert nextEnt != -1;

        ste.termExists = true;
        subCode = 0;

        if (nextEnt == entCount) {
            if (exactOnly) {
                fillTerm();
            }
            return SeekStatus.END;
        }

        assert prefixMatches(target);

        do {
            nextEnt++;

            suffix = suffixLengthsReader.readVInt();


            startBytePos = suffixesReader.getPosition();
            suffixesReader.skipBytes(suffix);

            final int cmp = Arrays.compareUnsigned(
                suffixBytes,
                startBytePos,
                startBytePos + suffix,
                target.bytes,
                target.offset + prefix,
                target.offset + target.length
            );

            if (cmp < 0) {
            } else if (cmp > 0) {
                fillTerm();

                return SeekStatus.NOT_FOUND;
            } else {


                assert ste.termExists;
                fillTerm();
                return SeekStatus.FOUND;
            }
        } while (nextEnt < entCount);

        if (exactOnly) {
            fillTerm();
        }

        return SeekStatus.END;
    }

    public SeekStatus scanToTermNonLeaf(BytesRef target, boolean exactOnly) throws IOException {


        assert nextEnt != -1;

        if (nextEnt == entCount) {
            if (exactOnly) {
                fillTerm();
                ste.termExists = subCode == 0;
            }
            return SeekStatus.END;
        }

        assert prefixMatches(target);

        while (nextEnt < entCount) {

            nextEnt++;

            final int code = suffixLengthsReader.readVInt();
            suffix = code >>> 1;


            final int termLen = prefix + suffix;
            startBytePos = suffixesReader.getPosition();
            suffixesReader.skipBytes(suffix);
            ste.termExists = (code & 1) == 0;
            if (ste.termExists) {
                state.termBlockOrd++;
                subCode = 0;
            } else {
                subCode = suffixLengthsReader.readVLong();
                lastSubFP = fp - subCode;
            }

            final int cmp = Arrays.compareUnsigned(
                suffixBytes,
                startBytePos,
                startBytePos + suffix,
                target.bytes,
                target.offset + prefix,
                target.offset + target.length
            );

            if (cmp < 0) {
            } else if (cmp > 0) {
                fillTerm();


                if (exactOnly == false && ste.termExists == false) {
                    ste.currentFrame = ste.pushFrame(null, ste.currentFrame.lastSubFP, termLen);
                    ste.currentFrame.loadBlock();
                    while (ste.currentFrame.next()) {
                        ste.currentFrame = ste.pushFrame(null, ste.currentFrame.lastSubFP, ste.term.length());
                        ste.currentFrame.loadBlock();
                    }
                }

                return SeekStatus.NOT_FOUND;
            } else {


                assert ste.termExists;
                fillTerm();
                return SeekStatus.FOUND;
            }
        }

        if (exactOnly) {
            fillTerm();
        }

        return SeekStatus.END;
    }

    private void fillTerm() {
        final int termLength = prefix + suffix;
        ste.term.setLength(termLength);
        ste.term.grow(termLength);
        System.arraycopy(suffixBytes, startBytePos, ste.term.bytes(), prefix, suffix);
    }
}
