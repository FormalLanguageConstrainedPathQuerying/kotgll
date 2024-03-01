/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.dataframe.traintestsplit;

import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.ml.dataframe.extractor.DataFrameDataExtractor;

import java.util.List;
import java.util.Random;

/**
 * This is a streaming implementation of a cross validation splitter that
 * is based on the reservoir idea. It randomly picks training docs while
 * respecting the exact training percent.
 */
abstract class AbstractReservoirTrainTestSplitter implements TrainTestSplitter {

    protected final int dependentVariableIndex;
    private final double samplingRatio;
    private final Random random;

    AbstractReservoirTrainTestSplitter(List<String> fieldNames, String dependentVariable, double trainingPercent, long randomizeSeed) {
        assert trainingPercent >= 1.0 && trainingPercent <= 100.0;
        this.dependentVariableIndex = findDependentVariableIndex(fieldNames, dependentVariable);
        this.samplingRatio = trainingPercent / 100.0;
        this.random = new Random(randomizeSeed);
    }

    private static int findDependentVariableIndex(List<String> fieldNames, String dependentVariable) {
        int dependentVariableIndex = fieldNames.indexOf(dependentVariable);
        if (dependentVariableIndex < 0) {
            throw ExceptionsHelper.serverError("Could not find dependent variable [" + dependentVariable + "] in fields " + fieldNames);
        }
        return dependentVariableIndex;
    }

    @Override
    public boolean isTraining(String[] row) {

        if (canBeUsedForTraining(row) == false) {
            return false;
        }

        SampleInfo sample = getSampleInfo(row);

        long targetSampleCount = (long) Math.max(1.0, samplingRatio * sample.classCount);

        double p = (double) (targetSampleCount - sample.training) / (sample.classCount - sample.observed);

        boolean isTraining = random.nextDouble() <= p;

        sample.observed++;
        if (isTraining) {
            sample.training++;
            return true;
        }

        return false;
    }

    private boolean canBeUsedForTraining(String[] row) {
        return row[dependentVariableIndex] != DataFrameDataExtractor.NULL_VALUE;
    }

    protected abstract SampleInfo getSampleInfo(String[] row);

    /**
     * Class count, count of docs picked for training, and count of observed
     */
    static class SampleInfo {

        private final long classCount;
        private long training;
        private long observed;

        SampleInfo(long classCount) {
            this.classCount = classCount;
        }
    }
}
