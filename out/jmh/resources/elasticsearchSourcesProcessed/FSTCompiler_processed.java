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

import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.FST.INPUT_TYPE;

import java.io.IOException;


/**
 * Builds a minimal FST (maps an IntsRef term to an arbitrary output) from pre-sorted terms with
 * outputs. The FST becomes an FSA if you use NoOutputs. The FST is written on-the-fly into a
 * compact serialized format byte array, which can be saved to / loaded from a Directory or used
 * directly for traversal. The FST is always finite (no cycles).
 *
 * <p>NOTE: The algorithm is described at
 * http:
 *
 * <p>The parameterized type T is the output type. See the subclasses of {@link Outputs}.
 *
 * <p>FSTs larger than 2.1GB are now possible (as of Lucene 4.2). FSTs containing more than 2.1B
 * nodes are also now possible, however they cannot be packed.
 *
 */
public class FSTCompiler<T> {

    static final float DIRECT_ADDRESSING_MAX_OVERSIZING_FACTOR = 1f;

    private final NodeHash<T> dedupHash;
    final FST<T> fst;
    private final T NO_OUTPUT;


    private final int minSuffixCount1;

    private final int minSuffixCount2;

    private final boolean doShareNonSingletonNodes;
    private final int shareMaxTailLength;

    private final IntsRefBuilder lastInput = new IntsRefBuilder();

    private UnCompiledNode<T>[] frontier;

    long lastFrozenNode;

    int[] numBytesPerArc = new int[4];
    int[] numLabelBytesPerArc = new int[numBytesPerArc.length];
    final FixedLengthArcsBuffer fixedLengthArcsBuffer = new FixedLengthArcsBuffer();

    long arcCount;
    long nodeCount;
    long binarySearchNodeCount;
    long directAddressingNodeCount;

    final boolean allowFixedLengthArcs;
    final float directAddressingMaxOversizingFactor;
    long directAddressingExpansionCredit;

    final BytesStore bytes;

    /**
     * Instantiates an FST/FSA builder with default settings and pruning options turned off. For more
     * tuning and tweaking, see {@link Builder}.
     */
    public FSTCompiler(INPUT_TYPE inputType, Outputs<T> outputs) {
        this(inputType, 0, 0, true, true, Integer.MAX_VALUE, outputs, true, 15, 1f);
    }

    private FSTCompiler(
        INPUT_TYPE inputType,
        int minSuffixCount1,
        int minSuffixCount2,
        boolean doShareSuffix,
        boolean doShareNonSingletonNodes,
        int shareMaxTailLength,
        Outputs<T> outputs,
        boolean allowFixedLengthArcs,
        int bytesPageBits,
        float directAddressingMaxOversizingFactor
    ) {
        this.minSuffixCount1 = minSuffixCount1;
        this.minSuffixCount2 = minSuffixCount2;
        this.doShareNonSingletonNodes = doShareNonSingletonNodes;
        this.shareMaxTailLength = shareMaxTailLength;
        this.allowFixedLengthArcs = allowFixedLengthArcs;
        this.directAddressingMaxOversizingFactor = directAddressingMaxOversizingFactor;
        fst = new FST<>(inputType, outputs, bytesPageBits);
        bytes = fst.bytes;
        assert bytes != null;
        if (doShareSuffix) {
            dedupHash = new NodeHash<>(fst, bytes.getReverseReader(false));
        } else {
            dedupHash = null;
        }
        NO_OUTPUT = outputs.getNoOutput();

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final UnCompiledNode<T>[] f = (UnCompiledNode<T>[]) new UnCompiledNode[10];
        frontier = f;
        for (int idx = 0; idx < frontier.length; idx++) {
            frontier[idx] = new UnCompiledNode<>(this, idx);
        }
    }

    /**
     * Fluent-style constructor for FST {@link FSTCompiler}.
     *
     * <p>Creates an FST/FSA builder with all the possible tuning and construction tweaks. Read
     * parameter documentation carefully.
     */
    public static class Builder<T> {

        private final INPUT_TYPE inputType;
        private final Outputs<T> outputs;
        private int minSuffixCount1;
        private int minSuffixCount2;
        private boolean shouldShareSuffix = true;
        private boolean shouldShareNonSingletonNodes = true;
        private int shareMaxTailLength = Integer.MAX_VALUE;
        private boolean allowFixedLengthArcs = true;
        private int bytesPageBits = 15;
        private float directAddressingMaxOversizingFactor = DIRECT_ADDRESSING_MAX_OVERSIZING_FACTOR;

        /**
         * @param inputType The input type (transition labels). Can be anything from {@link INPUT_TYPE}
         *     enumeration. Shorter types will consume less memory. Strings (character sequences) are
         *     represented as {@link INPUT_TYPE#BYTE4} (full unicode codepoints).
         * @param outputs The output type for each input sequence. Applies only if building an FST.
         */
        public Builder(INPUT_TYPE inputType, Outputs<T> outputs) {
            this.inputType = inputType;
            this.outputs = outputs;
        }

        /**
         * If pruning the input graph during construction, this threshold is used for telling if a node
         * is kept or pruned. If transition_count(node) &gt;= minSuffixCount1, the node is kept.
         *
         * <p>Default = 0.
         */
        public Builder<T> minSuffixCount1(int minSuffixCount1) {
            this.minSuffixCount1 = minSuffixCount1;
            return this;
        }

        /**
         * Better pruning: we prune node (and all following nodes) if the prior node has less than this
         * number of terms go through it.
         *
         * <p>Default = 0.
         */
        public Builder<T> minSuffixCount2(int minSuffixCount2) {
            this.minSuffixCount2 = minSuffixCount2;
            return this;
        }

        /**
         * If {@code true}, the shared suffixes will be compacted into unique paths. This requires an
         * additional RAM-intensive hash map for lookups in memory. Setting this parameter to {@code
         * false} creates a single suffix path for all input sequences. This will result in a larger
         * FST, but requires substantially less memory and CPU during building.
         *
         * <p>Default = {@code true}.
         */
        public Builder<T> shouldShareSuffix(boolean shouldShareSuffix) {
            this.shouldShareSuffix = shouldShareSuffix;
            return this;
        }

        /**
         * Only used if {@code shouldShareSuffix} is true. Set this to true to ensure FST is fully
         * minimal, at cost of more CPU and more RAM during building.
         *
         * <p>Default = {@code true}.
         */
        public Builder<T> shouldShareNonSingletonNodes(boolean shouldShareNonSingletonNodes) {
            this.shouldShareNonSingletonNodes = shouldShareNonSingletonNodes;
            return this;
        }

        /**
         * Only used if {@code shouldShareSuffix} is true. Set this to Integer.MAX_VALUE to ensure FST
         * is fully minimal, at cost of more CPU and more RAM during building.
         *
         * <p>Default = {@link Integer#MAX_VALUE}.
         */
        public Builder<T> shareMaxTailLength(int shareMaxTailLength) {
            this.shareMaxTailLength = shareMaxTailLength;
            return this;
        }

        /**
         * Pass {@code false} to disable the fixed length arc optimization (binary search or direct
         * addressing) while building the FST; this will make the resulting FST smaller but slower to
         * traverse.
         *
         * <p>Default = {@code true}.
         */
        public Builder<T> allowFixedLengthArcs(boolean allowFixedLengthArcs) {
            this.allowFixedLengthArcs = allowFixedLengthArcs;
            return this;
        }

        /**
         * How many bits wide to make each byte[] block in the BytesStore; if you know the FST will be
         * large then make this larger. For example 15 bits = 32768 byte pages.
         *
         * <p>Default = 15.
         */
        public Builder<T> bytesPageBits(int bytesPageBits) {
            this.bytesPageBits = bytesPageBits;
            return this;
        }

        /**
         * Overrides the default the maximum oversizing of fixed array allowed to enable direct
         * addressing of arcs instead of binary search.
         *
         * <p>Setting this factor to a negative value (e.g. -1) effectively disables direct addressing,
         * only binary search nodes will be created.
         *
         * <p>This factor does not determine whether to encode a node with a list of variable length
         * arcs or with fixed length arcs. It only determines the effective encoding of a node that is
         * already known to be encoded with fixed length arcs.
         *
         * <p>Default = 1.
         */
        public Builder<T> directAddressingMaxOversizingFactor(float factor) {
            this.directAddressingMaxOversizingFactor = factor;
            return this;
        }

        /** Creates a new {@link FSTCompiler}. */
        public FSTCompiler<T> build() {
            FSTCompiler<T> fstCompiler = new FSTCompiler<>(
                inputType,
                minSuffixCount1,
                minSuffixCount2,
                shouldShareSuffix,
                shouldShareNonSingletonNodes,
                shareMaxTailLength,
                outputs,
                allowFixedLengthArcs,
                bytesPageBits,
                directAddressingMaxOversizingFactor
            );
            return fstCompiler;
        }
    }

    public float getDirectAddressingMaxOversizingFactor() {
        return directAddressingMaxOversizingFactor;
    }

    public long getTermCount() {
        return frontier[0].inputCount;
    }

    public long getNodeCount() {
        return 1 + nodeCount;
    }

    public long getArcCount() {
        return arcCount;
    }

    public long getMappedStateCount() {
        return dedupHash == null ? 0 : nodeCount;
    }

    private CompiledNode compileNode(UnCompiledNode<T> nodeIn, int tailLength) throws IOException {
        final long node;
        long bytesPosStart = bytes.getPosition();
        if (dedupHash != null && (doShareNonSingletonNodes || nodeIn.numArcs <= 1) && tailLength <= shareMaxTailLength) {
            if (nodeIn.numArcs == 0) {
                node = fst.addNode(this, nodeIn);
                lastFrozenNode = node;
            } else {
                node = dedupHash.add(this, nodeIn);
            }
        } else {
            node = fst.addNode(this, nodeIn);
        }
        assert node != -2;

        long bytesPosEnd = bytes.getPosition();
        if (bytesPosEnd != bytesPosStart) {
            assert bytesPosEnd > bytesPosStart;
            lastFrozenNode = node;
        }

        nodeIn.clear();

        final CompiledNode fn = new CompiledNode();
        fn.node = node;
        return fn;
    }

    private void freezeTail(int prefixLenPlus1) throws IOException {
        final int downTo = Math.max(1, prefixLenPlus1);
        for (int idx = lastInput.length(); idx >= downTo; idx--) {

            boolean doPrune = false;
            boolean doCompile = false;

            final UnCompiledNode<T> node = frontier[idx];
            final UnCompiledNode<T> parent = frontier[idx - 1];

            if (node.inputCount < minSuffixCount1) {
                doPrune = true;
                doCompile = true;
            } else if (idx > prefixLenPlus1) {
                if (parent.inputCount < minSuffixCount2 || (minSuffixCount2 == 1 && parent.inputCount == 1 && idx > 1)) {

                    doPrune = true;
                } else {
                    doPrune = false;
                }
                doCompile = true;
            } else {
                doCompile = minSuffixCount2 == 0;
            }


            if (node.inputCount < minSuffixCount2 || (minSuffixCount2 == 1 && node.inputCount == 1 && idx > 1)) {
                for (int arcIdx = 0; arcIdx < node.numArcs; arcIdx++) {
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    final UnCompiledNode<T> target = (UnCompiledNode<T>) node.arcs[arcIdx].target;
                    target.clear();
                }
                node.numArcs = 0;
            }

            if (doPrune) {
                node.clear();
                parent.deleteLast(lastInput.intAt(idx - 1), node);
            } else {

                if (minSuffixCount2 != 0) {
                    compileAllTargets(node, lastInput.length() - idx);
                }
                final T nextFinalOutput = node.output;

                final boolean isFinal = node.isFinal || node.numArcs == 0;

                if (doCompile) {
                    parent.replaceLast(lastInput.intAt(idx - 1), compileNode(node, 1 + lastInput.length() - idx), nextFinalOutput, isFinal);
                } else {
                    parent.replaceLast(lastInput.intAt(idx - 1), node, nextFinalOutput, isFinal);
                    frontier[idx] = new UnCompiledNode<>(this, idx);
                }
            }
        }
    }

    /*
    private String toString(BytesRef b) {
    try {
      return b.utf8ToString() + " " + b;
    } catch (Throwable t) {
      return b.toString();
    }
    }
    */

    /**
     * Add the next input/output pair. The provided input must be sorted after the previous one
     * according to {@link IntsRef#compareTo}. It's also OK to add the same input twice in a row with
     * different outputs, as long as {@link Outputs} implements the {@link Outputs#merge} method. Note
     * that input is fully consumed after this method is returned (so caller is free to reuse), but
     * output is not. So if your outputs are changeable (eg {@link ByteSequenceOutputs})
     * then you cannot reuse across calls.
     */
    public void add(IntsRef input, T output) throws IOException {
        /*
        if (DEBUG) {
          BytesRef b = new BytesRef(input.length);
          for(int x=0;x<input.length;x++) {
        b.bytes[x] = (byte) input.ints[x];
          }
          b.length = input.length;
          if (output == NO_OUTPUT) {
        System.out.println("\nFST ADD: input=" + toString(b) + " " + b);
          } else {
        System.out.println("\nFST ADD: input=" + toString(b) + " " + b + " output=" + fst.outputs.outputToString(output));
          }
        }
        */

        if (output.equals(NO_OUTPUT)) {
            output = NO_OUTPUT;
        }

        assert lastInput.length() == 0 || input.compareTo(lastInput.get()) >= 0
            : "inputs are added out of order lastInput=" + lastInput.get() + " vs input=" + input;
        assert validOutput(output);

        if (input.length == 0) {
            frontier[0].inputCount++;
            frontier[0].isFinal = true;
            fst.setEmptyOutput(output);
            return;
        }

        int pos1 = 0;
        int pos2 = input.offset;
        final int pos1Stop = Math.min(lastInput.length(), input.length);
        while (true) {
            frontier[pos1].inputCount++;
            if (pos1 >= pos1Stop || lastInput.intAt(pos1) != input.ints[pos2]) {
                break;
            }
            pos1++;
            pos2++;
        }
        final int prefixLenPlus1 = pos1 + 1;

        if (frontier.length < input.length + 1) {
            final UnCompiledNode<T>[] next = ArrayUtil.grow(frontier, input.length + 1);
            for (int idx = frontier.length; idx < next.length; idx++) {
                next[idx] = new UnCompiledNode<>(this, idx);
            }
            frontier = next;
        }

        freezeTail(prefixLenPlus1);

        for (int idx = prefixLenPlus1; idx <= input.length; idx++) {
            frontier[idx - 1].addArc(input.ints[input.offset + idx - 1], frontier[idx]);
            frontier[idx].inputCount++;
        }

        final UnCompiledNode<T> lastNode = frontier[input.length];
        if (lastInput.length() != input.length || prefixLenPlus1 != input.length + 1) {
            lastNode.isFinal = true;
            lastNode.output = NO_OUTPUT;
        }

        for (int idx = 1; idx < prefixLenPlus1; idx++) {
            final UnCompiledNode<T> node = frontier[idx];
            final UnCompiledNode<T> parentNode = frontier[idx - 1];

            final T lastOutput = parentNode.getLastOutput(input.ints[input.offset + idx - 1]);
            assert validOutput(lastOutput);

            final T commonOutputPrefix;
            final T wordSuffix;

            if (lastOutput != NO_OUTPUT) {
                commonOutputPrefix = fst.outputs.common(output, lastOutput);
                assert validOutput(commonOutputPrefix);
                wordSuffix = fst.outputs.subtract(lastOutput, commonOutputPrefix);
                assert validOutput(wordSuffix);
                parentNode.setLastOutput(input.ints[input.offset + idx - 1], commonOutputPrefix);
                node.prependOutput(wordSuffix);
            } else {
                commonOutputPrefix = wordSuffix = NO_OUTPUT;
            }

            output = fst.outputs.subtract(output, commonOutputPrefix);
            assert validOutput(output);
        }

        if (lastInput.length() == input.length && prefixLenPlus1 == 1 + input.length) {
            lastNode.output = fst.outputs.merge(lastNode.output, output);
        } else {
            frontier[prefixLenPlus1 - 1].setLastOutput(input.ints[input.offset + prefixLenPlus1 - 1], output);
        }

        lastInput.copyInts(input);

    }

    private boolean validOutput(T output) {
        return output == NO_OUTPUT || output.equals(NO_OUTPUT) == false;
    }

    /** Returns final FST. NOTE: this will return null if nothing is accepted by the FST. */
    public FST<T> compile() throws IOException {

        final UnCompiledNode<T> root = frontier[0];

        freezeTail(0);
        if (root.inputCount < minSuffixCount1 || root.inputCount < minSuffixCount2 || root.numArcs == 0) {
            if (fst.emptyOutput == null) {
                return null;
            } else if (minSuffixCount1 > 0 || minSuffixCount2 > 0) {
                return null;
            }
        } else {
            if (minSuffixCount2 != 0) {
                compileAllTargets(root, lastInput.length());
            }
        }
        fst.finish(compileNode(root, lastInput.length()).node);

        return fst;
    }

    private void compileAllTargets(UnCompiledNode<T> node, int tailLength) throws IOException {
        for (int arcIdx = 0; arcIdx < node.numArcs; arcIdx++) {
            final Arc<T> arc = node.arcs[arcIdx];
            if (arc.target.isCompiled() == false) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                final UnCompiledNode<T> n = (UnCompiledNode<T>) arc.target;
                if (n.numArcs == 0) {
                    arc.isFinal = n.isFinal = true;
                }
                arc.target = compileNode(n, tailLength - 1);
            }
        }
    }

    /** Expert: holds a pending (seen but not yet serialized) arc. */
    static class Arc<T> {
        int label; 
        Node target;
        boolean isFinal;
        T output;
        T nextFinalOutput;
    }


    interface Node {
        boolean isCompiled();
    }

    public long fstRamBytesUsed() {
        return fst.ramBytesUsed();
    }

    static final class CompiledNode implements Node {
        long node;

        @Override
        public boolean isCompiled() {
            return true;
        }
    }

    /** Expert: holds a pending (seen but not yet serialized) Node. */
    static final class UnCompiledNode<T> implements Node {
        final FSTCompiler<T> owner;
        int numArcs;
        Arc<T>[] arcs;
        T output;
        boolean isFinal;
        long inputCount;

        /** This node's depth, starting from the automaton root. */
        final int depth;

        /**
         * @param depth The node's depth starting from the automaton root. Needed for LUCENE-2934 (node
         *     expansion based on conditions other than the fanout size).
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
        UnCompiledNode(FSTCompiler<T> owner, int depth) {
            this.owner = owner;
            arcs = (Arc<T>[]) new Arc[1];
            arcs[0] = new Arc<>();
            output = owner.NO_OUTPUT;
            this.depth = depth;
        }

        @Override
        public boolean isCompiled() {
            return false;
        }

        void clear() {
            numArcs = 0;
            isFinal = false;
            output = owner.NO_OUTPUT;
            inputCount = 0;

        }

        T getLastOutput(int labelToMatch) {
            assert numArcs > 0;
            assert arcs[numArcs - 1].label == labelToMatch;
            return arcs[numArcs - 1].output;
        }

        void addArc(int label, Node target) {
            assert label >= 0;
            assert numArcs == 0 || label > arcs[numArcs - 1].label
                : "arc[numArcs-1].label=" + arcs[numArcs - 1].label + " new label=" + label + " numArcs=" + numArcs;
            if (numArcs == arcs.length) {
                final Arc<T>[] newArcs = ArrayUtil.grow(arcs);
                for (int arcIdx = numArcs; arcIdx < newArcs.length; arcIdx++) {
                    newArcs[arcIdx] = new Arc<>();
                }
                arcs = newArcs;
            }
            final Arc<T> arc = arcs[numArcs++];
            arc.label = label;
            arc.target = target;
            arc.output = arc.nextFinalOutput = owner.NO_OUTPUT;
            arc.isFinal = false;
        }

        void replaceLast(int labelToMatch, Node target, T nextFinalOutput, boolean isFinal) {
            assert numArcs > 0;
            final Arc<T> arc = arcs[numArcs - 1];
            assert arc.label == labelToMatch : "arc.label=" + arc.label + " vs " + labelToMatch;
            arc.target = target;
            arc.nextFinalOutput = nextFinalOutput;
            arc.isFinal = isFinal;
        }

        void deleteLast(int label, Node target) {
            assert numArcs > 0;
            assert label == arcs[numArcs - 1].label;
            assert target == arcs[numArcs - 1].target;
            numArcs--;
        }

        void setLastOutput(int labelToMatch, T newOutput) {
            assert owner.validOutput(newOutput);
            assert numArcs > 0;
            final Arc<T> arc = arcs[numArcs - 1];
            assert arc.label == labelToMatch;
            arc.output = newOutput;
        }

        void prependOutput(T outputPrefix) {
            assert owner.validOutput(outputPrefix);

            for (int arcIdx = 0; arcIdx < numArcs; arcIdx++) {
                arcs[arcIdx].output = owner.fst.outputs.add(outputPrefix, arcs[arcIdx].output);
                assert owner.validOutput(arcs[arcIdx].output);
            }

            if (isFinal) {
                output = owner.fst.outputs.add(outputPrefix, output);
                assert owner.validOutput(output);
            }
        }
    }

    /**
     * Reusable buffer for building nodes with fixed length arcs (binary search or direct addressing).
     */
    static class FixedLengthArcsBuffer {

        private byte[] bytes = new byte[11];
        private final ByteArrayDataOutput bado = new ByteArrayDataOutput(bytes);

        /** Ensures the capacity of the internal byte array. Enlarges it if needed. */
        FixedLengthArcsBuffer ensureCapacity(int capacity) {
            if (bytes.length < capacity) {
                bytes = new byte[ArrayUtil.oversize(capacity, Byte.BYTES)];
                bado.reset(bytes);
            }
            return this;
        }

        FixedLengthArcsBuffer resetPosition() {
            bado.reset(bytes);
            return this;
        }

        FixedLengthArcsBuffer writeByte(byte b) {
            bado.writeByte(b);
            return this;
        }

        FixedLengthArcsBuffer writeVInt(int i) {
            try {
                bado.writeVInt(i);
            } catch (IOException e) { 
                throw new RuntimeException(e);
            }
            return this;
        }

        int getPosition() {
            return bado.getPosition();
        }

        /** Gets the internal byte array. */
        byte[] getBytes() {
            return bytes;
        }
    }
}
