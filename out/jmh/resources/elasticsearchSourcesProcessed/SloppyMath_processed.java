/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.geometry.simplify;

/*
 * Based on org.apache.lucene.util.SloppyMath
 */

/** Math functions that trade off accuracy for speed. */
public class SloppyMath {

    /**
     * Returns the Haversine distance in meters between two points specified in decimal degrees
     * (latitude/longitude). This works correctly even if the dateline is between the two points.
     *
     * <p>Error is at most 4E-1 (40cm) from the actual haversine distance, but is typically much
     * smaller for reasonable distances: around 1E-5 (0.01mm) for distances less than 1000km.
     *
     * @param lat1 Latitude of the first point.
     * @param lon1 Longitude of the first point.
     * @param lat2 Latitude of the second point.
     * @param lon2 Longitude of the second point.
     * @return distance in meters.
     */
    public static double haversinMeters(double lat1, double lon1, double lat2, double lon2) {
        return haversinMeters(haversinSortKey(lat1, lon1, lat2, lon2));
    }

    /**
     * Returns the Haversine distance in meters between two points given the previous result from
     * {@link #haversinSortKey(double, double, double, double)}
     *
     * @return distance in meters.
     */
    public static double haversinMeters(double sortKey) {
        return TO_METERS * 2 * asin(Math.min(1, Math.sqrt(sortKey * 0.5)));
    }

    /**
     * Returns a sort key for distance. This is less expensive to compute than {@link
     * #haversinMeters(double, double, double, double)}, but it always compares the same. This can be
     * converted into an actual distance with {@link #haversinMeters(double)}, which effectively does
     * the second half of the computation.
     */
    public static double haversinSortKey(double lat1, double lon1, double lat2, double lon2) {
        double x1 = Math.toRadians(lat1);
        double x2 = Math.toRadians(lat2);
        double h1 = 1 - cos(x1 - x2);
        double h2 = 1 - cos(Math.toRadians(lon1 - lon2));
        double h = h1 + cos(x1) * cos(x2) * h2;
        return Double.longBitsToDouble(Double.doubleToRawLongBits(h) & 0xFFFFFFFFFFFFFFF8L);
    }

    /**
     * Returns the trigonometric cosine of an angle.
     *
     * <p>Error is around 1E-15.
     *
     * <p>Special cases:
     *
     * <ul>
     *   <li>If the argument is {@code NaN} or an infinity, then the result is {@code NaN}.
     * </ul>
     *
     * @param a an angle, in radians.
     * @return the cosine of the argument.
     * @see Math#cos(double)
     */
    public static double cos(double a) {
        if (a < 0.0) {
            a = -a;
        }
        if (a > SIN_COS_MAX_VALUE_FOR_INT_MODULO) {
            return Math.cos(a);
        }
        int index = (int) (a * SIN_COS_INDEXER + 0.5);
        double delta = (a - index * SIN_COS_DELTA_HI) - index * SIN_COS_DELTA_LO;
        index &= (SIN_COS_TABS_SIZE - 2); 
        double indexCos = cosTab[index];
        double indexSin = sinTab[index];
        return indexCos + delta * (-indexSin + delta * (-indexCos * ONE_DIV_F2 + delta * (indexSin * ONE_DIV_F3 + delta * indexCos
            * ONE_DIV_F4)));
    }

    /**
     * Returns the arc sine of a value.
     *
     * <p>The returned angle is in the range <i>-pi</i>/2 through <i>pi</i>/2. Error is around 1E-7.
     *
     * <p>Special cases:
     *
     * <ul>
     *   <li>If the argument is {@code NaN} or its absolute value is greater than 1, then the result
     *       is {@code NaN}.
     * </ul>
     *
     * @param a the value whose arc sine is to be returned.
     * @return arc sine of the argument
     * @see Math#asin(double)
     */
    public static double asin(double a) {
        boolean negateResult;
        if (a < 0.0) {
            a = -a;
            negateResult = true;
        } else {
            negateResult = false;
        }
        if (a <= ASIN_MAX_VALUE_FOR_TABS) {
            int index = (int) (a * ASIN_INDEXER + 0.5);
            double delta = a - index * ASIN_DELTA;
            double result = asinTab[index] + delta * (asinDer1DivF1Tab[index] + delta * (asinDer2DivF2Tab[index] + delta
                * (asinDer3DivF3Tab[index] + delta * asinDer4DivF4Tab[index])));
            return negateResult ? -result : result;
        } else { 
            if (a < 1.0) {
                double t = (1.0 - a) * 0.5;
                double p = t * (ASIN_PS0 + t * (ASIN_PS1 + t * (ASIN_PS2 + t * (ASIN_PS3 + t * (ASIN_PS4 + t * ASIN_PS5)))));
                double q = 1.0 + t * (ASIN_QS1 + t * (ASIN_QS2 + t * (ASIN_QS3 + t * ASIN_QS4)));
                double s = Math.sqrt(t);
                double z = s + s * (p / q);
                double result = ASIN_PIO2_HI - ((z + z) - ASIN_PIO2_LO);
                return negateResult ? -result : result;
            } else { 
                if (a == 1.0) {
                    return negateResult ? -Math.PI / 2 : Math.PI / 2;
                } else {
                    return Double.NaN;
                }
            }
        }
    }

    private static final double TO_METERS = 6_371_008.7714D; 

    private static final double ONE_DIV_F2 = 1 / 2.0;
    private static final double ONE_DIV_F3 = 1 / 6.0;
    private static final double ONE_DIV_F4 = 1 / 24.0;

    private static final double PIO2_HI = Double.longBitsToDouble(0x3FF921FB54400000L);
    private static final double PIO2_LO = Double.longBitsToDouble(0x3DD0B4611A626331L);
    private static final double TWOPI_HI = 4 * PIO2_HI;
    private static final double TWOPI_LO = 4 * PIO2_LO;
    private static final int SIN_COS_TABS_SIZE = (1 << 11) + 1;
    private static final double SIN_COS_DELTA_HI = TWOPI_HI / (SIN_COS_TABS_SIZE - 1);
    private static final double SIN_COS_DELTA_LO = TWOPI_LO / (SIN_COS_TABS_SIZE - 1);
    private static final double SIN_COS_INDEXER = 1 / (SIN_COS_DELTA_HI + SIN_COS_DELTA_LO);
    private static final double[] sinTab = new double[SIN_COS_TABS_SIZE];
    private static final double[] cosTab = new double[SIN_COS_TABS_SIZE];

    static final double SIN_COS_MAX_VALUE_FOR_INT_MODULO = ((Integer.MAX_VALUE >> 9) / SIN_COS_INDEXER) * 0.99;

    private static final double ASIN_MAX_VALUE_FOR_TABS = StrictMath.sin(Math.toRadians(73.0));

    private static final int ASIN_TABS_SIZE = (1 << 13) + 1;
    private static final double ASIN_DELTA = ASIN_MAX_VALUE_FOR_TABS / (ASIN_TABS_SIZE - 1);
    private static final double ASIN_INDEXER = 1 / ASIN_DELTA;
    private static final double[] asinTab = new double[ASIN_TABS_SIZE];
    private static final double[] asinDer1DivF1Tab = new double[ASIN_TABS_SIZE];
    private static final double[] asinDer2DivF2Tab = new double[ASIN_TABS_SIZE];
    private static final double[] asinDer3DivF3Tab = new double[ASIN_TABS_SIZE];
    private static final double[] asinDer4DivF4Tab = new double[ASIN_TABS_SIZE];

    private static final double ASIN_PIO2_HI = Double.longBitsToDouble(0x3FF921FB54442D18L);
    private static final double ASIN_PIO2_LO = Double.longBitsToDouble(0x3C91A62633145C07L);
    private static final double ASIN_PS0 = Double.longBitsToDouble(0x3fc5555555555555L);
    private static final double ASIN_PS1 = Double.longBitsToDouble(0xbfd4d61203eb6f7dL);
    private static final double ASIN_PS2 = Double.longBitsToDouble(0x3fc9c1550e884455L);
    private static final double ASIN_PS3 = Double.longBitsToDouble(0xbfa48228b5688f3bL);
    private static final double ASIN_PS4 = Double.longBitsToDouble(0x3f49efe07501b288L);
    private static final double ASIN_PS5 = Double.longBitsToDouble(0x3f023de10dfdf709L);
    private static final double ASIN_QS1 = Double.longBitsToDouble(0xc0033a271c8a2d4bL);
    private static final double ASIN_QS2 = Double.longBitsToDouble(0x40002ae59c598ac8L);
    private static final double ASIN_QS3 = Double.longBitsToDouble(0xbfe6066c1b8d0159L);
    private static final double ASIN_QS4 = Double.longBitsToDouble(0x3fb3b8c5b12e9282L);

    /* Initializes look-up tables. */
    static {
        final int SIN_COS_PI_INDEX = (SIN_COS_TABS_SIZE - 1) / 2;
        final int SIN_COS_PI_MUL_2_INDEX = 2 * SIN_COS_PI_INDEX;
        final int SIN_COS_PI_MUL_0_5_INDEX = SIN_COS_PI_INDEX / 2;
        final int SIN_COS_PI_MUL_1_5_INDEX = 3 * SIN_COS_PI_INDEX / 2;
        for (int i = 0; i < SIN_COS_TABS_SIZE; i++) {
            double angle = i * SIN_COS_DELTA_HI + i * SIN_COS_DELTA_LO;
            double sinAngle = StrictMath.sin(angle);
            double cosAngle = StrictMath.cos(angle);
            if (i == SIN_COS_PI_INDEX) {
                sinAngle = 0.0;
            } else if (i == SIN_COS_PI_MUL_2_INDEX) {
                sinAngle = 0.0;
            } else if (i == SIN_COS_PI_MUL_0_5_INDEX) {
                cosAngle = 0.0;
            } else if (i == SIN_COS_PI_MUL_1_5_INDEX) {
                cosAngle = 0.0;
            }
            sinTab[i] = sinAngle;
            cosTab[i] = cosAngle;
        }

        for (int i = 0; i < ASIN_TABS_SIZE; i++) {
            double x = i * ASIN_DELTA;
            asinTab[i] = StrictMath.asin(x);
            double oneMinusXSqInv = 1.0 / (1 - x * x);
            double oneMinusXSqInv0_5 = StrictMath.sqrt(oneMinusXSqInv);
            double oneMinusXSqInv1_5 = oneMinusXSqInv0_5 * oneMinusXSqInv;
            double oneMinusXSqInv2_5 = oneMinusXSqInv1_5 * oneMinusXSqInv;
            double oneMinusXSqInv3_5 = oneMinusXSqInv2_5 * oneMinusXSqInv;
            asinDer1DivF1Tab[i] = oneMinusXSqInv0_5;
            asinDer2DivF2Tab[i] = (x * oneMinusXSqInv1_5) * ONE_DIV_F2;
            asinDer3DivF3Tab[i] = ((1 + 2 * x * x) * oneMinusXSqInv2_5) * ONE_DIV_F3;
            asinDer4DivF4Tab[i] = ((5 + 2 * x * (2 + x * (5 - 2 * x))) * oneMinusXSqInv3_5) * ONE_DIV_F4;
        }
    }
}
