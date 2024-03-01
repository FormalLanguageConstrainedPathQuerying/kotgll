/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.aggs.categorization;

import org.elasticsearch.xpack.ml.aggs.categorization.TokenListCategory.TokenAndWeight;

import java.util.List;

/**
 * Partial port of the C++ class
 * <a href="https:
 */
public class TokenListSimilarityTester {

    /**
     * Calculate the weighted edit distance between two sequences. Each
     * element of each sequence has an associated weight, such that some
     * elements can be considered more expensive to add/remove/replace than
     * others.
     *
     * Unfortunately, in the case of arbitrary weightings, the
     * Berghel-Roach algorithm cannot be applied. Ukkonen gives a
     * counter-example on page 114 of Information and Control, Vol 64,
     * Nos. 1-3, January/February/March 1985. The problem is that the
     * matrix diagonals are not necessarily monotonically increasing.
     * See http:
     *
     * TODO: It may be possible to apply some of the lesser optimisations
     *       from section 2 of Ukkonen's paper to this algorithm.
     */
    public static int weightedEditDistance(List<TokenAndWeight> first, List<TokenAndWeight> second) {


        int firstLen = first.size();
        int secondLen = second.size();

        if (firstLen == 0) {
            return second.stream().mapToInt(TokenAndWeight::getWeight).sum();
        }

        if (secondLen == 0) {
            return first.stream().mapToInt(TokenAndWeight::getWeight).sum();
        }

        int[] currentCol = new int[secondLen + 1];
        int[] prevCol = new int[secondLen + 1];

        currentCol[0] = 0;
        for (int downMinusOne = 0; downMinusOne < secondLen; ++downMinusOne) {
            currentCol[downMinusOne + 1] = currentCol[downMinusOne] + second.get(downMinusOne).getWeight();
        }

        for (TokenAndWeight firstTokenAndWeight : first) {
            {
                int[] temp = prevCol;
                prevCol = currentCol;
                currentCol = temp;
            }
            int firstCost = firstTokenAndWeight.getWeight();
            currentCol[0] = prevCol[0] + firstCost;

            for (int downMinusOne = 0; downMinusOne < secondLen; ++downMinusOne) {
                TokenAndWeight secondTokenAndWeight = second.get(downMinusOne);
                int secondCost = secondTokenAndWeight.getWeight();


                int option1 = prevCol[downMinusOne + 1] + firstCost;

                int option2 = currentCol[downMinusOne] + secondCost;

                int option3 = prevCol[downMinusOne] + ((firstTokenAndWeight.getTokenId() == secondTokenAndWeight.getTokenId())
                    ? 0
                    : Math.max(firstCost, secondCost));

                currentCol[downMinusOne + 1] = Math.min(Math.min(option1, option2), option3);
            }
        }

        return currentCol[secondLen];
    }
}
