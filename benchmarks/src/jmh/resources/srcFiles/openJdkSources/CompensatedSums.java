/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8214761
 * @key randomness
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run testng CompensatedSums
 * @summary
 */

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

import jdk.test.lib.RandomFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CompensatedSums {

    @Test
    public void testCompensatedSums() {
        Random r = RandomFactory.getRandom();

        double naive = 0;
        double jdkSequentialStreamError = 0;
        double goodSequentialStreamError = 0;
        double jdkParallelStreamError = 0;
        double goodParallelStreamError = 0;
        double badParallelStreamError = 0;

        for (int loop = 0; loop < 100; loop++) {
            double[] rand = r.doubles(1_000_000)
                    .map(Math::log)
                    .map(x -> (Double.doubleToLongBits(x) % 2 == 0) ? x : -x)
                    .toArray();

            double[] sum = new double[2];
            for (int i=0; i < rand.length; i++) {
                sumWithCompensation(sum, rand[i]);
            }


            naive += square(DoubleStream.of(rand).reduce((x, y) -> x+y).getAsDouble() - sum[0]);

            jdkSequentialStreamError += square(DoubleStream.of(rand).sum() - sum[0]);

            goodSequentialStreamError += square(computeFinalSum(DoubleStream.of(rand).collect(doubleSupplier,objDoubleConsumer,goodCollectorConsumer)) - sum[0]);

            jdkParallelStreamError += square(DoubleStream.of(rand).parallel().sum() - sum[0]);

            goodParallelStreamError += square(computeFinalSum(DoubleStream.of(rand).parallel().collect(doubleSupplier,objDoubleConsumer,goodCollectorConsumer)) - sum[0]);

            badParallelStreamError += square(computeFinalSum(DoubleStream.of(rand).parallel().collect(doubleSupplier,objDoubleConsumer,badCollectorConsumer)) - sum[0]);


        }

        Assert.assertTrue(jdkParallelStreamError <= goodParallelStreamError);
        /*
         * Due to floating-point addition being inherently non-associative,
         * and due to the unpredictable scheduling of the threads used
         * in parallel streams, this assertion can fail intermittently,
         * hence is suppressed for now.
         */

        Assert.assertTrue(goodSequentialStreamError >= jdkSequentialStreamError);
        Assert.assertTrue(naive > jdkSequentialStreamError);
        Assert.assertTrue(naive > jdkParallelStreamError);

    }

    private static double square(double arg) {
        return arg * arg;
    }

    static double[] sumWithCompensation(double[] intermediateSum, double value) {
        double tmp = value - intermediateSum[1];
        double sum = intermediateSum[0];
        double velvel = sum + tmp; 
        intermediateSum[1] = (velvel - sum) - tmp;
        intermediateSum[0] = velvel;
        return intermediateSum;
    }

    static double computeFinalSum(double[] summands) {
        double tmp = summands[0] - summands[1];
        double simpleSum = summands[summands.length - 1];
        if (Double.isNaN(tmp) && Double.isInfinite(simpleSum))
            return simpleSum;
        else
            return tmp;
    }

    static Supplier<double[]> doubleSupplier = () -> new double[3];
    static ObjDoubleConsumer<double[]> objDoubleConsumer = (double[] ll, double d) -> {
                                                             sumWithCompensation(ll, d);
                                                             ll[2] += d;
                                                           };
    static BiConsumer<double[], double[]> badCollectorConsumer =
            (ll, rr) -> {
                sumWithCompensation(ll, rr[0]);
                sumWithCompensation(ll, rr[1]);
                ll[2] += rr[2];
            };

    static BiConsumer<double[], double[]> goodCollectorConsumer =
            (ll, rr) -> {
                sumWithCompensation(ll, rr[0]);
                sumWithCompensation(ll, -rr[1]);
                ll[2] += rr[2];
            };

}
