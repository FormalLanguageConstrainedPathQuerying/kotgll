/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.aggs.categorization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.search.aggregations.AggregationReduceContext;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.xpack.ml.aggs.categorization.TokenListCategory.TokenAndWeight;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.apache.lucene.util.RamUsageEstimator.NUM_BYTES_ARRAY_HEADER;
import static org.apache.lucene.util.RamUsageEstimator.NUM_BYTES_OBJECT_REF;
import static org.apache.lucene.util.RamUsageEstimator.alignObjectSize;
import static org.apache.lucene.util.RamUsageEstimator.shallowSizeOfInstance;
import static org.apache.lucene.util.RamUsageEstimator.sizeOfCollection;

/**
 * Port of the C++ class <a href="https:
 * <code>CTokenListDataCategorizerBase</code></a> and parts of its base class and derived class.
 */
public class TokenListCategorizer implements Accountable {

    public static final int MAX_TOKENS = 100;
    private static final long SHALLOW_SIZE = shallowSizeOfInstance(TokenListCategorizer.class);
    private static final long SHALLOW_SIZE_OF_ARRAY_LIST = shallowSizeOfInstance(ArrayList.class);
    private static final float EPSILON = 0.000001f;
    private static final Logger logger = LogManager.getLogger(TokenListCategorizer.class);

    /**
     * The lower threshold for comparison. If another category matches this
     * closely, we'll take it providing there's no other better match.
     */
    private final float lowerThreshold;

    /**
     * The upper threshold for comparison. If another category matches this
     * closely, we accept it immediately (i.e. don't look for a better one).
     */
    private final float upperThreshold;

    private final CategorizationBytesRefHash bytesRefHash;
    @Nullable
    private final CategorizationPartOfSpeechDictionary partOfSpeechDictionary;

    /**
     * Categories stored in such a way that the most common are accessed first.
     * This is implemented as an {@link ArrayList} with bespoke ordering rather
     * than a {@link PriorityQueue} to make the regular modification and indexing
     * possible.
     */
    private final List<TokenListCategory> categoriesByNumMatches;
    private long cachedSizeInBytes;
    private long categoriesByNumMatchesContentsSize;

    public TokenListCategorizer(
        CategorizationBytesRefHash bytesRefHash,
        CategorizationPartOfSpeechDictionary partOfSpeechDictionary,
        float threshold
    ) {

        if (threshold < 0.01f || threshold > 1.0f) {
            throw new IllegalArgumentException("threshold must be between 0.01 and 1.0: got " + threshold);
        }

        this.bytesRefHash = bytesRefHash;
        this.partOfSpeechDictionary = partOfSpeechDictionary;
        this.lowerThreshold = threshold;
        this.upperThreshold = (1.0f + threshold) / 2.0f;
        this.categoriesByNumMatches = new ArrayList<>();
        cacheRamUsage(0);
    }

    public TokenListCategory computeCategory(TokenStream ts, int unfilteredStringLen, long numDocs) throws IOException {
        assert partOfSpeechDictionary != null
            : "This version of computeCategory should only be used when a part-of-speech dictionary is available";
        if (numDocs <= 0) {
            assert numDocs == 0 : "number of documents was negative: " + numDocs;
            return null;
        }
        ArrayList<TokenAndWeight> weightedTokenIds = new ArrayList<>();
        CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        WeightCalculator weightCalculator = new WeightCalculator(partOfSpeechDictionary);
        while (ts.incrementToken() && weightedTokenIds.size() < MAX_TOKENS) {
            if (termAtt.length() > 0) {
                String term = termAtt.toString();
                int weight = weightCalculator.calculateWeight(term);
                weightedTokenIds.add(new TokenAndWeight(bytesRefHash.put(new BytesRef(term.getBytes(StandardCharsets.UTF_8))), weight));
            }
        }
        if (weightedTokenIds.isEmpty()) {
            return null;
        }
        return computeCategory(weightedTokenIds, unfilteredStringLen, numDocs);
    }

    public TokenListCategory computeCategory(List<TokenAndWeight> weightedTokenIds, int unfilteredStringLen, long numDocs) {

        int workWeight = 0;
        int minReweightedTotalWeight = 0;
        int maxReweightedTotalWeight = 0;
        SortedMap<Integer, TokenAndWeight> groupingMap = new TreeMap<>();
        for (TokenAndWeight weightedTokenId : weightedTokenIds) {
            int tokenId = weightedTokenId.getTokenId();
            int weight = weightedTokenId.getWeight();
            workWeight += weight;
            minReweightedTotalWeight += WeightCalculator.getMinMatchingWeight(weight);
            maxReweightedTotalWeight += WeightCalculator.getMaxMatchingWeight(weight);
            groupingMap.compute(tokenId, (k, v) -> ((v == null) ? weightedTokenId : new TokenAndWeight(tokenId, v.getWeight() + weight)));
        }
        List<TokenAndWeight> workTokenUniqueIds = new ArrayList<>(groupingMap.values());

        return computeCategory(
            weightedTokenIds,
            workTokenUniqueIds,
            workWeight,
            minReweightedTotalWeight,
            maxReweightedTotalWeight,
            unfilteredStringLen,
            unfilteredStringLen,
            numDocs
        );
    }

    public TokenListCategory mergeWireCategory(SerializableTokenListCategory serializableCategory) {

        int sizeBefore = categoriesByNumMatches.size();
        TokenListCategory foreignCategory = new TokenListCategory(0, serializableCategory, bytesRefHash);
        TokenListCategory mergedCategory = computeCategory(
            foreignCategory.getBaseWeightedTokenIds(),
            foreignCategory.getCommonUniqueTokenIds(),
            foreignCategory.getBaseWeight(),
            WeightCalculator.getMinMatchingWeight(foreignCategory.getBaseWeight()),
            WeightCalculator.getMaxMatchingWeight(foreignCategory.getBaseWeight()),
            foreignCategory.getBaseUnfilteredLength(),
            foreignCategory.getMaxUnfilteredStringLength(),
            foreignCategory.getNumMatches()
        );
        if (logger.isDebugEnabled() && categoriesByNumMatches.size() == sizeBefore) {
            logger.debug(
                "Merged wire category [{}] into existing category to form [{}]",
                serializableCategory,
                new SerializableTokenListCategory(mergedCategory, bytesRefHash)
            );
        }
        return mergedCategory;
    }

    private synchronized TokenListCategory computeCategory(
        List<TokenAndWeight> weightedTokenIds,
        List<TokenAndWeight> workTokenUniqueIds,
        int workWeight,
        int minReweightedTotalWeight,
        int maxReweightedTotalWeight,
        int unfilteredStringLen,
        int maxUnfilteredStringLen,
        long numDocs
    ) {

        int minWeight = minMatchingWeight(minReweightedTotalWeight, lowerThreshold);
        int maxWeight = maxMatchingWeight(maxReweightedTotalWeight, lowerThreshold);

        int bestSoFarIndex = -1;
        float bestSoFarSimilarity = lowerThreshold;
        for (int index = 0; index < categoriesByNumMatches.size(); ++index) {
            TokenListCategory compCategory = categoriesByNumMatches.get(index);
            List<TokenAndWeight> baseTokenIds = compCategory.getBaseWeightedTokenIds();
            int baseWeight = compCategory.getBaseWeight();

            boolean matchesSearch = compCategory.matchesSearchForCategory(
                workWeight,
                maxUnfilteredStringLen,
                workTokenUniqueIds,
                weightedTokenIds
            );
            if (matchesSearch == false) {
                if (baseWeight < minWeight || baseWeight > maxWeight) {
                    assert baseTokenIds.equals(weightedTokenIds) == false
                        : "Min [" + minWeight + "] and/or max [" + maxWeight + "] weights calculated incorrectly " + baseTokenIds;
                    continue;
                }

                int missingCommonTokenWeight = compCategory.missingCommonTokenWeight(workTokenUniqueIds);
                if (missingCommonTokenWeight > 0) {
                    int origUniqueTokenWeight = compCategory.getOrigUniqueTokenWeight();
                    int commonUniqueTokenWeight = compCategory.getCommonUniqueTokenWeight();
                    float proportionOfOrig = (float) (commonUniqueTokenWeight - missingCommonTokenWeight) / (float) origUniqueTokenWeight;
                    if (proportionOfOrig < lowerThreshold) {
                        continue;
                    }
                }
            }

            float similarity = similarity(weightedTokenIds, workWeight, baseTokenIds, baseWeight);

            if (matchesSearch || similarity > upperThreshold) {
                if (similarity <= lowerThreshold) {
                    logger.trace(
                        "Reverse search match below threshold [{}]: orig tokens {} new tokens {}",
                        similarity,
                        compCategory.getBaseWeightedTokenIds(),
                        weightedTokenIds
                    );
                }

                return addCategoryMatch(maxUnfilteredStringLen, weightedTokenIds, workTokenUniqueIds, numDocs, index);
            }

            if (similarity > bestSoFarSimilarity) {
                bestSoFarIndex = index;
                bestSoFarSimilarity = similarity;

                minWeight = minMatchingWeight(minReweightedTotalWeight, similarity);
                maxWeight = maxMatchingWeight(maxReweightedTotalWeight, similarity);
            }
        }

        if (bestSoFarIndex >= 0) {
            return addCategoryMatch(maxUnfilteredStringLen, weightedTokenIds, workTokenUniqueIds, numDocs, bestSoFarIndex);
        }

        int newIndex = categoriesByNumMatches.size();
        TokenListCategory newCategory = new TokenListCategory(
            newIndex,
            unfilteredStringLen,
            weightedTokenIds,
            workTokenUniqueIds,
            maxUnfilteredStringLen,
            numDocs
        );
        categoriesByNumMatches.add(newCategory);
        cacheRamUsage(newCategory.ramBytesUsed());
        return repositionCategory(newCategory, newIndex);
    }

    @Override
    public long ramBytesUsed() {
        return cachedSizeInBytes;
    }

    long ramBytesUsedSlow() {
        return SHALLOW_SIZE + sizeOfCollection(categoriesByNumMatches);
    }

    private synchronized void cacheRamUsage(long contentsSizeDiff) {
        categoriesByNumMatchesContentsSize += contentsSizeDiff;
        cachedSizeInBytes = SHALLOW_SIZE
            + alignObjectSize(
                SHALLOW_SIZE_OF_ARRAY_LIST + NUM_BYTES_ARRAY_HEADER + categoriesByNumMatches.size() * NUM_BYTES_OBJECT_REF
                    + categoriesByNumMatchesContentsSize
            );
    }

    public int getCategoryCount() {
        return categoriesByNumMatches.size();
    }

    private TokenListCategory addCategoryMatch(
        int unfilteredLength,
        List<TokenAndWeight> weightedTokenIds,
        List<TokenAndWeight> uniqueTokenIds,
        long numDocs,
        int matchIndex
    ) {
        TokenListCategory category = categoriesByNumMatches.get(matchIndex);
        long previousSize = category.ramBytesUsed();
        category.addString(unfilteredLength, weightedTokenIds, uniqueTokenIds, numDocs);
        cacheRamUsage(category.ramBytesUsed() - previousSize);
        if (numDocs == 1) {
            return repositionCategory(category, matchIndex);
        }
        categoriesByNumMatches.sort(Comparator.comparing(TokenListCategory::getNumMatches).reversed());
        return category;
    }

    private TokenListCategory repositionCategory(TokenListCategory category, int currentIndex) {
        long newNumMatches = category.getNumMatches();

        int swapIndex = currentIndex;
        while (swapIndex > 0) {
            --swapIndex;
            if (newNumMatches <= categoriesByNumMatches.get(swapIndex).getNumMatches()) {
                ++swapIndex;
                break;
            }
        }

        if (swapIndex != currentIndex) {
            Collections.swap(categoriesByNumMatches, currentIndex, swapIndex);
        }
        return category;
    }

    static int minMatchingWeight(int weight, float threshold) {
        if (weight == 0) {
            return 0;
        }

        return (int) Math.floor((float) weight * threshold + EPSILON) + 1;
    }

    static int maxMatchingWeight(int weight, float threshold) {
        if (weight == 0) {
            return 0;
        }

        return (int) Math.ceil((float) weight / threshold - EPSILON) - 1;
    }

    /**
     * Compute the similarity between two vectors.
     */
    static float similarity(List<TokenAndWeight> left, int leftWeight, List<TokenAndWeight> right, int rightWeight) {
        int maxWeight = Math.max(leftWeight, rightWeight);
        if (maxWeight > 0) {
            return 1.0f - (float) TokenListSimilarityTester.weightedEditDistance(left, right) / (float) maxWeight;
        } else {
            return 1.0f;
        }
    }

    public InternalCategorizationAggregation.Bucket[] toOrderedBuckets(int size) {
        return categoriesByNumMatches.stream()
            .limit(size)
            .map(
                category -> new InternalCategorizationAggregation.Bucket(
                    new SerializableTokenListCategory(category, bytesRefHash),
                    category.getBucketOrd()
                )
            )
            .toArray(InternalCategorizationAggregation.Bucket[]::new);
    }

    public InternalCategorizationAggregation.Bucket[] toOrderedBuckets(
        int size,
        long minNumMatches,
        AggregationReduceContext reduceContext
    ) {
        return categoriesByNumMatches.stream()
            .limit(size)
            .takeWhile(category -> category.getNumMatches() >= minNumMatches)
            .map(
                category -> new InternalCategorizationAggregation.Bucket(
                    new SerializableTokenListCategory(category, bytesRefHash),
                    category.getBucketOrd(),
                    category.getSubAggs().isEmpty()
                        ? InternalAggregations.EMPTY
                        : InternalAggregations.reduce(category.getSubAggs(), reduceContext)
                )
            )
            .toArray(InternalCategorizationAggregation.Bucket[]::new);
    }

    /**
     * Equivalent to the <code>TWeightVerbs5Other2AdjacentBoost6</code> type from
     * <a href="https:
     * in the C++ code.
     */
    static class WeightCalculator {

        private static final int MIN_DICTIONARY_LENGTH = 2;
        private static final int CONSECUTIVE_DICTIONARY_WORDS_FOR_EXTRA_WEIGHT = 3;
        private static final int EXTRA_VERB_WEIGHT = 5;
        private static final int EXTRA_OTHER_DICTIONARY_WEIGHT = 2;
        private static final int ADJACENCY_BOOST_MULTIPLIER = 6;

        private final CategorizationPartOfSpeechDictionary partOfSpeechDictionary;
        private int consecutiveHighWeights;

        WeightCalculator(CategorizationPartOfSpeechDictionary partOfSpeechDictionary) {
            this.partOfSpeechDictionary = partOfSpeechDictionary;
        }

        /**
         * The idea here is that human readable phrases are more likely to define the message category, with
         * verbs being more important for distinguishing similar messages (for example, "starting" versus
         * "stopping" with other tokens being equal). Tokens that aren't in the dictionary are more likely
         * to be entity names. Therefore, the weighting prefers dictionary words to non-dictionary words,
         * prefers verbs to nouns, and prefers long uninterrupted sequences of dictionary words over short
         * sequences.
         */
        int calculateWeight(String term) {
            if (term.length() < MIN_DICTIONARY_LENGTH) {
                consecutiveHighWeights = 0;
                return 1;
            }
            CategorizationPartOfSpeechDictionary.PartOfSpeech pos = partOfSpeechDictionary.getPartOfSpeech(term);
            if (pos == CategorizationPartOfSpeechDictionary.PartOfSpeech.NOT_IN_DICTIONARY) {
                consecutiveHighWeights = 0;
                return 1;
            }
            int posWeight = (pos == CategorizationPartOfSpeechDictionary.PartOfSpeech.VERB)
                ? EXTRA_VERB_WEIGHT
                : EXTRA_OTHER_DICTIONARY_WEIGHT;
            int adjacencyBoost = (++consecutiveHighWeights >= CONSECUTIVE_DICTIONARY_WORDS_FOR_EXTRA_WEIGHT)
                ? ADJACENCY_BOOST_MULTIPLIER
                : 1;
            return 1 + (posWeight * adjacencyBoost);
        }

        static int getMinMatchingWeight(int weight) {
            return (weight <= ADJACENCY_BOOST_MULTIPLIER) ? weight : (1 + (weight - 1) / ADJACENCY_BOOST_MULTIPLIER);
        }

        static int getMaxMatchingWeight(int weight) {
            return (weight <= Math.min(EXTRA_VERB_WEIGHT, EXTRA_OTHER_DICTIONARY_WEIGHT)
                || weight > Math.max(EXTRA_VERB_WEIGHT + 1, EXTRA_OTHER_DICTIONARY_WEIGHT + 1))
                    ? weight
                    : (1 + (weight - 1) * ADJACENCY_BOOST_MULTIPLIER);
        }
    }
}
