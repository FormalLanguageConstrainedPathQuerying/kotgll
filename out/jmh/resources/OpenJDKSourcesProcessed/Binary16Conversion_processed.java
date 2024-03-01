/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8289551 8302976
 * @summary Verify conversion between float and the binary16 format
 * @requires (vm.cpu.features ~= ".*avx512vl.*" | vm.cpu.features ~= ".*f16c.*") | os.arch=="aarch64"
 *           | (os.arch == "riscv64" & vm.cpu.features ~= ".*zfh,.*")
 * @requires vm.compiler1.enabled & vm.compiler2.enabled
 * @requires vm.compMode != "Xcomp"
 * @comment default run
 * @run main Binary16Conversion
 * @comment C1 JIT compilation only:
 * @run main/othervm -Xcomp -XX:TieredStopAtLevel=1 -XX:CompileCommand=compileonly,Binary16Conversion::test* Binary16Conversion
 * @comment C2 JIT compilation only:
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileCommand=compileonly,Binary16Conversion::test* Binary16Conversion
 */

public class Binary16Conversion {

    public static final int FLOAT_SIGNIFICAND_WIDTH   = 24;

    public static void main(String... argv) {
        System.out.println("Start ...");
        short s = Float.floatToFloat16(0.0f); 

        int errors = 0;
        errors += testBinary16RoundTrip();
        errors += testBinary16CardinalValues();
        errors += testRoundFloatToBinary16();
        errors += testRoundFloatToBinary16HalfWayCases();
        errors += testRoundFloatToBinary16FullBinade();
        errors += testAlternativeImplementation();

        if (errors > 0)
            throw new RuntimeException(errors + " errors");
    }

    /*
     * Put all 16-bit values through a conversion loop and make sure
     * the values are preserved (NaN bit patterns notwithstanding).
     */
    private static int testBinary16RoundTrip() {
        int errors = 0;
        for (int i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
            short s = (short)i;
            float f =  Float.float16ToFloat(s);
            short s2 = Float.floatToFloat16(f);

            if (!Binary16.equivalent(s, s2)) {
                errors++;
                System.out.println("Roundtrip failure on " +
                                   Integer.toHexString(0xFFFF & (int)s) +
                                   "\t got back " + Integer.toHexString(0xFFFF & (int)s2));
            }
        }
        return errors;
    }

    private static int testBinary16CardinalValues() {
        int errors = 0;
        float[][] testCases = {
            {Binary16.POSITIVE_ZERO,         +0.0f},
            {Binary16.MIN_VALUE,              0x1.0p-24f},
            {Binary16.MAX_SUBNORMAL,          0x1.ff8p-15f},
            {Binary16.MIN_NORMAL,             0x1.0p-14f},
            {Binary16.ONE,                    1.0f},
            {Binary16.MAX_VALUE,              65504.0f},
            {Binary16.POSITIVE_INFINITY,      Float.POSITIVE_INFINITY},
        };


        for (var testCase : testCases) {
            errors += compareAndReportError((short)testCase[0],
                                            testCase[1]);
        }

        for (var testCase : testCases) {
            errors += compareAndReportError(testCase[1],
                                            (short)testCase[0]);
        }

        return errors;
    }

    private static int testRoundFloatToBinary16() {
        int errors = 0;

        float[][] testCases = {

            {0x1.ff8000p-1f,       (short)0x3bfe},              
            {0x1.ff8010p-1f,       (short)0x3bfe},              
            {0x1.ffa000p-1f,       (short)0x3bfe},              
            {0x1.ffa010p-1f,       (short)0x3bff},              

            {0x1.ffc000p-1f,       Binary16.ONE-1},             
            {0x1.ffc010p-1f,       Binary16.ONE-1},             
            {0x1.ffe000p-1f,       Binary16.ONE},               
            {0x1.ffe010p-1f,       Binary16.ONE},               

            {0x1.ff8000p-15f,      Binary16.MAX_SUBNORMAL},     
            {0x1.ff8010p-15f,      Binary16.MAX_SUBNORMAL},     
            {0x1.ffc000p-15f,      Binary16.MIN_NORMAL},        
            {0x1.ffc010p-15f,      Binary16.MIN_NORMAL},        

            {0x1.fffffep-26f,      Binary16.POSITIVE_ZERO},     
            {0x1.000000p-25f,      Binary16.POSITIVE_ZERO},
            {0x1.000002p-25f,      Binary16.MIN_VALUE},         
            {0x1.100000p-25f,      Binary16.MIN_VALUE},

            {0x1.ffc000p15f,       Binary16.MAX_VALUE},         
            {0x1.ffc010p15f,       Binary16.MAX_VALUE},         
            {0x1.ffe000p15f,       Binary16.POSITIVE_INFINITY}, 
            {0x1.ffe010p15f,       Binary16.POSITIVE_INFINITY}, 
        };

        for (var testCase : testCases) {
            errors += compareAndReportError(testCase[0],
                                            (short)testCase[1]);
        }
        return errors;
    }

    private static int testRoundFloatToBinary16HalfWayCases() {
        int errors = 0;


        for (int i = Binary16.POSITIVE_ZERO; 
             i    <= Binary16.MAX_VALUE;     
             i += 2) {     
            short lower = (short) i;
            short upper = (short)(i+1);

            float lowerFloat = Float.float16ToFloat(lower);
            float upperFloat = Float.float16ToFloat(upper);
            assert lowerFloat < upperFloat;

            float midway = (lowerFloat + upperFloat) * 0.5f; 

            errors += compareAndReportError(Math.nextUp(lowerFloat),   lower);
            errors += compareAndReportError(Math.nextDown(midway),     lower);

            errors += compareAndReportError(              midway,      lower);

            errors += compareAndReportError(Math.nextUp(  midway),     upper);
            errors += compareAndReportError(Math.nextDown(upperFloat), upper);
        }

        float binary16_MAX_VALUE = Float.float16ToFloat(Binary16.MAX_VALUE);
        float binary16_MAX_VALUE_halfUlp = binary16_MAX_VALUE + 16.0f;

        errors += compareAndReportError(Math.nextDown(binary16_MAX_VALUE), Binary16.MAX_VALUE);
        errors += compareAndReportError(              binary16_MAX_VALUE,  Binary16.MAX_VALUE);
        errors += compareAndReportError(Math.nextUp(  binary16_MAX_VALUE), Binary16.MAX_VALUE);

        errors += compareAndReportError(Math.nextDown(binary16_MAX_VALUE_halfUlp), Binary16.MAX_VALUE);
        errors += compareAndReportError(              binary16_MAX_VALUE_halfUlp,  Binary16.POSITIVE_INFINITY);
        errors += compareAndReportError(Math.nextUp(  binary16_MAX_VALUE_halfUlp), Binary16.POSITIVE_INFINITY);

        return errors;
    }

    private static int compareAndReportError(float input,
                                             short expected) {
        return compareAndReportError0( input,                 expected) +
               compareAndReportError0(-input, Binary16.negate(expected));
    }

    private static int compareAndReportError0(float input,
                                              short expected) {
        short actual = Float.floatToFloat16(input);
        if (!Binary16.equivalent(actual, expected)) {
            System.out.println("Unexpected result of converting " +
                               Float.toHexString(input) +
                               " to short. Expected 0x" + Integer.toHexString(0xFFFF & expected) +
                               " got 0x" + Integer.toHexString(0xFFFF & actual));
            return 1;
            }
        return 0;
    }

    private static int compareAndReportError0(short input,
                                              float expected) {
        float actual = Float.float16ToFloat(input);
        if (Float.compare(actual, expected) != 0) {
            System.out.println("Unexpected result of converting " +
                               Integer.toHexString(input & 0xFFFF) +
                               " to float. Expected " + Float.toHexString(expected) +
                               " got " + Float.toHexString(actual));
            return 1;
            }
        return 0;
    }

    private static int compareAndReportError(short input,
                                             float expected) {
        return compareAndReportError0(                input,   expected) +
               compareAndReportError0(Binary16.negate(input), -expected);
    }

    private static int testRoundFloatToBinary16FullBinade() {
        int errors = 0;

        short previous = (short)0;
        for (int i = Float.floatToIntBits(1.0f);
             i <= Float.floatToIntBits(Math.nextDown(2.0f));
             i++) {

            float f = Float.intBitsToFloat(i);
            short f_as_bin16 = Float.floatToFloat16(f);
            short f_as_bin16_down = (short)(f_as_bin16 - 1);
            short f_as_bin16_up   = (short)(f_as_bin16 + 1);


            if (f_as_bin16 < previous) {
                errors++;
                System.out.println("Semi-monotonicity violation observed on loat: " + Float.toHexString(f) + "/" + Integer.toHexString(i) + " " +
                                   Integer.toHexString(0xffff & f_as_bin16) + " previous: " + Integer.toHexString(0xffff & previous) + " f_as_bin16: " + Integer.toHexString(0xffff & f_as_bin16));
            }


            float f_prime_down = Float.float16ToFloat(f_as_bin16_down);
            float f_prime      = Float.float16ToFloat(f_as_bin16);
            float f_prime_up   = Float.float16ToFloat(f_as_bin16_up);

            previous = f_as_bin16;

            float f_prime_diff = Math.abs(f - f_prime);
            if (f_prime_diff == 0.0) {
                continue;
            }
            float f_prime_down_diff = Math.abs(f - f_prime_down);
            float f_prime_up_diff   = Math.abs(f - f_prime_up);

            if (f_prime_diff > f_prime_down_diff ||
                f_prime_diff > f_prime_up_diff) {
                errors++;
                System.out.println("Round-to-nearest violation on converting " +
                                   Float.toHexString(f) + "/" + Integer.toHexString(i) + " to binary16 and back: " +
                                   Integer.toHexString(0xffff & f_as_bin16) + " f_prime: " + Float.toHexString(f_prime));
            }
        }
        return errors;
    }

    private static int testAlternativeImplementation() {
        int errors = 0;


        for (long ell   = Float.floatToIntBits(2.0f);
             ell       <= Float.floatToIntBits(4.0f);
             ell++) {
            float f = Float.intBitsToFloat((int)ell);
            short s1 = Float.floatToFloat16(f);
            short s2 = testAltFloatToFloat16(f);

            if (s1 != s2) {
                errors++;
                System.out.println("Different conversion of float value (" + f + "/" +
                                    Integer.toHexString(Float.floatToRawIntBits(f)) + "): " +
                                    Integer.toHexString(s1 & 0xffff) + "(" + s1 + ")" + " != " +
                                    Integer.toHexString(s2 & 0xffff) + "(" + s2 + ")");
            }
        }

        return errors;
    }

    /*
     * Rely on float operations to do rounding in both normal and
     * subnormal binary16 cases.
     */
    public static short testAltFloatToFloat16(float f) {
        int doppel = Float.floatToRawIntBits(f);
        short sign_bit = (short)((doppel & 0x8000_0000) >> 16);

        if (Float.isNaN(f)) {
            return (short)(sign_bit
                    | 0x7c00 
                    | (doppel & 0x007f_e000) >> 13 
                    | (doppel & 0x0000_1ff0) >> 4  
                    | (doppel & 0x0000_000f));     
        }

        float abs_f = Math.abs(f);

        if (abs_f >= (65504.0f + 16.0f) ) {
            return (short)(sign_bit | 0x7c00); 
        } else {
            if (abs_f <= 0x1.0p-25f) { 
                return sign_bit; 
            }

            int exp = Math.getExponent(f);
            assert -25 <= exp && exp <= 15;
            short signif_bits;

            if (exp <= -15) { 
                exp = -15; 
                float f_adjust = abs_f * 0x1.0p-125f;

                signif_bits = (short)(Float.floatToRawIntBits(f_adjust) & 0x07ff);
                return (short)(sign_bit | ( ((exp + 15) << 10) + signif_bits ) );
            } else {
                int scalingExp = -139 - exp;
                float scaled = Math.scalb(Math.scalb(f, scalingExp),
                                                       -scalingExp);
                exp = Math.getExponent(scaled);
                doppel = Float.floatToRawIntBits(scaled);

                signif_bits = (short)((doppel & 0x007f_e000) >>
                                      (FLOAT_SIGNIFICAND_WIDTH - 11));
                return (short)(sign_bit | ( ((exp + 15) << 10) | signif_bits ) );
            }
        }
    }

    public static class Binary16 {
        public static final short POSITIVE_INFINITY = (short)0x7c00;
        public static final short MAX_VALUE         = 0x7bff;
        public static final short ONE               = 0x3c00;
        public static final short MIN_NORMAL        = 0x0400;
        public static final short MAX_SUBNORMAL     = 0x03ff;
        public static final short MIN_VALUE         = 0x0001;
        public static final short POSITIVE_ZERO     = 0x0000;

        public static boolean isNaN(short binary16) {
            return ((binary16 & 0x7c00) == 0x7c00) 
                && ((binary16 & 0x03ff) != 0 );    
        }

        public static short negate(short binary16) {
            return (short)(binary16 ^ 0x8000 ); 
        }

        public static boolean equivalent(short bin16_1, short bin16_2) {
            return (bin16_1 == bin16_2) ||
                isNaN(bin16_1) && isNaN(bin16_2);
        }
    }
}
