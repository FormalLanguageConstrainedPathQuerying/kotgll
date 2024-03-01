/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.aggregations.bucket.terms.heuristic;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xcontent.ObjectParser;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;

public class JLHScore extends SignificanceHeuristic {
    public static final String NAME = "jlh";
    public static final ObjectParser<JLHScore, Void> PARSER = new ObjectParser<>(NAME, JLHScore::new);

    public JLHScore() {}

    /**
     * Read from a stream.
     */
    public JLHScore(StreamInput in) {
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {}

    @Override
    public String getWriteableName() {
        return NAME;
    }

    /**
     * Calculates the significance of a term in a sample against a background of
     * normal distributions by comparing the changes in frequency. This is the heart
     * of the significant terms feature.
     */
    @Override
    public double getScore(long subsetFreq, long subsetSize, long supersetFreq, long supersetSize) {
        checkFrequencyValidity(subsetFreq, subsetSize, supersetFreq, supersetSize, "JLHScore");
        if ((subsetSize == 0) || (supersetSize == 0)) {
            return 0;
        }
        if (supersetFreq == 0) {
            supersetFreq = 1;
        }
        double subsetProbability = (double) subsetFreq / (double) subsetSize;
        double supersetProbability = (double) supersetFreq / (double) supersetSize;

        double absoluteProbabilityChange = subsetProbability - supersetProbability;
        if (absoluteProbabilityChange <= 0) {
            return 0;
        }
        double relativeProbabilityChange = (subsetProbability / supersetProbability);

        return absoluteProbabilityChange * relativeProbabilityChange;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME).endObject();
        return builder;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class JLHScoreBuilder implements SignificanceHeuristicBuilder {

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(NAME).endObject();
            return builder;
        }
    }
}
