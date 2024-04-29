/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * This project is based on a modification of https:
 */

package org.elasticsearch.tdigest;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Maintains a t-digest by collecting new points in a buffer that is then sorted occasionally and merged
 * into a sorted array that contains previously computed centroids.
 * <p>
 * This can be very fast because the cost of sorting and merging is amortized over several insertion. If
 * we keep N centroids total and have the input array is k long, then the amortized cost is something like
 * <p>
 * N/k + log k
 * <p>
 * These costs even out when N/k = log k.  Balancing costs is often a good place to start in optimizing an
 * algorithm.  For different values of compression factor, the following table shows estimated asymptotic
 * values of N and suggested values of k:
 * <table>
 * <thead>
 * <tr><td>Compression</td><td>N</td><td>k</td></tr>
 * </thead>
 * <tbody>
 * <tr><td>50</td><td>78</td><td>25</td></tr>
 * <tr><td>100</td><td>157</td><td>42</td></tr>
 * <tr><td>200</td><td>314</td><td>73</td></tr>
 * </tbody>
 * <caption>Sizing considerations for t-digest</caption>
 * </table>
 * <p>
 * The virtues of this kind of t-digest implementation include:
 * <ul>
 * <li>No allocation is required after initialization</li>
 * <li>The data structure automatically compresses existing centroids when possible</li>
 * <li>No Java object overhead is incurred for centroids since data is kept in primitive arrays</li>
 * </ul>
 * <p>
 * The current implementation takes the liberty of using ping-pong buffers for implementing the merge resulting
 * in a substantial memory penalty, but the complexity of an in place merge was not considered as worthwhile
 * since even with the overhead, the memory cost is less than 40 bytes per centroid which is much less than half
 * what the AVLTreeDigest uses and no dynamic allocation is required at all.
 */
public class MergingDigest extends AbstractTDigest {
    private int mergeCount = 0;

    private final double publicCompression;
    private final double compression;

    private int lastUsedCell;

    private double totalWeight = 0;

    private final double[] weight;
    private final double[] mean;

    private double unmergedWeight = 0;

    private int tempUsed = 0;
    private final double[] tempWeight;
    private final double[] tempMean;

    private final int[] order;

    public boolean useAlternatingSort = true;
    public boolean useTwoLevelCompression = true;

    public static boolean useWeightLimit = true;

    /**
     * Allocates a buffer merging t-digest.  This is the normally used constructor that
     * allocates default sized internal arrays.  Other versions are available, but should
     * only be used for special cases.
     *
     * @param compression The compression factor
     */
    public MergingDigest(double compression) {
        this(compression, -1);
    }

    /**
     * If you know the size of the temporary buffer for incoming points, you can use this entry point.
     *
     * @param compression Compression factor for t-digest.  Same as 1/\delta in the paper.
     * @param bufferSize  How many samples to retain before merging.
     */
    public MergingDigest(double compression, int bufferSize) {
        this(compression, bufferSize, -1);
    }

    /**
     * Fully specified constructor.  Normally only used for deserializing a buffer t-digest.
     *
     * @param compression Compression factor
     * @param bufferSize  Number of temporary centroids
     * @param size        Size of main buffer
     */
    public MergingDigest(double compression, int bufferSize, int size) {

        if (compression < 10) {
            compression = 10;
        }

        double sizeFudge = 0;
        if (useWeightLimit) {
            sizeFudge = 10;
        }

        size = (int) Math.max(compression + sizeFudge, size);

        if (bufferSize < 5 * size) {
            bufferSize = 5 * size;
        }

        double scale = Math.max(1, bufferSize / size - 1);
        if (useTwoLevelCompression == false) {
            scale = 1;
        }

        this.publicCompression = compression;
        this.compression = Math.sqrt(scale) * publicCompression;

        if (size < this.compression + sizeFudge) {
            size = (int) Math.ceil(this.compression + sizeFudge);
        }

        if (bufferSize <= 2 * size) {
            bufferSize = 2 * size;
        }

        weight = new double[size];
        mean = new double[size];

        tempWeight = new double[bufferSize];
        tempMean = new double[bufferSize];
        order = new int[bufferSize];

        lastUsedCell = 0;
    }

    @Override
    public void add(double x, long w) {
        checkValue(x);
        if (tempUsed >= tempWeight.length - lastUsedCell - 1) {
            mergeNewValues();
        }
        int where = tempUsed++;
        tempWeight[where] = w;
        tempMean[where] = x;
        unmergedWeight += w;
        if (x < min) {
            min = x;
        }
        if (x > max) {
            max = x;
        }
    }

    private void mergeNewValues() {
        mergeNewValues(compression);
    }

    private void mergeNewValues(double compression) {
        if (totalWeight == 0 && unmergedWeight == 0) {
            return;
        }
        if (unmergedWeight > 0) {
            merge(tempMean, tempWeight, tempUsed, order, unmergedWeight, useAlternatingSort & mergeCount % 2 == 1, compression);
            mergeCount++;
            tempUsed = 0;
            unmergedWeight = 0;
        }
    }

    private void merge(
        double[] incomingMean,
        double[] incomingWeight,
        int incomingCount,
        int[] incomingOrder,
        double unmergedWeight,
        boolean runBackwards,
        double compression
    ) {
        System.arraycopy(mean, 0, incomingMean, incomingCount, lastUsedCell);
        System.arraycopy(weight, 0, incomingWeight, incomingCount, lastUsedCell);
        incomingCount += lastUsedCell;

        if (incomingOrder == null) {
            incomingOrder = new int[incomingCount];
        }
        Sort.stableSort(incomingOrder, incomingMean, incomingCount);

        totalWeight += unmergedWeight;

        if (runBackwards) {
            Sort.reverse(incomingOrder, 0, incomingCount);
        }

        lastUsedCell = 0;
        mean[lastUsedCell] = incomingMean[incomingOrder[0]];
        weight[lastUsedCell] = incomingWeight[incomingOrder[0]];
        double wSoFar = 0;


        double normalizer = scale.normalizer(compression, totalWeight);
        double k1 = scale.k(0, normalizer);
        double wLimit = totalWeight * scale.q(k1 + 1, normalizer);
        for (int i = 1; i < incomingCount; i++) {
            int ix = incomingOrder[i];
            double proposedWeight = weight[lastUsedCell] + incomingWeight[ix];
            double projectedW = wSoFar + proposedWeight;
            boolean addThis;
            if (useWeightLimit) {
                double q0 = wSoFar / totalWeight;
                double q2 = (wSoFar + proposedWeight) / totalWeight;
                addThis = proposedWeight <= totalWeight * Math.min(scale.max(q0, normalizer), scale.max(q2, normalizer));
            } else {
                addThis = projectedW <= wLimit;
            }
            if (i == 1 || i == incomingCount - 1) {
                addThis = false;
            }

            if (addThis) {
                weight[lastUsedCell] += incomingWeight[ix];
                mean[lastUsedCell] = mean[lastUsedCell] + (incomingMean[ix] - mean[lastUsedCell]) * incomingWeight[ix]
                    / weight[lastUsedCell];
                incomingWeight[ix] = 0;
            } else {
                wSoFar += weight[lastUsedCell];
                if (useWeightLimit == false) {
                    k1 = scale.k(wSoFar / totalWeight, normalizer);
                    wLimit = totalWeight * scale.q(k1 + 1, normalizer);
                }

                lastUsedCell++;
                mean[lastUsedCell] = incomingMean[ix];
                weight[lastUsedCell] = incomingWeight[ix];
                incomingWeight[ix] = 0;
            }
        }
        lastUsedCell++;

        double sum = 0;
        for (int i = 0; i < lastUsedCell; i++) {
            sum += weight[i];
        }
        assert sum == totalWeight;
        if (runBackwards) {
            Sort.reverse(mean, 0, lastUsedCell);
            Sort.reverse(weight, 0, lastUsedCell);
        }
        if (totalWeight > 0) {
            min = Math.min(min, mean[0]);
            max = Math.max(max, mean[lastUsedCell - 1]);
        }
    }

    /**
     * Merges any pending inputs and compresses the data down to the public setting.
     * Note that this typically loses a bit of precision and thus isn't a thing to
     * be doing all the time. It is best done only when we want to show results to
     * the outside world.
     */
    @Override
    public void compress() {
        mergeNewValues(publicCompression);
    }

    @Override
    public long size() {
        return (long) (totalWeight + unmergedWeight);
    }

    @Override
    public double cdf(double x) {
        checkValue(x);
        mergeNewValues();

        if (lastUsedCell == 0) {
            return Double.NaN;
        }
        if (lastUsedCell == 1) {
            if (x < min) return 0;
            if (x > max) return 1;
            return 0.5;
        } else {
            if (x < min) {
                return 0;
            }
            if (Double.compare(x, min) == 0) {
                double dw = 0;
                for (int i = 0; i < lastUsedCell && Double.compare(mean[i], x) == 0; i++) {
                    dw += weight[i];
                }
                return dw / 2.0 / size();
            }

            if (x > max) {
                return 1;
            }
            if (x == max) {
                double dw = 0;
                for (int i = lastUsedCell - 1; i >= 0 && Double.compare(mean[i], x) == 0; i--) {
                    dw += weight[i];
                }
                return (size() - dw / 2.0) / size();
            }

            double left = (mean[1] - mean[0]) / 2;
            double weightSoFar = 0;

            for (int i = 0; i < lastUsedCell - 1; i++) {
                double right = (mean[i + 1] - mean[i]) / 2;
                if (x < mean[i] + right) {
                    double value = (weightSoFar + weight[i] * interpolate(x, mean[i] - left, mean[i] + right)) / size();
                    return Math.max(value, 0.0);
                }
                weightSoFar += weight[i];
                left = right;
            }

            int lastOffset = lastUsedCell - 1;
            double right = (mean[lastOffset] - mean[lastOffset - 1]) / 2;
            if (x < mean[lastOffset] + right) {
                return (weightSoFar + weight[lastOffset] * interpolate(x, mean[lastOffset] - right, mean[lastOffset] + right)) / size();
            }
            return 1;
        }
    }

    @Override
    public double quantile(double q) {
        if (q < 0 || q > 1) {
            throw new IllegalArgumentException("q should be in [0,1], got " + q);
        }
        mergeNewValues();

        if (lastUsedCell == 0) {
            return Double.NaN;
        } else if (lastUsedCell == 1) {
            return mean[0];
        }

        int n = lastUsedCell;

        final double index = q * totalWeight;

        if (index < 0) {
            return min;
        }
        if (index >= totalWeight) {
            return max;
        }

        double weightSoFar = weight[0] / 2;

        if (weight[0] > 1 && index < weightSoFar) {
            return weightedAverage(min, weightSoFar - index, mean[0], index);
        }

        if (weight[n - 1] > 1 && totalWeight - index <= weight[n - 1] / 2) {
            return max - (totalWeight - index - 1) / (weight[n - 1] / 2 - 1) * (max - mean[n - 1]);
        }

        for (int i = 0; i < n - 1; i++) {
            double dw = (weight[i] + weight[i + 1]) / 2;
            if (weightSoFar + dw > index) {
                double z1 = index - weightSoFar;
                double z2 = weightSoFar + dw - index;
                return weightedAverage(mean[i], z2, mean[i + 1], z1);
            }
            weightSoFar += dw;
        }

        assert weight[n - 1] >= 1;
        assert index >= totalWeight - weight[n - 1];

        double z1 = index - weightSoFar;
        double z2 = weight[n - 1] / 2.0 - z1;
        return weightedAverage(mean[n - 1], z1, max, z2);
    }

    @Override
    public int centroidCount() {
        mergeNewValues();
        return lastUsedCell;
    }

    @Override
    public Collection<Centroid> centroids() {
        mergeNewValues();

        return new AbstractCollection<>() {
            @Override
            public Iterator<Centroid> iterator() {
                return new Iterator<>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < lastUsedCell;
                    }

                    @Override
                    public Centroid next() {
                        Centroid rc = new Centroid(mean[i], (long) weight[i]);
                        i++;
                        return rc;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Default operation");
                    }
                };
            }

            @Override
            public int size() {
                return lastUsedCell;
            }
        };
    }

    @Override
    public double compression() {
        return publicCompression;
    }

    public ScaleFunction getScaleFunction() {
        return scale;
    }

    @Override
    public void setScaleFunction(ScaleFunction scaleFunction) {
        super.setScaleFunction(scaleFunction);
    }

    @Override
    public int byteSize() {
        return 48 + 8 * (mean.length + weight.length + tempMean.length + tempWeight.length) + 4 * order.length;
    }

    @Override
    public String toString() {
        return "MergingDigest"
            + "-"
            + getScaleFunction()
            + "-"
            + (useWeightLimit ? "weight" : "kSize")
            + "-"
            + (useAlternatingSort ? "alternating" : "stable")
            + "-"
            + (useTwoLevelCompression ? "twoLevel" : "oneLevel");
    }
}
