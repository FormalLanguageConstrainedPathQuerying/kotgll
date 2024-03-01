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

import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PagedGrowableWriter;

import java.io.IOException;

final class NodeHash<T> {

    private PagedGrowableWriter table;
    private long count;
    private long mask;
    private final FST<T> fst;
    private final FST.Arc<T> scratchArc = new FST.Arc<>();
    private final FST.BytesReader in;

    NodeHash(FST<T> fst, FST.BytesReader in) {
        table = new PagedGrowableWriter(16, 1 << 27, 8, PackedInts.COMPACT);
        mask = 15;
        this.fst = fst;
        this.in = in;
    }

    private boolean nodesEqual(FSTCompiler.UnCompiledNode<T> node, long address) throws IOException {
        fst.readFirstRealTargetArc(address, scratchArc, in);

        if (scratchArc.bytesPerArc() != 0) {
            if (scratchArc.nodeFlags() == FST.ARCS_FOR_BINARY_SEARCH) {
                if (node.numArcs != scratchArc.numArcs()) {
                    return false;
                }
            } else {
                assert scratchArc.nodeFlags() == FST.ARCS_FOR_DIRECT_ADDRESSING;
                if ((node.arcs[node.numArcs - 1].label - node.arcs[0].label + 1) != scratchArc.numArcs()
                    || node.numArcs != FST.Arc.BitTable.countBits(scratchArc, in)) {
                    return false;
                }
            }
        }

        for (int arcUpto = 0; arcUpto < node.numArcs; arcUpto++) {
            final FSTCompiler.Arc<T> arc = node.arcs[arcUpto];
            if (arc.label != scratchArc.label()
                || arc.output.equals(scratchArc.output()) == false
                || ((FSTCompiler.CompiledNode) arc.target).node != scratchArc.target()
                || arc.nextFinalOutput.equals(scratchArc.nextFinalOutput()) == false
                || arc.isFinal != scratchArc.isFinal()) {
                return false;
            }

            if (scratchArc.isLast()) {
                if (arcUpto == node.numArcs - 1) {
                    return true;
                } else {
                    return false;
                }
            }
            fst.readNextRealArc(scratchArc, in);
        }

        return false;
    }

    private long hash(FSTCompiler.UnCompiledNode<T> node) {
        final int PRIME = 31;
        long h = 0;
        for (int arcIdx = 0; arcIdx < node.numArcs; arcIdx++) {
            final FSTCompiler.Arc<T> arc = node.arcs[arcIdx];
            h = PRIME * h + arc.label;
            long n = ((FSTCompiler.CompiledNode) arc.target).node;
            h = PRIME * h + (int) (n ^ (n >> 32));
            h = PRIME * h + arc.output.hashCode();
            h = PRIME * h + arc.nextFinalOutput.hashCode();
            if (arc.isFinal) {
                h += 17;
            }
        }
        return h & Long.MAX_VALUE;
    }

    private long hash(long node) throws IOException {
        final int PRIME = 31;
        long h = 0;
        fst.readFirstRealTargetArc(node, scratchArc, in);
        while (true) {
            h = PRIME * h + scratchArc.label();
            h = PRIME * h + (int) (scratchArc.target() ^ (scratchArc.target() >> 32));
            h = PRIME * h + scratchArc.output().hashCode();
            h = PRIME * h + scratchArc.nextFinalOutput().hashCode();
            if (scratchArc.isFinal()) {
                h += 17;
            }
            if (scratchArc.isLast()) {
                break;
            }
            fst.readNextRealArc(scratchArc, in);
        }
        return h & Long.MAX_VALUE;
    }

    public long add(FSTCompiler<T> fstCompiler, FSTCompiler.UnCompiledNode<T> nodeIn) throws IOException {
        final long h = hash(nodeIn);
        long pos = h & mask;
        int c = 0;
        while (true) {
            final long v = table.get(pos);
            if (v == 0) {
                final long node = fst.addNode(fstCompiler, nodeIn);
                assert hash(node) == h : "frozenHash=" + hash(node) + " vs h=" + h;
                count++;
                table.set(pos, node);
                if (count > 2 * table.size() / 3) {
                    rehash();
                }
                return node;
            } else if (nodesEqual(nodeIn, v)) {
                return v;
            }

            pos = (pos + (++c)) & mask;
        }
    }

    private void addNew(long address) throws IOException {
        long pos = hash(address) & mask;
        int c = 0;
        while (true) {
            if (table.get(pos) == 0) {
                table.set(pos, address);
                break;
            }

            pos = (pos + (++c)) & mask;
        }
    }

    private void rehash() throws IOException {
        final PagedGrowableWriter oldTable = table;

        table = new PagedGrowableWriter(2 * oldTable.size(), 1 << 30, PackedInts.bitsRequired(count), PackedInts.COMPACT);
        mask = table.size() - 1;
        for (long idx = 0; idx < oldTable.size(); idx++) {
            final long address = oldTable.get(idx);
            if (address != 0) {
                addNew(address);
            }
        }
    }
}
