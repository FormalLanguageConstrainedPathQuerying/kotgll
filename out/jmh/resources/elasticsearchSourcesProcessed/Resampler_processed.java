/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.profiling;

import java.util.Random;
import java.util.random.RandomGenerator;

class Resampler {
    private final boolean requiresResampling;
    private final RandomGenerator r;
    private final double adjustedSampleRate;
    private final double p;

    Resampler(GetStackTracesRequest request, double sampleRate, long totalCount) {
        if (totalCount > request.getSampleSize() * 1.1) {
            this.requiresResampling = true;
            this.r = createRandom(request);
            this.p = (double) request.getSampleSize() / totalCount;
        } else {
            this.requiresResampling = false;
            this.r = null;
            this.p = 1.0d;
        }
        this.adjustedSampleRate = request.isAdjustSampleCount() ? sampleRate : 1.0d;
    }

    protected RandomGenerator createRandom(GetStackTracesRequest request) {
        return new Random(request.hashCode());
    }

    public int adjustSampleCount(int originalCount) {
        int rawCount;
        if (requiresResampling) {
            rawCount = 0;
            for (int i = 0; i < originalCount; i++) {
                if (r.nextDouble() < p) {
                    rawCount++;
                }
            }
        } else {
            rawCount = originalCount;
        }
        return (int) Math.floor(rawCount / (p * adjustedSampleRate));
    }
}
