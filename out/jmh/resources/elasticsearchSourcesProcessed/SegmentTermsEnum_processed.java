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
import org.apache.lucene.index.BaseTermsEnum;
import org.apache.lucene.index.ImpactsEnum;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermState;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.FST;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.Util;

import java.io.IOException;
import java.io.PrintStream;

/** Iterates through terms in this field. */
final class SegmentTermsEnum extends BaseTermsEnum {

    IndexInput in;

    private SegmentTermsEnumFrame[] stack;
    private final SegmentTermsEnumFrame staticFrame;
    SegmentTermsEnumFrame currentFrame;
    boolean termExists;
    final FieldReader fr;

    private int targetBeforeCurrentLength;


    private final ByteArrayDataInput scratchReader = new ByteArrayDataInput();

    private int validIndexPrefix;

    private boolean eof;

    final BytesRefBuilder term = new BytesRefBuilder();
    private final FST.BytesReader fstReader;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private FST.Arc<BytesRef>[] arcs = new FST.Arc[1];

    SegmentTermsEnum(FieldReader fr) throws IOException {
        this.fr = fr;

        stack = new SegmentTermsEnumFrame[0];

        staticFrame = new SegmentTermsEnumFrame(this, -1);

        if (fr.index == null) {
            fstReader = null;
        } else {
            fstReader = fr.index.getBytesReader();
        }

        for (int arcIdx = 0; arcIdx < arcs.length; arcIdx++) {
            arcs[arcIdx] = new FST.Arc<>();
        }

        currentFrame = staticFrame;
        final FST.Arc<BytesRef> arc;
        if (fr.index != null) {
            arc = fr.index.getFirstArc(arcs[0]);
            assert arc.isFinal();
        } else {
            arc = null;
        }
        validIndexPrefix = 0;

    }

    void initIndexInput() {
        if (this.in == null) {
            this.in = fr.parent.termsIn.clone();
        }
    }

    /** Runs next() through the entire terms dict, computing aggregate statistics. */
    public Stats computeBlockStats() throws IOException {

        Stats stats = new Stats(fr.parent.segment, fr.fieldInfo.name);
        if (fr.index != null) {
            stats.indexNumBytes = fr.index.ramBytesUsed();
        }

        currentFrame = staticFrame;
        FST.Arc<BytesRef> arc;
        if (fr.index != null) {
            arc = fr.index.getFirstArc(arcs[0]);
            assert arc.isFinal();
        } else {
            arc = null;
        }

        currentFrame = pushFrame(arc, fr.rootCode, 0);
        currentFrame.fpOrig = currentFrame.fp;
        currentFrame.loadBlock();
        validIndexPrefix = 0;

        stats.startBlock(currentFrame, currentFrame.isLastInFloor == false);

        allTerms: while (true) {

            while (currentFrame.nextEnt == currentFrame.entCount) {
                stats.endBlock(currentFrame);
                if (currentFrame.isLastInFloor == false) {
                    currentFrame.loadNextFloorBlock();
                    stats.startBlock(currentFrame, true);
                    break;
                } else {
                    if (currentFrame.ord == 0) {
                        break allTerms;
                    }
                    final long lastFP = currentFrame.fpOrig;
                    currentFrame = stack[currentFrame.ord - 1];
                    assert lastFP == currentFrame.lastSubFP;
                }
            }

            while (true) {
                if (currentFrame.next()) {
                    currentFrame = pushFrame(null, currentFrame.lastSubFP, term.length());
                    currentFrame.fpOrig = currentFrame.fp;
                    currentFrame.loadBlock();
                    stats.startBlock(currentFrame, currentFrame.isLastInFloor == false);
                } else {
                    stats.term(term.get());
                    break;
                }
            }
        }

        stats.finish();

        currentFrame = staticFrame;
        if (fr.index != null) {
            arc = fr.index.getFirstArc(arcs[0]);
            assert arc.isFinal();
        } else {
            arc = null;
        }
        currentFrame = pushFrame(arc, fr.rootCode, 0);
        currentFrame.rewind();
        currentFrame.loadBlock();
        validIndexPrefix = 0;
        term.clear();

        return stats;
    }

    private SegmentTermsEnumFrame getFrame(int ord) throws IOException {
        if (ord >= stack.length) {
            final SegmentTermsEnumFrame[] next = new SegmentTermsEnumFrame[ArrayUtil.oversize(
                1 + ord,
                RamUsageEstimator.NUM_BYTES_OBJECT_REF
            )];
            System.arraycopy(stack, 0, next, 0, stack.length);
            for (int stackOrd = stack.length; stackOrd < next.length; stackOrd++) {
                next[stackOrd] = new SegmentTermsEnumFrame(this, stackOrd);
            }
            stack = next;
        }
        assert stack[ord].ord == ord;
        return stack[ord];
    }

    private FST.Arc<BytesRef> getArc(int ord) {
        if (ord >= arcs.length) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            final FST.Arc<BytesRef>[] next = new FST.Arc[ArrayUtil.oversize(1 + ord, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
            System.arraycopy(arcs, 0, next, 0, arcs.length);
            for (int arcOrd = arcs.length; arcOrd < next.length; arcOrd++) {
                next[arcOrd] = new FST.Arc<>();
            }
            arcs = next;
        }
        return arcs[ord];
    }

    SegmentTermsEnumFrame pushFrame(FST.Arc<BytesRef> arc, BytesRef frameData, int length) throws IOException {
        scratchReader.reset(frameData.bytes, frameData.offset, frameData.length);
        final long code = scratchReader.readVLong();
        final long fpSeek = code >>> Lucene40BlockTreeTermsReader.OUTPUT_FLAGS_NUM_BITS;
        final SegmentTermsEnumFrame f = getFrame(1 + currentFrame.ord);
        f.hasTerms = (code & Lucene40BlockTreeTermsReader.OUTPUT_FLAG_HAS_TERMS) != 0;
        f.hasTermsOrig = f.hasTerms;
        f.isFloor = (code & Lucene40BlockTreeTermsReader.OUTPUT_FLAG_IS_FLOOR) != 0;
        if (f.isFloor) {
            f.setFloorData(scratchReader, frameData);
        }
        pushFrame(arc, fpSeek, length);

        return f;
    }

    SegmentTermsEnumFrame pushFrame(FST.Arc<BytesRef> arc, long fp, int length) throws IOException {
        final SegmentTermsEnumFrame f = getFrame(1 + currentFrame.ord);
        f.arc = arc;
        if (f.fpOrig == fp && f.nextEnt != -1) {
            if (f.ord > targetBeforeCurrentLength) {
                f.rewind();
            } else {
            }
            assert length == f.prefix;
        } else {
            f.nextEnt = -1;
            f.prefix = length;
            f.state.termBlockOrd = 0;
            f.fpOrig = f.fp = fp;
            f.lastSubFP = -1;
        }

        return f;
    }

    private boolean clearEOF() {
        eof = false;
        return true;
    }

    private boolean setEOF() {
        eof = true;
        return true;
    }

    /*
    @SuppressWarnings("unused")
    static String brToString(BytesRef b) {
    try {
      return b.utf8ToString() + " " + b;
    } catch (Throwable t) {
      return b.toString();
    }
    }

    @SuppressWarnings("unused")
    static String brToString(BytesRefBuilder b) {
    return brToString(b.get());
    }
    */

    @Override
    public boolean seekExact(BytesRef target) throws IOException {

        if (fr.index == null) {
            throw new IllegalStateException("terms index was not loaded");
        }

        if (fr.size() > 0 && (target.compareTo(fr.getMin()) < 0 || target.compareTo(fr.getMax()) > 0)) {
            return false;
        }

        term.grow(1 + target.length);

        assert clearEOF();


        FST.Arc<BytesRef> arc;
        int targetUpto;
        BytesRef output;

        targetBeforeCurrentLength = currentFrame.ord;

        if (currentFrame != staticFrame) {



            arc = arcs[0];
            assert arc.isFinal();
            output = arc.output();
            targetUpto = 0;

            SegmentTermsEnumFrame lastFrame = stack[0];
            assert validIndexPrefix <= term.length();

            final int targetLimit = Math.min(target.length, validIndexPrefix);

            int cmp = 0;


            while (targetUpto < targetLimit) {
                cmp = (term.byteAt(targetUpto) & 0xFF) - (target.bytes[target.offset + targetUpto] & 0xFF);
                if (cmp != 0) {
                    break;
                }
                arc = arcs[1 + targetUpto];
                assert arc.label() == (target.bytes[target.offset + targetUpto] & 0xFF)
                    : "arc.label=" + (char) arc.label() + " targetLabel=" + (char) (target.bytes[target.offset + targetUpto] & 0xFF);
                if (arc.output() != Lucene40BlockTreeTermsReader.NO_OUTPUT) {
                    output = Lucene40BlockTreeTermsReader.FST_OUTPUTS.add(output, arc.output());
                }
                if (arc.isFinal()) {
                    lastFrame = stack[1 + lastFrame.ord];
                }
                targetUpto++;
            }

            if (cmp == 0) {
                final int targetUptoMid = targetUpto;

                final int targetLimit2 = Math.min(target.length, term.length());
                while (targetUpto < targetLimit2) {
                    cmp = (term.byteAt(targetUpto) & 0xFF) - (target.bytes[target.offset + targetUpto] & 0xFF);
                    if (cmp != 0) {
                        break;
                    }
                    targetUpto++;
                }

                if (cmp == 0) {
                    cmp = term.length() - target.length;
                }
                targetUpto = targetUptoMid;
            }

            if (cmp < 0) {
                currentFrame = lastFrame;

            } else if (cmp > 0) {
                targetBeforeCurrentLength = lastFrame.ord;
                currentFrame = lastFrame;
                currentFrame.rewind();
            } else {
                assert term.length() == target.length;
                if (termExists) {
                    return true;
                } else {
                }
            }

        } else {

            targetBeforeCurrentLength = -1;
            arc = fr.index.getFirstArc(arcs[0]);

            assert arc.isFinal();
            assert arc.output() != null;


            output = arc.output();

            currentFrame = staticFrame;

            targetUpto = 0;
            currentFrame = pushFrame(arc, Lucene40BlockTreeTermsReader.FST_OUTPUTS.add(output, arc.nextFinalOutput()), 0);
        }


        while (targetUpto < target.length) {

            final int targetLabel = target.bytes[target.offset + targetUpto] & 0xFF;

            final FST.Arc<BytesRef> nextArc = fr.index.findTargetArc(targetLabel, arc, getArc(1 + targetUpto), fstReader);

            if (nextArc == null) {


                validIndexPrefix = currentFrame.prefix;

                currentFrame.scanToFloorFrame(target);

                if (currentFrame.hasTerms == false) {
                    termExists = false;
                    term.setByteAt(targetUpto, (byte) targetLabel);
                    term.setLength(1 + targetUpto);
                    return false;
                }

                currentFrame.loadBlock();

                final SeekStatus result = currentFrame.scanToTerm(target, true);
                if (result == SeekStatus.FOUND) {
                    return true;
                } else {
                    return false;
                }
            } else {
                arc = nextArc;
                term.setByteAt(targetUpto, (byte) targetLabel);
                assert arc.output() != null;
                if (arc.output() != Lucene40BlockTreeTermsReader.NO_OUTPUT) {
                    output = Lucene40BlockTreeTermsReader.FST_OUTPUTS.add(output, arc.output());
                }

                targetUpto++;

                if (arc.isFinal()) {
                    currentFrame = pushFrame(arc, Lucene40BlockTreeTermsReader.FST_OUTPUTS.add(output, arc.nextFinalOutput()), targetUpto);
                }
            }
        }

        validIndexPrefix = currentFrame.prefix;

        currentFrame.scanToFloorFrame(target);

        if (currentFrame.hasTerms == false) {
            termExists = false;
            term.setLength(targetUpto);
            return false;
        }

        currentFrame.loadBlock();

        final SeekStatus result = currentFrame.scanToTerm(target, true);
        if (result == SeekStatus.FOUND) {
            return true;
        } else {

            return false;
        }
    }

    @Override
    public SeekStatus seekCeil(BytesRef target) throws IOException {

        if (fr.index == null) {
            throw new IllegalStateException("terms index was not loaded");
        }

        term.grow(1 + target.length);

        assert clearEOF();


        FST.Arc<BytesRef> arc;
        int targetUpto;
        BytesRef output;

        targetBeforeCurrentLength = currentFrame.ord;

        if (currentFrame != staticFrame) {



            arc = arcs[0];
            assert arc.isFinal();
            output = arc.output();
            targetUpto = 0;

            SegmentTermsEnumFrame lastFrame = stack[0];
            assert validIndexPrefix <= term.length();

            final int targetLimit = Math.min(target.length, validIndexPrefix);

            int cmp = 0;


            while (targetUpto < targetLimit) {
                cmp = (term.byteAt(targetUpto) & 0xFF) - (target.bytes[target.offset + targetUpto] & 0xFF);
                if (cmp != 0) {
                    break;
                }
                arc = arcs[1 + targetUpto];
                assert arc.label() == (target.bytes[target.offset + targetUpto] & 0xFF)
                    : "arc.label=" + (char) arc.label() + " targetLabel=" + (char) (target.bytes[target.offset + targetUpto] & 0xFF);
                if (arc.output() != Lucene40BlockTreeTermsReader.NO_OUTPUT) {
                    output = Lucene40BlockTreeTermsReader.FST_OUTPUTS.add(output, arc.output());
                }
                if (arc.isFinal()) {
                    lastFrame = stack[1 + lastFrame.ord];
                }
                targetUpto++;
            }

            if (cmp == 0) {
                final int targetUptoMid = targetUpto;
                final int targetLimit2 = Math.min(target.length, term.length());
                while (targetUpto < targetLimit2) {
                    cmp = (term.byteAt(targetUpto) & 0xFF) - (target.bytes[target.offset + targetUpto] & 0xFF);
                    if (cmp != 0) {
                        break;
                    }
                    targetUpto++;
                }

                if (cmp == 0) {
                    cmp = term.length() - target.length;
                }
                targetUpto = targetUptoMid;
            }

            if (cmp < 0) {
                currentFrame = lastFrame;

            } else if (cmp > 0) {
                targetBeforeCurrentLength = 0;
                currentFrame = lastFrame;
                currentFrame.rewind();
            } else {
                assert term.length() == target.length;
                if (termExists) {
                    return SeekStatus.FOUND;
                } else {
                }
            }

        } else {

            targetBeforeCurrentLength = -1;
            arc = fr.index.getFirstArc(arcs[0]);

            assert arc.isFinal();
            assert arc.output() != null;


            output = arc.output();

            currentFrame = staticFrame;

            targetUpto = 0;
            currentFrame = pushFrame(arc, Lucene40BlockTreeTermsReader.FST_OUTPUTS.add(output, arc.nextFinalOutput()), 0);
        }


        while (targetUpto < target.length) {

            final int targetLabel = target.bytes[target.offset + targetUpto] & 0xFF;

            final FST.Arc<BytesRef> nextArc = fr.index.findTargetArc(targetLabel, arc, getArc(1 + targetUpto), fstReader);

            if (nextArc == null) {


                validIndexPrefix = currentFrame.prefix;

                currentFrame.scanToFloorFrame(target);

                currentFrame.loadBlock();

                final SeekStatus result = currentFrame.scanToTerm(target, false);
                if (result == SeekStatus.END) {
                    term.copyBytes(target);
                    termExists = false;

                    if (next() != null) {
                        return SeekStatus.NOT_FOUND;
                    } else {
                        return SeekStatus.END;
                    }
                } else {
                    return result;
                }
            } else {
                term.setByteAt(targetUpto, (byte) targetLabel);
                arc = nextArc;
                assert arc.output() != null;
                if (arc.output() != Lucene40BlockTreeTermsReader.NO_OUTPUT) {
                    output = Lucene40BlockTreeTermsReader.FST_OUTPUTS.add(output, arc.output());
                }

                targetUpto++;

                if (arc.isFinal()) {
                    currentFrame = pushFrame(arc, Lucene40BlockTreeTermsReader.FST_OUTPUTS.add(output, arc.nextFinalOutput()), targetUpto);
                }
            }
        }

        validIndexPrefix = currentFrame.prefix;

        currentFrame.scanToFloorFrame(target);

        currentFrame.loadBlock();

        final SeekStatus result = currentFrame.scanToTerm(target, false);

        if (result == SeekStatus.END) {
            term.copyBytes(target);
            termExists = false;
            if (next() != null) {
                return SeekStatus.NOT_FOUND;
            } else {
                return SeekStatus.END;
            }
        } else {
            return result;
        }
    }

    @SuppressWarnings("unused")
    private void printSeekState(PrintStream out) throws IOException {
        if (currentFrame == staticFrame) {
            out.println("  no prior seek");
        } else {
            out.println("  prior seek state:");
            int ord = 0;
            boolean isSeekFrame = true;
            while (true) {
                SegmentTermsEnumFrame f = getFrame(ord);
                assert f != null;
                final BytesRef prefix = new BytesRef(term.get().bytes, 0, f.prefix);
                if (f.nextEnt == -1) {
                    out.println(
                        "    frame "
                            + (isSeekFrame ? "(seek)" : "(next)")
                            + " ord="
                            + ord
                            + " fp="
                            + f.fp
                            + (f.isFloor ? (" (fpOrig=" + f.fpOrig + ")") : "")
                            + " prefixLen="
                            + f.prefix
                            + " prefix="
                            + prefix
                            + (f.nextEnt == -1 ? "" : (" (of " + f.entCount + ")"))
                            + " hasTerms="
                            + f.hasTerms
                            + " isFloor="
                            + f.isFloor
                            + " code="
                            + ((f.fp << Lucene40BlockTreeTermsReader.OUTPUT_FLAGS_NUM_BITS) + (f.hasTerms
                                ? Lucene40BlockTreeTermsReader.OUTPUT_FLAG_HAS_TERMS
                                : 0) + (f.isFloor ? Lucene40BlockTreeTermsReader.OUTPUT_FLAG_IS_FLOOR : 0))
                            + " isLastInFloor="
                            + f.isLastInFloor
                            + " mdUpto="
                            + f.metaDataUpto
                            + " tbOrd="
                            + f.getTermBlockOrd()
                    );
                } else {
                    out.println(
                        "    frame "
                            + (isSeekFrame ? "(seek, loaded)" : "(next, loaded)")
                            + " ord="
                            + ord
                            + " fp="
                            + f.fp
                            + (f.isFloor ? (" (fpOrig=" + f.fpOrig + ")") : "")
                            + " prefixLen="
                            + f.prefix
                            + " prefix="
                            + prefix
                            + " nextEnt="
                            + f.nextEnt
                            + (f.nextEnt == -1 ? "" : (" (of " + f.entCount + ")"))
                            + " hasTerms="
                            + f.hasTerms
                            + " isFloor="
                            + f.isFloor
                            + " code="
                            + ((f.fp << Lucene40BlockTreeTermsReader.OUTPUT_FLAGS_NUM_BITS) + (f.hasTerms
                                ? Lucene40BlockTreeTermsReader.OUTPUT_FLAG_HAS_TERMS
                                : 0) + (f.isFloor ? Lucene40BlockTreeTermsReader.OUTPUT_FLAG_IS_FLOOR : 0))
                            + " lastSubFP="
                            + f.lastSubFP
                            + " isLastInFloor="
                            + f.isLastInFloor
                            + " mdUpto="
                            + f.metaDataUpto
                            + " tbOrd="
                            + f.getTermBlockOrd()
                    );
                }
                if (fr.index != null) {
                    assert isSeekFrame == false || f.arc != null : "isSeekFrame=" + isSeekFrame + " f.arc=" + f.arc;
                    if (f.prefix > 0 && isSeekFrame && f.arc.label() != (term.byteAt(f.prefix - 1) & 0xFF)) {
                        out.println(
                            "      broken seek state: arc.label="
                                + (char) f.arc.label()
                                + " vs term byte="
                                + (char) (term.byteAt(f.prefix - 1) & 0xFF)
                        );
                        throw new RuntimeException("seek state is broken");
                    }
                    BytesRef output = Util.get(fr.index, prefix);
                    if (output == null) {
                        out.println("      broken seek state: prefix is not final in index");
                        throw new RuntimeException("seek state is broken");
                    } else if (isSeekFrame && f.isFloor == false) {
                        final ByteArrayDataInput reader = new ByteArrayDataInput(output.bytes, output.offset, output.length);
                        final long codeOrig = reader.readVLong();
                        final long code = (f.fp << Lucene40BlockTreeTermsReader.OUTPUT_FLAGS_NUM_BITS) | (f.hasTerms
                            ? Lucene40BlockTreeTermsReader.OUTPUT_FLAG_HAS_TERMS
                            : 0) | (f.isFloor ? Lucene40BlockTreeTermsReader.OUTPUT_FLAG_IS_FLOOR : 0);
                        if (codeOrig != code) {
                            out.println("      broken seek state: output code=" + codeOrig + " doesn't match frame code=" + code);
                            throw new RuntimeException("seek state is broken");
                        }
                    }
                }
                if (f == currentFrame) {
                    break;
                }
                if (f.prefix == validIndexPrefix) {
                    isSeekFrame = false;
                }
                ord++;
            }
        }
    }

    /* Decodes only the term bytes of the next term.  If caller then asks for
    metadata, ie docFreq, totalTermFreq or pulls a D/&PEnum, we then (lazily)
    decode all metadata up to the current term. */
    @Override
    public BytesRef next() throws IOException {
        if (in == null) {
            final FST.Arc<BytesRef> arc;
            if (fr.index != null) {
                arc = fr.index.getFirstArc(arcs[0]);
                assert arc.isFinal();
            } else {
                arc = null;
            }
            currentFrame = pushFrame(arc, fr.rootCode, 0);
            currentFrame.loadBlock();
        }

        targetBeforeCurrentLength = currentFrame.ord;

        assert eof == false;

        if (currentFrame == staticFrame) {
            final boolean result = seekExact(term.get());
            assert result;
        }

        while (currentFrame.nextEnt == currentFrame.entCount) {
            if (currentFrame.isLastInFloor == false) {
                currentFrame.loadNextFloorBlock();
                break;
            } else {
                if (currentFrame.ord == 0) {
                    assert setEOF();
                    term.clear();
                    validIndexPrefix = 0;
                    currentFrame.rewind();
                    termExists = false;
                    return null;
                }
                final long lastFP = currentFrame.fpOrig;
                currentFrame = stack[currentFrame.ord - 1];

                if (currentFrame.nextEnt == -1 || currentFrame.lastSubFP != lastFP) {
                    currentFrame.scanToFloorFrame(term.get());
                    currentFrame.loadBlock();
                    currentFrame.scanToSubBlock(lastFP);
                }

                validIndexPrefix = Math.min(validIndexPrefix, currentFrame.prefix);
            }
        }

        while (true) {
            if (currentFrame.next()) {
                currentFrame = pushFrame(null, currentFrame.lastSubFP, term.length());
                currentFrame.loadBlock();
            } else {
                return term.get();
            }
        }
    }

    @Override
    public BytesRef term() {
        assert eof == false;
        return term.get();
    }

    @Override
    public int docFreq() throws IOException {
        assert eof == false;
        currentFrame.decodeMetaData();
        return currentFrame.state.docFreq;
    }

    @Override
    public long totalTermFreq() throws IOException {
        assert eof == false;
        currentFrame.decodeMetaData();
        return currentFrame.state.totalTermFreq;
    }

    @Override
    public PostingsEnum postings(PostingsEnum reuse, int flags) throws IOException {
        assert eof == false;
        currentFrame.decodeMetaData();
        return fr.parent.postingsReader.postings(fr.fieldInfo, currentFrame.state, reuse, flags);
    }

    @Override
    public ImpactsEnum impacts(int flags) throws IOException {
        assert eof == false;
        currentFrame.decodeMetaData();
        return fr.parent.postingsReader.impacts(fr.fieldInfo, currentFrame.state, flags);
    }

    @Override
    public void seekExact(BytesRef target, TermState otherState) {
        assert clearEOF();
        if (target.compareTo(term.get()) != 0 || termExists == false) {
            assert otherState != null && otherState instanceof BlockTermState;
            currentFrame = staticFrame;
            currentFrame.state.copyFrom(otherState);
            term.copyBytes(target);
            currentFrame.metaDataUpto = currentFrame.getTermBlockOrd();
            assert currentFrame.metaDataUpto > 0;
            validIndexPrefix = 0;
        } else {
        }
    }

    @Override
    public TermState termState() throws IOException {
        assert eof == false;
        currentFrame.decodeMetaData();
        TermState ts = currentFrame.state.clone();
        return ts;
    }

    @Override
    public void seekExact(long ord) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long ord() {
        throw new UnsupportedOperationException();
    }
}
