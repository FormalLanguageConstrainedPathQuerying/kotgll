/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.aggs.categorization;

import org.apache.lucene.util.Accountable;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.search.aggregations.InternalAggregations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.lucene.util.RamUsageEstimator.NUM_BYTES_ARRAY_HEADER;
import static org.apache.lucene.util.RamUsageEstimator.NUM_BYTES_OBJECT_REF;
import static org.apache.lucene.util.RamUsageEstimator.alignObjectSize;
import static org.apache.lucene.util.RamUsageEstimator.shallowSizeOfInstance;
import static org.apache.lucene.util.RamUsageEstimator.sizeOfCollection;

/**
 * Port of the C++ class <a href="https:
 * <code>CTokenListCategory</code></a>.
 */
public class TokenListCategory implements Accountable {

    private static final long SHALLOW_SIZE = shallowSizeOfInstance(TokenListCategory.class);
    private static final long SHALLOW_SIZE_OF_ARRAY_LIST = shallowSizeOfInstance(ArrayList.class);

    /**
     * ID that's locally unique for a given {@link TokenListCategorizer}.
     */
    private final int id;

    /**
     * The weighted tokens that the category was originally created from.
     * These never change.
     */
    private final List<TokenAndWeight> baseWeightedTokenIds;

    /**
     * Cache the total weight of the base tokens.
     */
    private final int baseWeight;

    /**
     * The original length (i.e. before filtering) of the string this category
     * was originally based on.
     */
    private final int baseUnfilteredLength;

    /**
     * The maximum original length of all the strings that have been
     * classified as this category. The original length may be longer than the
     * length of the strings in passed to the addString() method, because
     * it will include the date.
     */
    private int maxUnfilteredStringLength;

    /**
     * The index into the base token IDs where the subsequence of tokens that
     * are in the same order for all strings of this category begins.
     */
    private int orderedCommonTokenBeginIndex;

    /**
     * One past the index into the base token IDs where the subsequence of
     * tokens that are in the same order for all strings of this category ends.
     */
    private int orderedCommonTokenEndIndex;

    /**
     * The unique token IDs that all strings classified to be this category
     * contain. This list must always be sorted into ascending order.
     */
    private final List<TokenAndWeight> commonUniqueTokenIds;

    /**
     * Cache the weight of the common unique tokens.
     */
    private int commonUniqueTokenWeight;

    /**
     * What was the weight of the original unique tokens (i.e. when the category
     * only represented one string)? Remembering this means we can ensure
     * that the degree of commonality doesn't fall below a certain level as
     * the number of strings classified as this category grows.
     */
    private final int origUniqueTokenWeight;

    /**
     * Number of matched strings.
     */
    private long numMatches;

    /**
     * Used at the shard level for tracking the bucket ordinal for collecting sub aggregations.
     */
    private long bucketOrd = -1;

    /**
     * Used in the reduce phase to remember all sub-aggregations for buckets that got merged into this category.
     */
    private List<InternalAggregations> subAggs = List.of();

    private long cachedSizeInBytes;

    /**
     * Create a new category.
     * @param id Locally unique category ID. This will not be unique across the cluster, but may assist an owning container class in
     *           distinguishing categories.
     * @param unfilteredLength Length of the string that this category is being created for <em>before</em> any text processing was applied.
     * @param baseWeightedTokenIds List of token IDs with weights <em>in the order they appeared in the original string</em>.
     * @param uniqueTokenIds List of unique token IDs with weights <em>sorted into ascending order of token ID</em>. Must not contain
     *                       duplicate token IDs.
     * @param numMatches Number of strings that had the <code>baseWeightedTokenIds</code> in this order.
     */
    public TokenListCategory(
        int id,
        int unfilteredLength,
        List<TokenAndWeight> baseWeightedTokenIds,
        List<TokenAndWeight> uniqueTokenIds,
        long numMatches
    ) {
        this(id, unfilteredLength, baseWeightedTokenIds, uniqueTokenIds, unfilteredLength, numMatches);
    }

    public TokenListCategory(
        int id,
        int unfilteredLength,
        List<TokenAndWeight> baseWeightedTokenIds,
        List<TokenAndWeight> uniqueTokenIds,
        int maxUnfilteredStringLength,
        long numMatches
    ) {
        this.id = id;
        this.baseWeightedTokenIds = List.copyOf(baseWeightedTokenIds);
        this.baseWeight = baseWeightedTokenIds.stream().mapToInt(TokenAndWeight::getWeight).sum();
        assert unfilteredLength > 0 : "unfiltered length must be positive, got " + unfilteredLength;
        this.baseUnfilteredLength = unfilteredLength;
        assert maxUnfilteredStringLength >= baseUnfilteredLength
            : "max unfiltered length, " + maxUnfilteredStringLength + ", is smaller than base unfiltered length, " + baseUnfilteredLength;
        this.maxUnfilteredStringLength = maxUnfilteredStringLength;
        this.orderedCommonTokenBeginIndex = 0;
        this.orderedCommonTokenEndIndex = baseWeightedTokenIds.size();
        assert uniqueTokenIds.stream().map(TokenAndWeight::getTokenId).distinct().count() == uniqueTokenIds.size()
            : "Unique token IDs contains duplicates " + uniqueTokenIds;
        assert isSorted(uniqueTokenIds) : "Unique token IDs is not sorted " + uniqueTokenIds;
        assert Sets.intersection(
            uniqueTokenIds.stream().map(TokenAndWeight::getTokenId).collect(Collectors.toSet()),
            baseWeightedTokenIds.stream().map(TokenAndWeight::getTokenId).collect(Collectors.toSet())
        ).size() == uniqueTokenIds.size() : "Some unique token IDs " + uniqueTokenIds + " are not base token IDs " + baseWeightedTokenIds;
        this.commonUniqueTokenIds = new ArrayList<>(uniqueTokenIds);
        this.commonUniqueTokenWeight = commonUniqueTokenIds.stream().mapToInt(TokenAndWeight::getWeight).sum();
        this.origUniqueTokenWeight = commonUniqueTokenWeight;
        assert numMatches > 0 : "number of matches must be positive, got " + numMatches;
        assert numMatches > 1 || maxUnfilteredStringLength == baseUnfilteredLength
            : "max unfiltered length, "
                + maxUnfilteredStringLength
                + ", is different to base unfiltered length, "
                + baseUnfilteredLength
                + ", for a category with a single match";
        this.numMatches = numMatches;
        cacheRamUsage();
    }

    public TokenListCategory(int id, SerializableTokenListCategory serializable, CategorizationBytesRefHash bytesRefHash) {
        this.id = id;
        this.baseWeightedTokenIds = IntStream.range(0, serializable.baseTokens.length)
            .mapToObj(index -> new TokenAndWeight(bytesRefHash.put(serializable.baseTokens[index]), serializable.baseTokenWeights[index]))
            .collect(Collectors.toList());
        this.baseWeight = baseWeightedTokenIds.stream().mapToInt(TokenAndWeight::getWeight).sum();
        this.baseUnfilteredLength = serializable.baseUnfilteredLength;
        this.maxUnfilteredStringLength = serializable.maxUnfilteredStringLength;
        this.orderedCommonTokenBeginIndex = serializable.orderedCommonTokenBeginIndex;
        this.orderedCommonTokenEndIndex = serializable.orderedCommonTokenEndIndex;
        this.commonUniqueTokenIds = IntStream.range(0, serializable.commonUniqueTokenIndexes.length)
            .mapToObj(
                index -> new TokenAndWeight(
                    baseWeightedTokenIds.get(serializable.commonUniqueTokenIndexes[index]).getTokenId(),
                    serializable.commonUniqueTokenWeights[index]
                )
            )
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));
        this.commonUniqueTokenWeight = commonUniqueTokenIds.stream().mapToInt(TokenAndWeight::getWeight).sum();
        this.origUniqueTokenWeight = serializable.origUniqueTokenWeight;
        this.numMatches = serializable.numMatches;
        cacheRamUsage();
    }

    public void addString(
        int unfilteredLength,
        List<TokenAndWeight> weightedTokenIds,
        List<TokenAndWeight> uniqueTokenIds,
        long numMatches
    ) {
        assert isSorted(uniqueTokenIds) : "Unique token IDs is not sorted " + uniqueTokenIds;
        assert numMatches > 0 : "number of matches must be positive, got " + numMatches;
        mergeWith(unfilteredLength, weightedTokenIds, 0, weightedTokenIds.size(), uniqueTokenIds, numMatches);
    }

    public void mergeWith(TokenListCategory other) {
        mergeWith(
            other.maxUnfilteredStringLength,
            other.baseWeightedTokenIds,
            other.orderedCommonTokenBeginIndex,
            other.orderedCommonTokenEndIndex,
            other.commonUniqueTokenIds,
            other.numMatches
        );
    }

    private void mergeWith(
        int unfilteredLength,
        List<TokenAndWeight> weightedTokenIds,
        int orderedCommonTokenBeginIndex,
        int orderedCommonTokenEndIndex,
        List<TokenAndWeight> uniqueTokenIds,
        long numMatches
    ) {
        updateCommonUniqueTokenIds(uniqueTokenIds);
        updateOrderedCommonTokenIds(weightedTokenIds, orderedCommonTokenBeginIndex, orderedCommonTokenEndIndex);
        if (unfilteredLength > maxUnfilteredStringLength) {
            maxUnfilteredStringLength = unfilteredLength;
        }
        this.numMatches += numMatches;
    }

    public void addSubAggs(InternalAggregations aggs) {
        if (subAggs.isEmpty()) {
            subAggs = new ArrayList<>();
        }
        subAggs.add(aggs);
    }

    public List<InternalAggregations> getSubAggs() {
        return subAggs;
    }

    /**
     * Updates the common unique token IDs to remove any that aren't in a new list
     * of token IDs. Since both lists are sorted the approach is to step through
     * both lists in parallel looking for differences.
     */
    private void updateCommonUniqueTokenIds(List<TokenAndWeight> newUniqueTokenIds) {
        assert commonUniqueTokenWeight == commonUniqueTokenIds.stream().mapToInt(TokenAndWeight::getWeight).sum()
            : "commonUniqueTokenWeight not up to date";

        commonUniqueTokenWeight = 0;

        int initialSize = commonUniqueTokenIds.size();
        int commonIndex = 0;
        int newIndex = 0;
        int outputIndex = 0;

        while (commonIndex < initialSize) {
            if (newIndex >= newUniqueTokenIds.size()) {
                ++commonIndex;
                continue;
            }
            TokenAndWeight commonTokenAndWeight = commonUniqueTokenIds.get(commonIndex);
            int cmp = commonTokenAndWeight.compareTo(newUniqueTokenIds.get(newIndex));
            if (cmp < 0) {
                ++commonIndex;
                continue;
            }
            if (cmp == 0) {
                commonUniqueTokenIds.set(outputIndex++, commonTokenAndWeight);
                commonUniqueTokenWeight += commonTokenAndWeight.getWeight();
                ++commonIndex;
            }
            ++newIndex;
        }
        if (outputIndex < initialSize) {
            commonUniqueTokenIds.subList(outputIndex, initialSize).clear();
            cacheRamUsage();
        } else {
            assert outputIndex == initialSize
                : "should be impossible for output index to exceed initial size, but got " + outputIndex + " > " + initialSize;
        }
        assert commonUniqueTokenWeight == commonUniqueTokenIds.stream().mapToInt(TokenAndWeight::getWeight).sum()
            : "commonUniqueTokenWeight not up to date";
    }

    /**
     * Updates the ordered common tokens. This means updating the start and end of the
     * ordered common subsequence of the base tokens. (The base tokens are never changed,
     * we just update the pointers to the start and end of the common subsequence as more
     * examples are seen.) The highest weighted subsequence that's common to the existing
     * and new token lists is preferred, i.e. weight is preferred to length.
     * NB: This private method makes the assumption that {@link #updateCommonUniqueTokenIds}
     * was called before it in the update sequence.
     */
    void updateOrderedCommonTokenIds(List<TokenAndWeight> newTokenIds, int newBeginIndex, int newEndIndex) {

        while (orderedCommonTokenEndIndex > orderedCommonTokenBeginIndex
            && isTokenIdCommon(baseWeightedTokenIds.get(orderedCommonTokenEndIndex - 1)) == false) {
            --orderedCommonTokenEndIndex;
        }
        while (orderedCommonTokenBeginIndex < orderedCommonTokenEndIndex
            && isTokenIdCommon(baseWeightedTokenIds.get(orderedCommonTokenBeginIndex)) == false) {
            ++orderedCommonTokenBeginIndex;
        }


        int bestOrderedCommonTokenBeginIndex = orderedCommonTokenEndIndex;
        int bestOrderedCommonTokenEndIndex = orderedCommonTokenEndIndex;

        int bestWeight = 0;
        for (int tryOrderedCommonTokenBeginIndex =
            orderedCommonTokenBeginIndex; tryOrderedCommonTokenBeginIndex < orderedCommonTokenEndIndex; ++tryOrderedCommonTokenBeginIndex) {

            int newIndex = newBeginIndex;
            int tryWeight = 0;
            for (int commonIndex = tryOrderedCommonTokenBeginIndex; commonIndex < orderedCommonTokenEndIndex; ++commonIndex) {

                if (isTokenIdCommon(baseWeightedTokenIds.get(commonIndex)) == false) {
                    continue;
                }

                while (newIndex < newEndIndex) {
                    TokenAndWeight baseToken = baseWeightedTokenIds.get(commonIndex);
                    TokenAndWeight newToken = newTokenIds.get(newIndex);
                    if (newToken.getTokenId() != baseToken.getTokenId()) {
                        ++newIndex;
                    } else {
                        tryWeight += baseToken.getWeight();
                        break;
                    }
                }

                if (newIndex >= newEndIndex) {
                    if (tryWeight > bestWeight) {
                        bestWeight = tryWeight;
                        bestOrderedCommonTokenBeginIndex = tryOrderedCommonTokenBeginIndex;
                        bestOrderedCommonTokenEndIndex = commonIndex;
                    }
                    break;
                }
            }
            if (newIndex < newEndIndex) {
                if (tryWeight > bestWeight) {
                    bestWeight = tryWeight;
                    bestOrderedCommonTokenBeginIndex = tryOrderedCommonTokenBeginIndex;
                    bestOrderedCommonTokenEndIndex = orderedCommonTokenEndIndex;
                }
                break;
            }
        }
        if (orderedCommonTokenBeginIndex != bestOrderedCommonTokenBeginIndex) {
            orderedCommonTokenBeginIndex = bestOrderedCommonTokenBeginIndex;
        }
        if (orderedCommonTokenEndIndex != bestOrderedCommonTokenEndIndex) {
            orderedCommonTokenEndIndex = bestOrderedCommonTokenEndIndex;
        }
    }

    boolean isTokenIdCommon(TokenAndWeight token) {
        return Collections.binarySearch(commonUniqueTokenIds, token) >= 0;
    }

    public int getId() {
        return id;
    }

    public List<TokenAndWeight> getBaseWeightedTokenIds() {
        return baseWeightedTokenIds;
    }

    public int getBaseWeight() {
        return baseWeight;
    }

    public int getBaseUnfilteredLength() {
        return baseUnfilteredLength;
    }

    public int getMaxUnfilteredStringLength() {
        return maxUnfilteredStringLength;
    }

    public int getOrderedCommonTokenBeginIndex() {
        return orderedCommonTokenBeginIndex;
    }

    public int getOrderedCommonTokenEndIndex() {
        return orderedCommonTokenEndIndex;
    }

    public List<TokenAndWeight> getCommonUniqueTokenIds() {
        return List.copyOf(commonUniqueTokenIds);
    }

    public int getCommonUniqueTokenWeight() {
        return commonUniqueTokenWeight;
    }

    public int getOrigUniqueTokenWeight() {
        return origUniqueTokenWeight;
    }

    public long getNumMatches() {
        return numMatches;
    }

    public int maxMatchingStringLen() {
        return maxMatchingStringLen(baseUnfilteredLength, maxUnfilteredStringLength, commonUniqueTokenIds.size());
    }

    static int maxMatchingStringLen(int baseUnfilteredLength, int maxUnfilteredStringLength, int numCommonUniqueTokenIds) {
        int extendedLength = Math.min(
            (maxUnfilteredStringLength * 11) / 10,
            (int) ((float) baseUnfilteredLength * Math.max((float) numCommonUniqueTokenIds / 1.5f, 2.0f))
        );
        return Math.max(maxUnfilteredStringLength, extendedLength);
    }

    /**
     * This should get set once, after creation of the object, when it gets put into an aggregation bucket.
     */
    void setBucketOrd(long bucketOrd) {
        assert bucketOrd >= 0 : "Attempt to set bucketOrd to negative number " + bucketOrd;
        assert this.bucketOrd == -1 || this.bucketOrd == bucketOrd
            : "Attempt to change bucketOrd from " + this.bucketOrd + " to " + bucketOrd;
        this.bucketOrd = bucketOrd;
    }

    long getBucketOrd() {
        return bucketOrd;
    }

    public int missingCommonTokenWeight(List<TokenAndWeight> uniqueTokenIds) {
        assert isSorted(uniqueTokenIds) : "Unique token IDs is not sorted " + uniqueTokenIds;

        int presentWeight = 0;

        int commonIndex = 0;
        int testIndex = 0;
        while (commonIndex < commonUniqueTokenIds.size() && testIndex < uniqueTokenIds.size()) {
            TokenAndWeight commonTokenAndWeight = commonUniqueTokenIds.get(commonIndex);
            int cmp = commonTokenAndWeight.compareTo(uniqueTokenIds.get(testIndex));
            if (cmp < 0) {
                ++commonIndex;
                continue;
            }
            if (cmp == 0) {
                presentWeight += commonTokenAndWeight.getWeight();
                ++commonIndex;
            }
            ++testIndex;
        }

        return commonUniqueTokenWeight - presentWeight;
    }

    public boolean matchesSearchForCategory(TokenListCategory other) {
        return matchesSearchForCategory(
            other.baseWeight,
            other.maxUnfilteredStringLength,
            other.commonUniqueTokenIds,
            other.baseWeightedTokenIds
        );
    }

    public boolean matchesSearchForCategory(
        int otherBaseWeight,
        int otherUnfilteredStringLen,
        List<TokenAndWeight> otherUniqueTokenIds,
        List<TokenAndWeight> otherBaseTokenIds
    ) {
        return (baseWeight == 0) == (otherBaseWeight == 0)
            && maxMatchingStringLen() >= otherUnfilteredStringLen
            && isMissingCommonTokenWeightZero(otherUniqueTokenIds)
            && containsCommonInOrderTokensInOrder(otherBaseTokenIds);
    }

    /**
     * @param uniqueTokenIds <em>Must</em> be sorted!
     * @return Is every common unique token for this category present with the same weight in the supplied {@code uniqueTokenIds}?
     */
    public boolean isMissingCommonTokenWeightZero(List<TokenAndWeight> uniqueTokenIds) {
        assert isSorted(uniqueTokenIds) : "Unique token IDs is not sorted " + uniqueTokenIds;

        int uniqueTokenIdsSize = uniqueTokenIds.size();
        int testIndex = 0;
        for (TokenAndWeight commonTokenAndWeight : commonUniqueTokenIds) {
            if (testIndex >= uniqueTokenIdsSize) {
                return false;
            }
            TokenAndWeight testTokenAndWeight;
            while ((testTokenAndWeight = uniqueTokenIds.get(testIndex)).getTokenId() < commonTokenAndWeight.getTokenId()) {
                if (++testIndex >= uniqueTokenIdsSize) {
                    return false;
                }
            }
            if (testTokenAndWeight.getTokenId() != commonTokenAndWeight.getTokenId()) {
                return false;
            }
            ++testIndex;
        }

        return true;
    }

    boolean containsCommonInOrderTokensInOrder(List<TokenAndWeight> tokenIds) {

        int testIndex = 0;
        for (int index = orderedCommonTokenBeginIndex; index < orderedCommonTokenEndIndex; ++index) {
            TokenAndWeight baseTokenAndWeight = baseWeightedTokenIds.get(index);

            if (isTokenIdCommon(baseTokenAndWeight) == false) {
                continue;
            }

            do {
                if (testIndex >= tokenIds.size()) {
                    return false;
                }
            } while (tokenIds.get(testIndex++).compareTo(baseTokenAndWeight) != 0);
        }

        return true;
    }

    @Override
    public long ramBytesUsed() {
        return cachedSizeInBytes;
    }

    long ramBytesUsedSlow() {
        return SHALLOW_SIZE + sizeOfCollection(baseWeightedTokenIds) + sizeOfCollection(commonUniqueTokenIds);
    }

    private void cacheRamUsage() {
        cachedSizeInBytes = SHALLOW_SIZE
            + alignObjectSize(
                SHALLOW_SIZE_OF_ARRAY_LIST + NUM_BYTES_ARRAY_HEADER + baseWeightedTokenIds.size() * (TokenAndWeight.SHALLOW_SIZE
                    + NUM_BYTES_OBJECT_REF)
            ) + alignObjectSize(
                SHALLOW_SIZE_OF_ARRAY_LIST + NUM_BYTES_ARRAY_HEADER + commonUniqueTokenIds.size() * (TokenAndWeight.SHALLOW_SIZE
                    + NUM_BYTES_OBJECT_REF)
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            baseWeightedTokenIds,
            baseWeight,
            baseUnfilteredLength,
            maxUnfilteredStringLength,
            orderedCommonTokenBeginIndex,
            orderedCommonTokenEndIndex,
            commonUniqueTokenIds,
            commonUniqueTokenWeight,
            origUniqueTokenWeight,
            numMatches
        );
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        TokenListCategory that = (TokenListCategory) other;
        return this.id == that.id
            && Objects.equals(this.baseWeightedTokenIds, that.baseWeightedTokenIds)
            && this.baseWeight == that.baseWeight
            && this.baseUnfilteredLength == that.baseUnfilteredLength
            && this.maxUnfilteredStringLength == that.maxUnfilteredStringLength
            && this.orderedCommonTokenBeginIndex == that.orderedCommonTokenBeginIndex
            && this.orderedCommonTokenEndIndex == that.orderedCommonTokenEndIndex
            && Objects.equals(this.commonUniqueTokenIds, that.commonUniqueTokenIds)
            && this.commonUniqueTokenWeight == that.commonUniqueTokenWeight
            && this.origUniqueTokenWeight == that.origUniqueTokenWeight
            && this.numMatches == that.numMatches;
    }

    @Override
    public String toString() {
        return "Category with base tokens " + baseWeightedTokenIds + " with [" + numMatches + "] matches";
    }

    public static class TokenAndWeight implements Comparable<TokenAndWeight>, Accountable {

        private static final long SHALLOW_SIZE = shallowSizeOfInstance(TokenAndWeight.class);

        private final int tokenId;
        private final int weight;

        public TokenAndWeight(int tokenId, int weight) {
            assert tokenId >= 0 : "token ID cannot be negative, got " + tokenId;
            this.tokenId = tokenId;
            assert weight >= 0 : "weight cannot be negative, got " + weight;
            this.weight = weight;
        }

        public int getTokenId() {
            return tokenId;
        }

        public int getWeight() {
            return weight;
        }

        @Override
        public long ramBytesUsed() {
            return SHALLOW_SIZE;
        }

        @Override
        public int hashCode() {
            return Objects.hash(tokenId, weight);
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            TokenAndWeight that = (TokenAndWeight) other;
            return this.tokenId == that.tokenId && this.weight == that.weight;
        }

        /**
         * Comparison is based <em>only</em> on {@link #tokenId}.
         * Beware: this means this method returning zero is <em>not</em>
         * the same as {@link #equals} returning <code>true</code>.
         */
        @Override
        public int compareTo(TokenAndWeight other) {
            return this.tokenId - other.tokenId;
        }

        @Override
        public String toString() {
            return "{" + tokenId + ", " + weight + "}";
        }
    }

    static boolean isSorted(List<TokenAndWeight> list) {
        TokenAndWeight previousTokenAndWeight = null;
        for (TokenAndWeight tokenAndWeight : list) {
            if (previousTokenAndWeight != null && tokenAndWeight.compareTo(previousTokenAndWeight) < 0) {
                return false;
            }
            previousTokenAndWeight = tokenAndWeight;
        }
        return true;
    }
}
