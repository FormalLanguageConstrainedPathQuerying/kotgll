/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.aggs.frequentitemsets;

import org.elasticsearch.core.Releasable;
import org.elasticsearch.core.Releasables;
import org.elasticsearch.xpack.ml.aggs.frequentitemsets.TransactionStore.TopItemIds;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Item set traverser to find the next interesting item set.
 *
 * The traverser returns items that we haven't visited in this combination yet.
 *
 *  basic algorithm
 *
 *  - expand the set with the item reported by the traverser
 *  - re-calculate the count of transactions that contain the given item set
 *    - optimization: if we go down the tree, a bitset is used to skip transactions,
 *      that do not pass a previous step:
 *          if [a, b] is not in T, [a, b, c] can not be in T either
 */
final class CountingItemSetTraverser implements Releasable {

    private static final int OCCURENCES_SIZE_INCREMENT = 10;

    private final TransactionStore transactionStore;
    private final ItemSetTraverser topItemSetTraverser;
    private final TransactionStore.TopTransactionIds topTransactionIds;

    private final TransactionsLookupTable transactionsLookupTable;
    private final int cacheTraversalDepth;
    private final int cacheNumberOfTransactions;

    private final long[] transactionSkipCounts;
    private final BitSet transactionSkipList;

    private long[] occurencesStack;
    private BitSet visited;

    CountingItemSetTraverser(
        TransactionStore transactionStore,
        TopItemIds topItemIds,
        int cacheTraversalDepth,
        int cacheNumberOfTransactions,
        long minCount
    ) throws IOException {
        this.transactionStore = transactionStore;

        boolean success = false;
        try {
            this.topItemSetTraverser = new ItemSetTraverser(topItemIds);
            this.topTransactionIds = transactionStore.getTopTransactionIds();
            this.transactionsLookupTable = transactionStore.createLookupTableByTopTransactions(topItemIds, topTransactionIds);
            success = true;
        } finally {
            if (false == success) {
                close();
            }
        }

        this.cacheTraversalDepth = cacheTraversalDepth;
        this.cacheNumberOfTransactions = cacheNumberOfTransactions;
        transactionSkipCounts = new long[cacheTraversalDepth - 1];
        transactionSkipList = new BitSet((cacheTraversalDepth - 1) * cacheNumberOfTransactions);
        occurencesStack = new long[OCCURENCES_SIZE_INCREMENT];
        visited = new java.util.BitSet();
    }

    public boolean next(long earlyStopMinCount) throws IOException {

        if (topItemSetTraverser.next() == false) {
            return false;
        }

        final long totalTransactionCount = transactionStore.getTotalTransactionCount();

        int depth = topItemSetTraverser.getNumberOfItems();
        long occurencesOfSingleItem = transactionStore.getItemCount(topItemSetTraverser.getItemId());

        if (depth == 1) {
            occurencesStack[0] = occurencesOfSingleItem;
            return true;
        } else if (occurencesOfSingleItem < earlyStopMinCount) {
            rememberCountInStack(depth, occurencesOfSingleItem);
            return true;
        } else if (depth < cacheTraversalDepth) {
            long skipCount = transactionSkipCounts[depth - 2];

            long maxReachableTransactionCount = totalTransactionCount - skipCount;

            transactionSkipList.clear((depth - 1) * cacheNumberOfTransactions, ((depth) * cacheNumberOfTransactions));

            int topTransactionPos = 0;
            long occurrences = 0;

            while (topTransactionPos < topTransactionIds.size()) {
                if (topTransactionPos < cacheNumberOfTransactions
                    && transactionSkipList.get(cacheNumberOfTransactions * (depth - 2) + topTransactionPos)) {
                    transactionSkipList.set(cacheNumberOfTransactions * (depth - 1) + topTransactionPos);
                    topTransactionPos++;
                    continue;
                }

                long transactionCount = transactionStore.getTransactionCount(topTransactionIds.getItemIdAt(topTransactionPos));

                if (transactionsLookupTable.isSubsetOf(topTransactionPos, topItemSetTraverser.getItemSetBitSet())) {
                    occurrences += transactionCount;
                } else if (topTransactionPos < cacheNumberOfTransactions) {
                    skipCount += transactionCount;
                    transactionSkipList.set(cacheNumberOfTransactions * (depth - 1) + topTransactionPos);
                }

                maxReachableTransactionCount -= transactionCount;
                if (maxReachableTransactionCount + occurrences < earlyStopMinCount) {
                    break;
                }

                topTransactionPos++;
            }
            transactionSkipCounts[depth - 1] = skipCount;

            rememberCountInStack(depth, occurrences);
            return true;
        }


        long skipCount = transactionSkipCounts[cacheTraversalDepth - 2];

        long maxReachableTransactionCount = totalTransactionCount - skipCount;

        int transactionNumber = 0;
        long occurrences = 0;
        for (Long transactionId : topTransactionIds) {
            if (transactionNumber < cacheNumberOfTransactions
                && transactionSkipList.get(cacheNumberOfTransactions * (cacheTraversalDepth - 2) + transactionNumber)) {
                transactionNumber++;

                continue;
            }
            long transactionCount = transactionStore.getTransactionCount(transactionId);

            if (transactionsLookupTable.isSubsetOf(transactionNumber, topItemSetTraverser.getItemSetBitSet())) {
                occurrences += transactionCount;
            }

            maxReachableTransactionCount -= transactionCount;

            if (maxReachableTransactionCount + occurrences < earlyStopMinCount) {
                break;
            }

            transactionNumber++;
        }

        rememberCountInStack(depth, occurrences);
        return true;
    }

    /**
     * Get the count of the current item set
     */
    public long getCount() {
        if (topItemSetTraverser.getNumberOfItems() > 0) {
            return occurencesStack[topItemSetTraverser.getNumberOfItems() - 1];
        }
        return 0;
    }

    /**
     * Get the count of the item set without the last item
     */
    public long getParentCount() {
        if (topItemSetTraverser.getNumberOfItems() > 1) {
            return occurencesStack[topItemSetTraverser.getNumberOfItems() - 2];
        }
        return 0;
    }

    public boolean hasBeenVisited() {
        if (topItemSetTraverser.getNumberOfItems() > 0) {
            return visited.get(topItemSetTraverser.getNumberOfItems() - 1);
        }
        return true;
    }

    public boolean hasParentBeenVisited() {
        if (topItemSetTraverser.getNumberOfItems() > 1) {
            return visited.get(topItemSetTraverser.getNumberOfItems() - 2);
        }
        return true;
    }

    public void setVisited() {
        if (topItemSetTraverser.getNumberOfItems() > 0) {
            visited.set(topItemSetTraverser.getNumberOfItems() - 1);
        }
    }

    public void setParentVisited() {
        if (topItemSetTraverser.getNumberOfItems() > 1) {
            visited.set(topItemSetTraverser.getNumberOfItems() - 2);
        }
    }

    /**
     * Get the number of items in the current set
     */
    public int getNumberOfItems() {
        return topItemSetTraverser.getNumberOfItems();
    }

    /**
     *
     * Get a bitset representation of the current item set
     */
    public ItemSetBitSet getItemSetBitSet() {
        return topItemSetTraverser.getItemSetBitSet();
    }

    public ItemSetBitSet getParentItemSetBitSet() {
        return topItemSetTraverser.getParentItemSetBitSet();
    }

    /**
     * Prune the traversal. This stops exploring the current branch
     */
    public void prune() {
        topItemSetTraverser.prune();
    }

    public void pruneToNextMainBranch() {
        long thisCount = getCount();

        while (getNumberOfItems() > 1 && getCount() == thisCount) {
            topItemSetTraverser.prune();
        }
    }

    /**
     * Return true if the item set tree is on a leaf, which mean no further items can be added to the candidate set.
     */
    public boolean atLeaf() {
        return topItemSetTraverser.atLeaf();
    }

    @Override
    public void close() {
        Releasables.close(topTransactionIds, transactionsLookupTable);
    }

    private void rememberCountInStack(int index, long occurences) {
        if (occurencesStack.length < index) {
            occurencesStack = Arrays.copyOf(occurencesStack, index + OCCURENCES_SIZE_INCREMENT);
        }

        occurencesStack[index - 1] = occurences;
        visited.clear(index - 1);
    }
}
