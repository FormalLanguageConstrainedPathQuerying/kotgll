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
package org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst;

import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.FST.Arc.BitTable;

import java.io.IOException;

/**
 * Can next() and advance() through the terms in an FST
 */
abstract class FSTEnum<T> {
    protected final FST<T> fst;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected FST.Arc<T>[] arcs = new FST.Arc[10];
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected T[] output = (T[]) new Object[10];

    protected final T NO_OUTPUT;
    protected final FST.BytesReader fstReader;

    protected int upto;
    int targetLength;

    /**
     * doFloor controls the behavior of advance: if it's true doFloor is true, advance positions to
     * the biggest term before target.
     */
    FSTEnum(FST<T> fst) {
        this.fst = fst;
        fstReader = fst.getBytesReader();
        NO_OUTPUT = fst.outputs.getNoOutput();
        fst.getFirstArc(getArc(0));
        output[0] = NO_OUTPUT;
    }

    protected abstract int getTargetLabel();

    protected abstract int getCurrentLabel();

    protected abstract void setCurrentLabel(int label);

    protected abstract void grow();

    /** Rewinds enum state to match the shared prefix between current term and target term */
    private void rewindPrefix() throws IOException {
        if (upto == 0) {
            upto = 1;
            fst.readFirstTargetArc(getArc(0), getArc(1), fstReader);
            return;
        }

        final int currentLimit = upto;
        upto = 1;
        while (upto < currentLimit && upto <= targetLength + 1) {
            final int cmp = getCurrentLabel() - getTargetLabel();
            if (cmp < 0) {
                break;
            } else if (cmp > 0) {
                final FST.Arc<T> arc = getArc(upto);
                fst.readFirstTargetArc(getArc(upto - 1), arc, fstReader);
                break;
            }
            upto++;
        }
    }

    protected void doNext() throws IOException {
        if (upto == 0) {
            upto = 1;
            fst.readFirstTargetArc(getArc(0), getArc(1), fstReader);
        } else {
            while (arcs[upto].isLast()) {
                upto--;
                if (upto == 0) {
                    return;
                }
            }
            fst.readNextArc(arcs[upto], fstReader);
        }

        pushFirst();
    }


    /** Seeks to smallest term that's &gt;= target. */
    protected void doSeekCeil() throws IOException {




        rewindPrefix();

        FST.Arc<T> arc = getArc(upto);

        while (arc != null) {
            int targetLabel = getTargetLabel();
            if (arc.bytesPerArc() != 0 && arc.label() != FST.END_LABEL) {
                final FST.BytesReader in = fst.getBytesReader();
                if (arc.nodeFlags() == FST.ARCS_FOR_DIRECT_ADDRESSING) {
                    arc = doSeekCeilArrayDirectAddressing(arc, targetLabel, in);
                } else {
                    assert arc.nodeFlags() == FST.ARCS_FOR_BINARY_SEARCH;
                    arc = doSeekCeilArrayPacked(arc, targetLabel, in);
                }
            } else {
                arc = doSeekCeilList(arc, targetLabel);
            }
        }
    }

    private FST.Arc<T> doSeekCeilArrayDirectAddressing(final FST.Arc<T> arc, final int targetLabel, final FST.BytesReader in)
        throws IOException {

        int targetIndex = targetLabel - arc.firstLabel();
        if (targetIndex >= arc.numArcs()) {
            upto--;
            while (true) {
                if (upto == 0) {
                    return null;
                }
                final FST.Arc<T> prevArc = getArc(upto);
                if (prevArc.isLast() == false) {
                    fst.readNextArc(prevArc, fstReader);
                    pushFirst();
                    return null;
                }
                upto--;
            }
        } else {
            if (targetIndex < 0) {
                targetIndex = -1;
            } else if (BitTable.isBitSet(targetIndex, arc, in)) {
                fst.readArcByDirectAddressing(arc, in, targetIndex);
                assert arc.label() == targetLabel;
                output[upto] = fst.outputs.add(output[upto - 1], arc.output());
                if (targetLabel == FST.END_LABEL) {
                    return null;
                }
                setCurrentLabel(arc.label());
                incr();
                return fst.readFirstTargetArc(arc, getArc(upto), fstReader);
            }
            int ceilIndex = BitTable.nextBitSet(targetIndex, arc, in);
            assert ceilIndex != -1;
            fst.readArcByDirectAddressing(arc, in, ceilIndex);
            assert arc.label() > targetLabel;
            pushFirst();
            return null;
        }
    }

    private FST.Arc<T> doSeekCeilArrayPacked(final FST.Arc<T> arc, final int targetLabel, final FST.BytesReader in) throws IOException {
        int idx = Util.binarySearch(fst, arc, targetLabel);
        if (idx >= 0) {
            fst.readArcByIndex(arc, in, idx);
            assert arc.arcIdx() == idx;
            assert arc.label() == targetLabel : "arc.label=" + arc.label() + " vs targetLabel=" + targetLabel + " mid=" + idx;
            output[upto] = fst.outputs.add(output[upto - 1], arc.output());
            if (targetLabel == FST.END_LABEL) {
                return null;
            }
            setCurrentLabel(arc.label());
            incr();
            return fst.readFirstTargetArc(arc, getArc(upto), fstReader);
        }
        idx = -1 - idx;
        if (idx == arc.numArcs()) {
            fst.readArcByIndex(arc, in, idx - 1);
            assert arc.isLast();
            upto--;
            while (true) {
                if (upto == 0) {
                    return null;
                }
                final FST.Arc<T> prevArc = getArc(upto);
                if (prevArc.isLast() == false) {
                    fst.readNextArc(prevArc, fstReader);
                    pushFirst();
                    return null;
                }
                upto--;
            }
        } else {
            fst.readArcByIndex(arc, in, idx);
            assert arc.label() > targetLabel;
            pushFirst();
            return null;
        }
    }

    private FST.Arc<T> doSeekCeilList(final FST.Arc<T> arc, final int targetLabel) throws IOException {
        if (arc.label() == targetLabel) {
            output[upto] = fst.outputs.add(output[upto - 1], arc.output());
            if (targetLabel == FST.END_LABEL) {
                return null;
            }
            setCurrentLabel(arc.label());
            incr();
            return fst.readFirstTargetArc(arc, getArc(upto), fstReader);
        } else if (arc.label() > targetLabel) {
            pushFirst();
            return null;
        } else if (arc.isLast()) {
            upto--;
            while (true) {
                if (upto == 0) {
                    return null;
                }
                final FST.Arc<T> prevArc = getArc(upto);
                if (prevArc.isLast() == false) {
                    fst.readNextArc(prevArc, fstReader);
                    pushFirst();
                    return null;
                }
                upto--;
            }
        } else {
            fst.readNextArc(arc, fstReader);
        }
        return arc;
    }

    /** Seeks to largest term that's &lt;= target. */
    void doSeekFloor() throws IOException {


        rewindPrefix();


        FST.Arc<T> arc = getArc(upto);


        while (arc != null) {
            int targetLabel = getTargetLabel();

            if (arc.bytesPerArc() != 0 && arc.label() != FST.END_LABEL) {
                final FST.BytesReader in = fst.getBytesReader();
                if (arc.nodeFlags() == FST.ARCS_FOR_DIRECT_ADDRESSING) {
                    arc = doSeekFloorArrayDirectAddressing(arc, targetLabel, in);
                } else {
                    assert arc.nodeFlags() == FST.ARCS_FOR_BINARY_SEARCH;
                    arc = doSeekFloorArrayPacked(arc, targetLabel, in);
                }
            } else {
                arc = doSeekFloorList(arc, targetLabel);
            }
        }
    }

    private FST.Arc<T> doSeekFloorArrayDirectAddressing(FST.Arc<T> arc, int targetLabel, FST.BytesReader in) throws IOException {

        int targetIndex = targetLabel - arc.firstLabel();
        if (targetIndex < 0) {
            return backtrackToFloorArc(arc, targetLabel, in);
        } else if (targetIndex >= arc.numArcs()) {
            fst.readLastArcByDirectAddressing(arc, in);
            assert arc.label() < targetLabel;
            assert arc.isLast();
            pushLast();
            return null;
        } else {
            if (BitTable.isBitSet(targetIndex, arc, in)) {
                fst.readArcByDirectAddressing(arc, in, targetIndex);
                assert arc.label() == targetLabel;
                output[upto] = fst.outputs.add(output[upto - 1], arc.output());
                if (targetLabel == FST.END_LABEL) {
                    return null;
                }
                setCurrentLabel(arc.label());
                incr();
                return fst.readFirstTargetArc(arc, getArc(upto), fstReader);
            }
            int floorIndex = BitTable.previousBitSet(targetIndex, arc, in);
            assert floorIndex != -1;
            fst.readArcByDirectAddressing(arc, in, floorIndex);
            assert arc.label() < targetLabel;
            assert arc.isLast() || fst.readNextArcLabel(arc, in) > targetLabel;
            pushLast();
            return null;
        }
    }

    /**
     * Backtracks until it finds a node which first arc is before our target label.` Then on the node,
     * finds the arc just before the targetLabel.
     *
     * @return null to continue the seek floor recursion loop.
     */
    private FST.Arc<T> backtrackToFloorArc(FST.Arc<T> arc, int targetLabel, final FST.BytesReader in) throws IOException {
        while (true) {
            fst.readFirstTargetArc(getArc(upto - 1), arc, fstReader);
            if (arc.label() < targetLabel) {
                if (arc.isLast() == false) {
                    if (arc.bytesPerArc() != 0 && arc.label() != FST.END_LABEL) {
                        if (arc.nodeFlags() == FST.ARCS_FOR_BINARY_SEARCH) {
                            findNextFloorArcBinarySearch(arc, targetLabel, in);
                        } else {
                            assert arc.nodeFlags() == FST.ARCS_FOR_DIRECT_ADDRESSING;
                            findNextFloorArcDirectAddressing(arc, targetLabel, in);
                        }
                    } else {
                        while (arc.isLast() == false && fst.readNextArcLabel(arc, in) < targetLabel) {
                            fst.readNextArc(arc, fstReader);
                        }
                    }
                }
                assert arc.label() < targetLabel;
                assert arc.isLast() || fst.readNextArcLabel(arc, in) >= targetLabel;
                pushLast();
                return null;
            }
            upto--;
            if (upto == 0) {
                return null;
            }
            targetLabel = getTargetLabel();
            arc = getArc(upto);
        }
    }

    /**
     * Finds and reads an arc on the current node which label is strictly less than the given label.
     * Skips the first arc, finds next floor arc; or none if the floor arc is the first arc itself (in
     * this case it has already been read).
     *
     * <p>Precondition: the given arc is the first arc of the node.
     */
    private void findNextFloorArcDirectAddressing(FST.Arc<T> arc, int targetLabel, final FST.BytesReader in) throws IOException {
        assert arc.nodeFlags() == FST.ARCS_FOR_DIRECT_ADDRESSING;
        assert arc.label() != FST.END_LABEL;
        assert arc.label() == arc.firstLabel();
        if (arc.numArcs() > 1) {
            int targetIndex = targetLabel - arc.firstLabel();
            assert targetIndex >= 0;
            if (targetIndex >= arc.numArcs()) {
                fst.readLastArcByDirectAddressing(arc, in);
            } else {
                int floorIndex = BitTable.previousBitSet(targetIndex, arc, in);
                if (floorIndex > 0) {
                    fst.readArcByDirectAddressing(arc, in, floorIndex);
                }
            }
        }
    }

    /** Same as {@link #findNextFloorArcDirectAddressing} for binary search node. */
    private void findNextFloorArcBinarySearch(FST.Arc<T> arc, int targetLabel, FST.BytesReader in) throws IOException {
        assert arc.nodeFlags() == FST.ARCS_FOR_BINARY_SEARCH;
        assert arc.label() != FST.END_LABEL;
        assert arc.arcIdx() == 0;
        if (arc.numArcs() > 1) {
            int idx = Util.binarySearch(fst, arc, targetLabel);
            assert idx != -1;
            if (idx > 1) {
                fst.readArcByIndex(arc, in, idx - 1);
            } else if (idx < -2) {
                fst.readArcByIndex(arc, in, -2 - idx);
            }
        }
    }

    private FST.Arc<T> doSeekFloorArrayPacked(FST.Arc<T> arc, int targetLabel, final FST.BytesReader in) throws IOException {
        int idx = Util.binarySearch(fst, arc, targetLabel);

        if (idx >= 0) {
            fst.readArcByIndex(arc, in, idx);
            assert arc.arcIdx() == idx;
            assert arc.label() == targetLabel : "arc.label=" + arc.label() + " vs targetLabel=" + targetLabel + " mid=" + idx;
            output[upto] = fst.outputs.add(output[upto - 1], arc.output());
            if (targetLabel == FST.END_LABEL) {
                return null;
            }
            setCurrentLabel(arc.label());
            incr();
            return fst.readFirstTargetArc(arc, getArc(upto), fstReader);
        } else if (idx == -1) {
            return backtrackToFloorArc(arc, targetLabel, in);
        } else {
            fst.readArcByIndex(arc, in, -2 - idx);
            assert arc.isLast() || fst.readNextArcLabel(arc, in) > targetLabel;
            assert arc.label() < targetLabel : "arc.label=" + arc.label() + " vs targetLabel=" + targetLabel;
            pushLast();
            return null;
        }
    }

    private FST.Arc<T> doSeekFloorList(FST.Arc<T> arc, int targetLabel) throws IOException {
        if (arc.label() == targetLabel) {
            output[upto] = fst.outputs.add(output[upto - 1], arc.output());
            if (targetLabel == FST.END_LABEL) {
                return null;
            }
            setCurrentLabel(arc.label());
            incr();
            return fst.readFirstTargetArc(arc, getArc(upto), fstReader);
        } else if (arc.label() > targetLabel) {
            while (true) {
                fst.readFirstTargetArc(getArc(upto - 1), arc, fstReader);
                if (arc.label() < targetLabel) {
                    while (arc.isLast() == false && fst.readNextArcLabel(arc, fstReader) < targetLabel) {
                        fst.readNextArc(arc, fstReader);
                    }
                    pushLast();
                    return null;
                }
                upto--;
                if (upto == 0) {
                    return null;
                }
                targetLabel = getTargetLabel();
                arc = getArc(upto);
            }
        } else if (arc.isLast() == false) {
            if (fst.readNextArcLabel(arc, fstReader) > targetLabel) {
                pushLast();
                return null;
            } else {
                return fst.readNextArc(arc, fstReader);
            }
        } else {
            pushLast();
            return null;
        }
    }

    /** Seeks to exactly target term. */
    boolean doSeekExact() throws IOException {



        rewindPrefix();

        FST.Arc<T> arc = getArc(upto - 1);
        int targetLabel = getTargetLabel();

        final FST.BytesReader fstReader = fst.getBytesReader();

        while (true) {
            final FST.Arc<T> nextArc = fst.findTargetArc(targetLabel, arc, getArc(upto), fstReader);
            if (nextArc == null) {
                fst.readFirstTargetArc(arc, getArc(upto), fstReader);
                return false;
            }
            output[upto] = fst.outputs.add(output[upto - 1], nextArc.output());
            if (targetLabel == FST.END_LABEL) {
                return true;
            }
            setCurrentLabel(targetLabel);
            incr();
            targetLabel = getTargetLabel();
            arc = nextArc;
        }
    }

    private void incr() {
        upto++;
        grow();
        if (arcs.length <= upto) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            final FST.Arc<T>[] newArcs = new FST.Arc[ArrayUtil.oversize(1 + upto, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
            System.arraycopy(arcs, 0, newArcs, 0, arcs.length);
            arcs = newArcs;
        }
        if (output.length <= upto) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            final T[] newOutput = (T[]) new Object[ArrayUtil.oversize(1 + upto, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
            System.arraycopy(output, 0, newOutput, 0, output.length);
            output = newOutput;
        }
    }

    private void pushFirst() throws IOException {

        FST.Arc<T> arc = arcs[upto];
        assert arc != null;

        while (true) {
            output[upto] = fst.outputs.add(output[upto - 1], arc.output());
            if (arc.label() == FST.END_LABEL) {
                break;
            }
            setCurrentLabel(arc.label());
            incr();

            final FST.Arc<T> nextArc = getArc(upto);
            fst.readFirstTargetArc(arc, nextArc, fstReader);
            arc = nextArc;
        }
    }

    private void pushLast() throws IOException {

        FST.Arc<T> arc = arcs[upto];
        assert arc != null;

        while (true) {
            setCurrentLabel(arc.label());
            output[upto] = fst.outputs.add(output[upto - 1], arc.output());
            if (arc.label() == FST.END_LABEL) {
                break;
            }
            incr();

            arc = fst.readLastTargetArc(arc, getArc(upto), fstReader);
        }
    }

    private FST.Arc<T> getArc(int idx) {
        if (arcs[idx] == null) {
            arcs[idx] = new FST.Arc<>();
        }
        return arcs[idx];
    }
}
