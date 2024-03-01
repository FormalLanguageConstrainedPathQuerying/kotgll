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

import org.apache.lucene.backward_codecs.store.EndiannessReverserUtil;
import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.store.ByteBuffersDataOutput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.compress.LZ4;
import org.apache.lucene.util.compress.LowercaseAsciiCompression;
import org.elasticsearch.core.IOUtils;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.ByteSequenceOutputs;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.BytesRefFSTEnum;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.FST;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.FSTCompiler;
import org.elasticsearch.xpack.lucene.bwc.codecs.lucene70.fst.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/*
  TODO:

    - Currently there is a one-to-one mapping of indexed
      term to term block, but we could decouple the two, ie,
      put more terms into the index than there are blocks.
      The index would take up more RAM but then it'd be able
      to avoid seeking more often and could make PK/FuzzyQ
      faster if the additional indexed terms could store
      the offset into the terms block.

    - The blocks are not written in true depth-first
      order, meaning if you just next() the file pointer will
      sometimes jump backwards.  For example, block foo* will
      be written before block f* because it finished before.
      This could possibly hurt performance if the terms dict is
      not hot, since OSs anticipate sequential file access.  We
      could fix the writer to re-order the blocks as a 2nd
      pass.

    - Each block encodes the term suffixes packed
      sequentially using a separate vInt per term, which is
      1) wasteful and 2) slow (must linear scan to find a
      particular suffix).  We should instead 1) make
      random-access array so we can directly access the Nth
      suffix, and 2) bulk-encode this array using bulk int[]
      codecs; then at search time we can binary search when
      we seek a particular term.
*/

/**
 * Block-based terms index and dictionary writer.
 *
 * <p>Writes terms dict and index, block-encoding (column stride) each term's metadata for each set
 * of terms between two index terms.
 *
 * <p>Files:
 *
 * <ul>
 *   <li><code>.tim</code>: <a href="#Termdictionary">Term Dictionary</a>
 *   <li><code>.tip</code>: <a href="#Termindex">Term Index</a>
 * </ul>
 *
 * <p><a id="Termdictionary"></a>
 *
 * <h2>Term Dictionary</h2>
 *
 * <p>The .tim file contains the list of terms in each field along with per-term statistics (such as
 * docfreq) and per-term metadata (typically pointers to the postings list for that term in the
 * inverted index).
 *
 * <p>The .tim is arranged in blocks: with blocks containing a variable number of entries (by
 * default 25-48), where each entry is either a term or a reference to a sub-block.
 *
 * <p>NOTE: The term dictionary can plug into different postings implementations: the postings
 * writer/reader are actually responsible for encoding and decoding the Postings Metadata and Term
 * Metadata sections.
 *
 * <ul>
 *   <li>TermsDict (.tim) --&gt; Header, <i>PostingsHeader</i>, NodeBlock<sup>NumBlocks</sup>,
 *       FieldSummary, DirOffset, Footer
 *   <li>NodeBlock --&gt; (OuterNode | InnerNode)
 *   <li>OuterNode --&gt; EntryCount, SuffixLength, Byte<sup>SuffixLength</sup>, StatsLength, &lt;
 *       TermStats &gt;<sup>EntryCount</sup>, MetaLength,
 *       &lt;<i>TermMetadata</i>&gt;<sup>EntryCount</sup>
 *   <li>InnerNode --&gt; EntryCount, SuffixLength[,Sub?], Byte<sup>SuffixLength</sup>, StatsLength,
 *       &lt; TermStats ? &gt;<sup>EntryCount</sup>, MetaLength, &lt;<i>TermMetadata ?
 *       </i>&gt;<sup>EntryCount</sup>
 *   <li>TermStats --&gt; DocFreq, TotalTermFreq
 *   <li>FieldSummary --&gt; NumFields, &lt;FieldNumber, NumTerms, RootCodeLength,
 *       Byte<sup>RootCodeLength</sup>, SumTotalTermFreq?, SumDocFreq, DocCount, LongsSize, MinTerm,
 *       MaxTerm&gt;<sup>NumFields</sup>
 *   <li>Header --&gt; {@link CodecUtil#writeHeader CodecHeader}
 *   <li>DirOffset --&gt; {@link DataOutput#writeLong Uint64}
 *   <li>MinTerm,MaxTerm --&gt; {@link DataOutput#writeVInt VInt} length followed by the byte[]
 *   <li>EntryCount,SuffixLength,StatsLength,DocFreq,MetaLength,NumFields,
 *       FieldNumber,RootCodeLength,DocCount,LongsSize --&gt; {@link DataOutput#writeVInt VInt}
 *   <li>TotalTermFreq,NumTerms,SumTotalTermFreq,SumDocFreq --&gt; {@link DataOutput#writeVLong
 *       VLong}
 *   <li>Footer --&gt; {@link CodecUtil#writeFooter CodecFooter}
 * </ul>
 *
 * <p>Notes:
 *
 * <ul>
 *   <li>Header is a {@link CodecUtil#writeHeader CodecHeader} storing the version information for
 *       the BlockTree implementation.
 *   <li>DirOffset is a pointer to the FieldSummary section.
 *   <li>DocFreq is the count of documents which contain the term.
 *   <li>TotalTermFreq is the total number of occurrences of the term. This is encoded as the
 *       difference between the total number of occurrences and the DocFreq.
 *   <li>FieldNumber is the fields number from {@link FieldInfos}. (.fnm)
 *   <li>NumTerms is the number of unique terms for the field.
 *   <li>RootCode points to the root block for the field.
 *   <li>SumDocFreq is the total number of postings, the number of term-document pairs across the
 *       entire field.
 *   <li>DocCount is the number of documents that have at least one posting for this field.
 *   <li>LongsSize records how many long values the postings writer/reader record per term (e.g., to
 *       hold freq/prox/doc file offsets).
 *   <li>MinTerm, MaxTerm are the lowest and highest term in this field.
 *   <li>PostingsHeader and TermMetadata are plugged into by the specific postings implementation:
 *       these contain arbitrary per-file data (such as parameters or versioning information) and
 *       per-term data (such as pointers to inverted files).
 *   <li>For inner nodes of the tree, every entry will steal one bit to mark whether it points to
 *       child nodes(sub-block). If so, the corresponding TermStats and TermMetaData are omitted
 * </ul>
 *
 * <a id="Termindex"></a>
 *
 * <h2>Term Index</h2>
 *
 * <p>The .tip file contains an index into the term dictionary, so that it can be accessed randomly.
 * The index is also used to determine when a given term cannot exist on disk (in the .tim file),
 * saving a disk seek.
 *
 * <ul>
 *   <li>TermsIndex (.tip) --&gt; Header, FSTIndex<sup>NumFields</sup>
 *       &lt;IndexStartFP&gt;<sup>NumFields</sup>, DirOffset, Footer
 *   <li>Header --&gt; {@link CodecUtil#writeHeader CodecHeader}
 *   <li>DirOffset --&gt; {@link DataOutput#writeLong Uint64}
 *   <li>IndexStartFP --&gt; {@link DataOutput#writeVLong VLong}
 *       <!-- TODO: better describe FST output here -->
 *   <li>FSTIndex --&gt; {@link FST FST&lt;byte[]&gt;}
 *   <li>Footer --&gt; {@link CodecUtil#writeFooter CodecFooter}
 * </ul>
 *
 * <p>Notes:
 *
 * <ul>
 *   <li>The .tip file contains a separate FST for each field. The FST maps a term prefix to the
 *       on-disk block that holds all terms starting with that prefix. Each field's IndexStartFP
 *       points to its FST.
 *   <li>DirOffset is a pointer to the start of the IndexStartFPs for all fields
 *   <li>It's possible that an on-disk block would contain too many terms (more than the allowed
 *       maximum (default: 48)). When this happens, the block is sub-divided into new blocks (called
 *       "floor blocks"), and then the output in the FST for the block's prefix encodes the leading
 *       byte of each sub-block, and its file pointer.
 * </ul>
 *
 * @see Lucene40BlockTreeTermsReader
 */
public final class Lucene40BlockTreeTermsWriter extends FieldsConsumer {

    /**
     * Suggested default value for the {@code minItemsInBlock} parameter to {@link
     * #Lucene40BlockTreeTermsWriter(SegmentWriteState,PostingsWriterBase,int,int)}.
     */
    public static final int DEFAULT_MIN_BLOCK_SIZE = 25;

    /**
     * Suggested default value for the {@code maxItemsInBlock} parameter to {@link
     * #Lucene40BlockTreeTermsWriter(SegmentWriteState,PostingsWriterBase,int,int)}.
     */
    public static final int DEFAULT_MAX_BLOCK_SIZE = 48;



    private final IndexOutput metaOut;
    private final IndexOutput termsOut;
    private final IndexOutput indexOut;
    final int maxDoc;
    final int minItemsInBlock;
    final int maxItemsInBlock;

    final PostingsWriterBase postingsWriter;
    final FieldInfos fieldInfos;

    private final List<ByteBuffersDataOutput> fields = new ArrayList<>();

    /**
     * Create a new writer. The number of items (terms or sub-blocks) per block will aim to be between
     * minItemsPerBlock and maxItemsPerBlock, though in some cases the blocks may be smaller than the
     * min.
     */
    public Lucene40BlockTreeTermsWriter(
        SegmentWriteState state,
        PostingsWriterBase postingsWriter,
        int minItemsInBlock,
        int maxItemsInBlock
    ) throws IOException {
        validateSettings(minItemsInBlock, maxItemsInBlock);

        this.minItemsInBlock = minItemsInBlock;
        this.maxItemsInBlock = maxItemsInBlock;

        this.maxDoc = state.segmentInfo.maxDoc();
        this.fieldInfos = state.fieldInfos;
        this.postingsWriter = postingsWriter;

        final String termsName = IndexFileNames.segmentFileName(
            state.segmentInfo.name,
            state.segmentSuffix,
            Lucene40BlockTreeTermsReader.TERMS_EXTENSION
        );
        termsOut = EndiannessReverserUtil.createOutput(state.directory, termsName, state.context);
        boolean success = false;
        IndexOutput metaOut = null, indexOut = null;
        try {
            CodecUtil.writeIndexHeader(
                termsOut,
                Lucene40BlockTreeTermsReader.TERMS_CODEC_NAME,
                Lucene40BlockTreeTermsReader.VERSION_CURRENT,
                state.segmentInfo.getId(),
                state.segmentSuffix
            );

            final String indexName = IndexFileNames.segmentFileName(
                state.segmentInfo.name,
                state.segmentSuffix,
                Lucene40BlockTreeTermsReader.TERMS_INDEX_EXTENSION
            );
            indexOut = EndiannessReverserUtil.createOutput(state.directory, indexName, state.context);
            CodecUtil.writeIndexHeader(
                indexOut,
                Lucene40BlockTreeTermsReader.TERMS_INDEX_CODEC_NAME,
                Lucene40BlockTreeTermsReader.VERSION_CURRENT,
                state.segmentInfo.getId(),
                state.segmentSuffix
            );

            final String metaName = IndexFileNames.segmentFileName(
                state.segmentInfo.name,
                state.segmentSuffix,
                Lucene40BlockTreeTermsReader.TERMS_META_EXTENSION
            );
            metaOut = EndiannessReverserUtil.createOutput(state.directory, metaName, state.context);
            CodecUtil.writeIndexHeader(
                metaOut,
                Lucene40BlockTreeTermsReader.TERMS_META_CODEC_NAME,
                Lucene40BlockTreeTermsReader.VERSION_CURRENT,
                state.segmentInfo.getId(),
                state.segmentSuffix
            );

            postingsWriter.init(metaOut, state); 

            this.metaOut = metaOut;
            this.indexOut = indexOut;
            success = true;
        } finally {
            if (success == false) {
                IOUtils.closeWhileHandlingException(metaOut, termsOut, indexOut);
            }
        }
    }

    /** Throws {@code IllegalArgumentException} if any of these settings is invalid. */
    public static void validateSettings(int minItemsInBlock, int maxItemsInBlock) {
        if (minItemsInBlock <= 1) {
            throw new IllegalArgumentException("minItemsInBlock must be >= 2; got " + minItemsInBlock);
        }
        if (minItemsInBlock > maxItemsInBlock) {
            throw new IllegalArgumentException(
                "maxItemsInBlock must be >= minItemsInBlock; got maxItemsInBlock=" + maxItemsInBlock + " minItemsInBlock=" + minItemsInBlock
            );
        }
        if (2 * (minItemsInBlock - 1) > maxItemsInBlock) {
            throw new IllegalArgumentException(
                "maxItemsInBlock must be at least 2*(minItemsInBlock-1); got maxItemsInBlock="
                    + maxItemsInBlock
                    + " minItemsInBlock="
                    + minItemsInBlock
            );
        }
    }

    @Override
    public void write(Fields fields, NormsProducer norms) throws IOException {

        String lastField = null;
        for (String field : fields) {
            assert lastField == null || lastField.compareTo(field) < 0;
            lastField = field;

            Terms terms = fields.terms(field);
            if (terms == null) {
                continue;
            }

            TermsEnum termsEnum = terms.iterator();
            TermsWriter termsWriter = new TermsWriter(fieldInfos.fieldInfo(field));
            while (true) {
                BytesRef term = termsEnum.next();

                if (term == null) {
                    break;
                }

                termsWriter.write(term, termsEnum, norms);
            }

            termsWriter.finish();

        }
    }

    static long encodeOutput(long fp, boolean hasTerms, boolean isFloor) {
        assert fp < (1L << 62);
        return (fp << 2) | (hasTerms ? Lucene40BlockTreeTermsReader.OUTPUT_FLAG_HAS_TERMS : 0) | (isFloor
            ? Lucene40BlockTreeTermsReader.OUTPUT_FLAG_IS_FLOOR
            : 0);
    }

    private static class PendingEntry {
        public final boolean isTerm;

        protected PendingEntry(boolean isTerm) {
            this.isTerm = isTerm;
        }
    }

    private static final class PendingTerm extends PendingEntry {
        public final byte[] termBytes;
        public final BlockTermState state;

        PendingTerm(BytesRef term, BlockTermState state) {
            super(true);
            this.termBytes = new byte[term.length];
            System.arraycopy(term.bytes, term.offset, termBytes, 0, term.length);
            this.state = state;
        }

        @Override
        public String toString() {
            return "TERM: " + brToString(termBytes);
        }
    }

    @SuppressWarnings("unused")
    static String brToString(BytesRef b) {
        if (b == null) {
            return "(null)";
        } else {
            try {
                return b.utf8ToString() + " " + b;
            } catch (Throwable t) {
                return b.toString();
            }
        }
    }

    @SuppressWarnings("unused")
    static String brToString(byte[] b) {
        return brToString(new BytesRef(b));
    }

    private static final class PendingBlock extends PendingEntry {
        public final BytesRef prefix;
        public final long fp;
        public FST<BytesRef> index;
        public List<FST<BytesRef>> subIndices;
        public final boolean hasTerms;
        public final boolean isFloor;
        public final int floorLeadByte;

        PendingBlock(BytesRef prefix, long fp, boolean hasTerms, boolean isFloor, int floorLeadByte, List<FST<BytesRef>> subIndices) {
            super(false);
            this.prefix = prefix;
            this.fp = fp;
            this.hasTerms = hasTerms;
            this.isFloor = isFloor;
            this.floorLeadByte = floorLeadByte;
            this.subIndices = subIndices;
        }

        @Override
        public String toString() {
            return "BLOCK: prefix=" + brToString(prefix);
        }

        public void compileIndex(List<PendingBlock> blocks, ByteBuffersDataOutput scratchBytes, IntsRefBuilder scratchIntsRef)
            throws IOException {

            assert (isFloor && blocks.size() > 1) || (isFloor == false && blocks.size() == 1) : "isFloor=" + isFloor + " blocks=" + blocks;
            assert this == blocks.get(0);

            assert scratchBytes.size() == 0;

            scratchBytes.writeVLong(encodeOutput(fp, hasTerms, isFloor));
            if (isFloor) {
                scratchBytes.writeVInt(blocks.size() - 1);
                for (int i = 1; i < blocks.size(); i++) {
                    PendingBlock sub = blocks.get(i);
                    assert sub.floorLeadByte != -1;
                    scratchBytes.writeByte((byte) sub.floorLeadByte);
                    assert sub.fp > fp;
                    scratchBytes.writeVLong((sub.fp - fp) << 1 | (sub.hasTerms ? 1 : 0));
                }
            }

            final ByteSequenceOutputs outputs = ByteSequenceOutputs.getSingleton();
            final FSTCompiler<BytesRef> fstCompiler = new FSTCompiler.Builder<>(FST.INPUT_TYPE.BYTE1, outputs).shouldShareNonSingletonNodes(
                false
            ).build();
            final byte[] bytes = scratchBytes.toArrayCopy();
            assert bytes.length > 0;
            fstCompiler.add(Util.toIntsRef(prefix, scratchIntsRef), new BytesRef(bytes, 0, bytes.length));
            scratchBytes.reset();

            for (PendingBlock block : blocks) {
                if (block.subIndices != null) {
                    for (FST<BytesRef> subIndex : block.subIndices) {
                        append(fstCompiler, subIndex, scratchIntsRef);
                    }
                    block.subIndices = null;
                }
            }

            index = fstCompiler.compile();

            assert subIndices == null;

            /*
            Writer w = new OutputStreamWriter(new FileOutputStream("out.dot"));
            Util.toDot(index, w, false, false);
            System.out.println("SAVED to out.dot");
            w.close();
            */
        }

        private void append(FSTCompiler<BytesRef> fstCompiler, FST<BytesRef> subIndex, IntsRefBuilder scratchIntsRef) throws IOException {
            final BytesRefFSTEnum<BytesRef> subIndexEnum = new BytesRefFSTEnum<>(subIndex);
            BytesRefFSTEnum.InputOutput<BytesRef> indexEnt;
            while ((indexEnt = subIndexEnum.next()) != null) {
                fstCompiler.add(Util.toIntsRef(indexEnt.input, scratchIntsRef), indexEnt.output);
            }
        }
    }

    private final ByteBuffersDataOutput scratchBytes = ByteBuffersDataOutput.newResettableInstance();
    private final IntsRefBuilder scratchIntsRef = new IntsRefBuilder();

    static final BytesRef EMPTY_BYTES_REF = new BytesRef();

    private static class StatsWriter {

        private final DataOutput out;
        private final boolean hasFreqs;
        private int singletonCount;

        StatsWriter(DataOutput out, boolean hasFreqs) {
            this.out = out;
            this.hasFreqs = hasFreqs;
        }

        void add(int df, long ttf) throws IOException {
            if (df == 1 && (hasFreqs == false || ttf == 1)) {
                singletonCount++;
            } else {
                finish();
                out.writeVInt(df << 1);
                if (hasFreqs) {
                    out.writeVLong(ttf - df);
                }
            }
        }

        void finish() throws IOException {
            if (singletonCount > 0) {
                out.writeVInt(((singletonCount - 1) << 1) | 1);
                singletonCount = 0;
            }
        }
    }

    class TermsWriter {
        private final FieldInfo fieldInfo;
        private long numTerms;
        final FixedBitSet docsSeen;
        long sumTotalTermFreq;
        long sumDocFreq;

        private final BytesRefBuilder lastTerm = new BytesRefBuilder();
        private int[] prefixStarts = new int[8];

        private final List<PendingEntry> pending = new ArrayList<>();

        private final List<PendingBlock> newBlocks = new ArrayList<>();

        private PendingTerm firstPendingTerm;
        private PendingTerm lastPendingTerm;

        /** Writes the top count entries in pending, using prevTerm to compute the prefix. */
        void writeBlocks(int prefixLength, int count) throws IOException {

            assert count > 0;


            assert prefixLength > 0 || count == pending.size();

            int lastSuffixLeadLabel = -1;

            boolean hasTerms = false;
            boolean hasSubBlocks = false;

            int start = pending.size() - count;
            int end = pending.size();
            int nextBlockStart = start;
            int nextFloorLeadLabel = -1;

            for (int i = start; i < end; i++) {

                PendingEntry ent = pending.get(i);

                int suffixLeadLabel;

                if (ent.isTerm) {
                    PendingTerm term = (PendingTerm) ent;
                    if (term.termBytes.length == prefixLength) {
                        assert lastSuffixLeadLabel == -1 : "i=" + i + " lastSuffixLeadLabel=" + lastSuffixLeadLabel;
                        suffixLeadLabel = -1;
                    } else {
                        suffixLeadLabel = term.termBytes[prefixLength] & 0xff;
                    }
                } else {
                    PendingBlock block = (PendingBlock) ent;
                    assert block.prefix.length > prefixLength;
                    suffixLeadLabel = block.prefix.bytes[block.prefix.offset + prefixLength] & 0xff;
                }

                if (suffixLeadLabel != lastSuffixLeadLabel) {
                    int itemsInBlock = i - nextBlockStart;
                    if (itemsInBlock >= minItemsInBlock && end - nextBlockStart > maxItemsInBlock) {
                        boolean isFloor = itemsInBlock < count;
                        newBlocks.add(writeBlock(prefixLength, isFloor, nextFloorLeadLabel, nextBlockStart, i, hasTerms, hasSubBlocks));

                        hasTerms = false;
                        hasSubBlocks = false;
                        nextFloorLeadLabel = suffixLeadLabel;
                        nextBlockStart = i;
                    }

                    lastSuffixLeadLabel = suffixLeadLabel;
                }

                if (ent.isTerm) {
                    hasTerms = true;
                } else {
                    hasSubBlocks = true;
                }
            }

            if (nextBlockStart < end) {
                int itemsInBlock = end - nextBlockStart;
                boolean isFloor = itemsInBlock < count;
                newBlocks.add(writeBlock(prefixLength, isFloor, nextFloorLeadLabel, nextBlockStart, end, hasTerms, hasSubBlocks));
            }

            assert newBlocks.isEmpty() == false;

            PendingBlock firstBlock = newBlocks.get(0);

            assert firstBlock.isFloor || newBlocks.size() == 1;

            firstBlock.compileIndex(newBlocks, scratchBytes, scratchIntsRef);

            pending.subList(pending.size() - count, pending.size()).clear();

            pending.add(firstBlock);

            newBlocks.clear();
        }

        private boolean allEqual(byte[] b, int startOffset, int endOffset, byte value) {
            Objects.checkFromToIndex(startOffset, endOffset, b.length);
            for (int i = startOffset; i < endOffset; ++i) {
                if (b[i] != value) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Writes the specified slice (start is inclusive, end is exclusive) from pending stack as a new
         * block. If isFloor is true, there were too many (more than maxItemsInBlock) entries sharing
         * the same prefix, and so we broke it into multiple floor blocks where we record the starting
         * label of the suffix of each floor block.
         */
        private PendingBlock writeBlock(
            int prefixLength,
            boolean isFloor,
            int floorLeadLabel,
            int start,
            int end,
            boolean hasTerms,
            boolean hasSubBlocks
        ) throws IOException {

            assert end > start;

            long startFP = termsOut.getFilePointer();

            boolean hasFloorLeadLabel = isFloor && floorLeadLabel != -1;

            final BytesRef prefix = new BytesRef(prefixLength + (hasFloorLeadLabel ? 1 : 0));
            System.arraycopy(lastTerm.get().bytes, 0, prefix.bytes, 0, prefixLength);
            prefix.length = prefixLength;


            int numEntries = end - start;
            int code = numEntries << 1;
            if (end == pending.size()) {
                code |= 1;
            }
            termsOut.writeVInt(code);


            boolean isLeafBlock = hasSubBlocks == false;


            final List<FST<BytesRef>> subIndices;

            boolean absolute = true;

            if (isLeafBlock) {
                subIndices = null;
                StatsWriter statsWriter = new StatsWriter(this.statsWriter, fieldInfo.getIndexOptions() != IndexOptions.DOCS);
                for (int i = start; i < end; i++) {
                    PendingEntry ent = pending.get(i);
                    assert ent.isTerm : "i=" + i;

                    PendingTerm term = (PendingTerm) ent;

                    assert StringHelper.startsWith(term.termBytes, prefix) : term + " prefix=" + prefix;
                    BlockTermState state = term.state;
                    final int suffix = term.termBytes.length - prefixLength;

                    suffixLengthsWriter.writeVInt(suffix);
                    suffixWriter.append(term.termBytes, prefixLength, suffix);
                    assert floorLeadLabel == -1 || (term.termBytes[prefixLength] & 0xff) >= floorLeadLabel;

                    statsWriter.add(state.docFreq, state.totalTermFreq);

                    postingsWriter.encodeTerm(metaWriter, fieldInfo, state, absolute);
                    absolute = false;
                }
                statsWriter.finish();
            } else {
                subIndices = new ArrayList<>();
                StatsWriter statsWriter = new StatsWriter(this.statsWriter, fieldInfo.getIndexOptions() != IndexOptions.DOCS);
                for (int i = start; i < end; i++) {
                    PendingEntry ent = pending.get(i);
                    if (ent.isTerm) {
                        PendingTerm term = (PendingTerm) ent;

                        assert StringHelper.startsWith(term.termBytes, prefix) : term + " prefix=" + prefix;
                        BlockTermState state = term.state;
                        final int suffix = term.termBytes.length - prefixLength;


                        suffixLengthsWriter.writeVInt(suffix << 1);
                        suffixWriter.append(term.termBytes, prefixLength, suffix);

                        statsWriter.add(state.docFreq, state.totalTermFreq);


                        postingsWriter.encodeTerm(metaWriter, fieldInfo, state, absolute);
                        absolute = false;
                    } else {
                        PendingBlock block = (PendingBlock) ent;
                        assert StringHelper.startsWith(block.prefix, prefix);
                        final int suffix = block.prefix.length - prefixLength;
                        assert StringHelper.startsWith(block.prefix, prefix);

                        assert suffix > 0;

                        suffixLengthsWriter.writeVInt((suffix << 1) | 1);
                        suffixWriter.append(block.prefix.bytes, prefixLength, suffix);


                        assert floorLeadLabel == -1 || (block.prefix.bytes[prefixLength] & 0xff) >= floorLeadLabel
                            : "floorLeadLabel=" + floorLeadLabel + " suffixLead=" + (block.prefix.bytes[prefixLength] & 0xff);
                        assert block.fp < startFP;

                        suffixLengthsWriter.writeVLong(startFP - block.fp);
                        subIndices.add(block.index);
                    }
                }
                statsWriter.finish();

                assert subIndices.size() != 0;
            }

            CompressionAlgorithm compressionAlg = CompressionAlgorithm.NO_COMPRESSION;
            if (suffixWriter.length() > 2L * numEntries && prefixLength > 2) {
                if (suffixWriter.length() > 6L * numEntries) {
                    LZ4.compress(suffixWriter.bytes(), 0, suffixWriter.length(), spareWriter, compressionHashTable);
                    if (spareWriter.size() < suffixWriter.length() - (suffixWriter.length() >>> 2)) {
                        compressionAlg = CompressionAlgorithm.LZ4;
                    }
                }
                if (compressionAlg == CompressionAlgorithm.NO_COMPRESSION) {
                    spareWriter.reset();
                    if (spareBytes.length < suffixWriter.length()) {
                        spareBytes = new byte[ArrayUtil.oversize(suffixWriter.length(), 1)];
                    }
                    if (LowercaseAsciiCompression.compress(suffixWriter.bytes(), suffixWriter.length(), spareBytes, spareWriter)) {
                        compressionAlg = CompressionAlgorithm.LOWERCASE_ASCII;
                    }
                }
            }
            long token = ((long) suffixWriter.length()) << 3;
            if (isLeafBlock) {
                token |= 0x04;
            }
            token |= compressionAlg.code;
            termsOut.writeVLong(token);
            if (compressionAlg == CompressionAlgorithm.NO_COMPRESSION) {
                termsOut.writeBytes(suffixWriter.bytes(), suffixWriter.length());
            } else {
                spareWriter.copyTo(termsOut);
            }
            suffixWriter.setLength(0);
            spareWriter.reset();

            final int numSuffixBytes = Math.toIntExact(suffixLengthsWriter.size());
            spareBytes = ArrayUtil.grow(spareBytes, numSuffixBytes);
            suffixLengthsWriter.copyTo(new ByteArrayDataOutput(spareBytes));
            suffixLengthsWriter.reset();
            if (allEqual(spareBytes, 1, numSuffixBytes, spareBytes[0])) {
                termsOut.writeVInt((numSuffixBytes << 1) | 1);
                termsOut.writeByte(spareBytes[0]);
            } else {
                termsOut.writeVInt(numSuffixBytes << 1);
                termsOut.writeBytes(spareBytes, numSuffixBytes);
            }

            final int numStatsBytes = Math.toIntExact(statsWriter.size());
            termsOut.writeVInt(numStatsBytes);
            statsWriter.copyTo(termsOut);
            statsWriter.reset();

            termsOut.writeVInt((int) metaWriter.size());
            metaWriter.copyTo(termsOut);
            metaWriter.reset();


            if (hasFloorLeadLabel) {
                prefix.bytes[prefix.length++] = (byte) floorLeadLabel;
            }

            return new PendingBlock(prefix, startFP, hasTerms, isFloor, floorLeadLabel, subIndices);
        }

        TermsWriter(FieldInfo fieldInfo) {
            this.fieldInfo = fieldInfo;
            assert fieldInfo.getIndexOptions() != IndexOptions.NONE;
            docsSeen = new FixedBitSet(maxDoc);
            postingsWriter.setField(fieldInfo);
        }

        /** Writes one term's worth of postings. */
        public void write(BytesRef text, TermsEnum termsEnum, NormsProducer norms) throws IOException {

            BlockTermState state = postingsWriter.writeTerm(text, termsEnum, docsSeen, norms);
            if (state != null) {

                assert state.docFreq != 0;
                assert fieldInfo.getIndexOptions() == IndexOptions.DOCS || state.totalTermFreq >= state.docFreq
                    : "postingsWriter=" + postingsWriter;
                pushTerm(text);

                PendingTerm term = new PendingTerm(text, state);
                pending.add(term);

                sumDocFreq += state.docFreq;
                sumTotalTermFreq += state.totalTermFreq;
                numTerms++;
                if (firstPendingTerm == null) {
                    firstPendingTerm = term;
                }
                lastPendingTerm = term;
            }
        }

        /** Pushes the new term to the top of the stack, and writes new blocks. */
        private void pushTerm(BytesRef text) throws IOException {
            int prefixLength = Arrays.mismatch(lastTerm.bytes(), 0, lastTerm.length(), text.bytes, text.offset, text.offset + text.length);
            if (prefixLength == -1) { 
                assert lastTerm.length() == 0;
                prefixLength = 0;
            }


            for (int i = lastTerm.length() - 1; i >= prefixLength; i--) {

                int prefixTopSize = pending.size() - prefixStarts[i];
                if (prefixTopSize >= minItemsInBlock) {
                    writeBlocks(i + 1, prefixTopSize);
                    prefixStarts[i] -= prefixTopSize - 1;
                }
            }

            if (prefixStarts.length < text.length) {
                prefixStarts = ArrayUtil.grow(prefixStarts, text.length);
            }

            for (int i = prefixLength; i < text.length; i++) {
                prefixStarts[i] = pending.size();
            }

            lastTerm.copyBytes(text);
        }

        public void finish() throws IOException {
            if (numTerms > 0) {

                pushTerm(new BytesRef());

                pushTerm(new BytesRef());
                writeBlocks(0, pending.size());

                assert pending.size() == 1 && pending.get(0).isTerm == false : "pending.size()=" + pending.size() + " pending=" + pending;
                final PendingBlock root = (PendingBlock) pending.get(0);
                assert root.prefix.length == 0;
                final BytesRef rootCode = root.index.getEmptyOutput();
                assert rootCode != null;

                ByteBuffersDataOutput metaOut = new ByteBuffersDataOutput();
                fields.add(metaOut);

                metaOut.writeVInt(fieldInfo.number);
                metaOut.writeVLong(numTerms);
                metaOut.writeVInt(rootCode.length);
                metaOut.writeBytes(rootCode.bytes, rootCode.offset, rootCode.length);
                assert fieldInfo.getIndexOptions() != IndexOptions.NONE;
                if (fieldInfo.getIndexOptions() != IndexOptions.DOCS) {
                    metaOut.writeVLong(sumTotalTermFreq);
                }
                metaOut.writeVLong(sumDocFreq);
                metaOut.writeVInt(docsSeen.cardinality());
                writeBytesRef(metaOut, new BytesRef(firstPendingTerm.termBytes));
                writeBytesRef(metaOut, new BytesRef(lastPendingTerm.termBytes));
                metaOut.writeVLong(indexOut.getFilePointer());
                root.index.save(metaOut, indexOut);

                /*
                if (DEBUG) {
                  final String dotFileName = segment + "_" + fieldInfo.name + ".dot";
                  Writer w = new OutputStreamWriter(new FileOutputStream(dotFileName));
                  Util.toDot(root.index, w, false, false);
                  System.out.println("SAVED to " + dotFileName);
                  w.close();
                }
                */

            } else {
                assert sumTotalTermFreq == 0 || fieldInfo.getIndexOptions() == IndexOptions.DOCS && sumTotalTermFreq == -1;
                assert sumDocFreq == 0;
                assert docsSeen.cardinality() == 0;
            }
        }

        private final ByteBuffersDataOutput suffixLengthsWriter = ByteBuffersDataOutput.newResettableInstance();
        private final BytesRefBuilder suffixWriter = new BytesRefBuilder();
        private final ByteBuffersDataOutput statsWriter = ByteBuffersDataOutput.newResettableInstance();
        private final ByteBuffersDataOutput metaWriter = ByteBuffersDataOutput.newResettableInstance();
        private final ByteBuffersDataOutput spareWriter = ByteBuffersDataOutput.newResettableInstance();
        private byte[] spareBytes = BytesRef.EMPTY_BYTES;
        private final LZ4.HighCompressionHashTable compressionHashTable = new LZ4.HighCompressionHashTable();
    }

    private boolean closed;

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        boolean success = false;
        try {
            metaOut.writeVInt(fields.size());
            for (ByteBuffersDataOutput fieldMeta : fields) {
                fieldMeta.copyTo(metaOut);
            }
            CodecUtil.writeFooter(indexOut);
            metaOut.writeLong(indexOut.getFilePointer());
            CodecUtil.writeFooter(termsOut);
            metaOut.writeLong(termsOut.getFilePointer());
            CodecUtil.writeFooter(metaOut);
            success = true;
        } finally {
            if (success) {
                IOUtils.close(metaOut, termsOut, indexOut, postingsWriter);
            } else {
                IOUtils.closeWhileHandlingException(metaOut, termsOut, indexOut, postingsWriter);
            }
        }
    }

    private static void writeBytesRef(DataOutput out, BytesRef bytes) throws IOException {
        out.writeVInt(bytes.length);
        out.writeBytes(bytes.bytes, bytes.offset, bytes.length);
    }
}
