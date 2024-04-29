/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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

package compiler.arraycopy.stress;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import jdk.test.whitebox.cpuinfo.CPUInfo;

/**
 * @test
 * @key stress randomness
 * @library /test/lib
 * @build compiler.arraycopy.stress.AbstractStressArrayCopy
 *        compiler.arraycopy.stress.StressBooleanArrayCopy
 *        compiler.arraycopy.stress.StressByteArrayCopy
 *        compiler.arraycopy.stress.StressCharArrayCopy
 *        compiler.arraycopy.stress.StressShortArrayCopy
 *        compiler.arraycopy.stress.StressIntArrayCopy
 *        compiler.arraycopy.stress.StressFloatArrayCopy
 *        compiler.arraycopy.stress.StressLongArrayCopy
 *        compiler.arraycopy.stress.StressDoubleArrayCopy
 *        compiler.arraycopy.stress.StressObjectArrayCopy
 *        jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 *
 * @run main/othervm/timeout=7200
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      compiler.arraycopy.stress.TestStressArrayCopy
 */
public class TestStressArrayCopy {

    static final int MAX_PARALLELISM = Integer.getInteger("maxParallelism",
        Math.max(1, Runtime.getRuntime().availableProcessors() / 4));

    private static List<String> mix(List<String> o, String... mix) {
        List<String> n = new ArrayList<>(o);
        for (String m : mix) {
            n.add(m);
        }
        return n;
    }

    private static List<List<String>> product(List<List<String>> list, String... mix) {
        List<List<String>> newList = new ArrayList<>();
        for (List<String> c : list) {
            for (String m : mix) {
                newList.add(mix(c, m));
            }
        }
        return newList;
    }

    private static List<List<String>> alternate(List<List<String>> list, String opt) {
        return product(list, "-XX:+" + opt, "-XX:-" + opt);
    }

    private static boolean containsFuzzy(List<String> list, String sub) {
        for (String s : list) {
            if (s.contains(sub)) return true;
        }
        return false;
    }

    public static void main(String... args) throws Exception {
        List<List<String>> configs = new ArrayList<>();
        List<String> cpuFeatures = CPUInfo.getFeatures();

        if (Platform.isX64() || Platform.isX86()) {
            if (cpuFeatures.isEmpty()) {
                configs.add(new ArrayList());
            }

            if (containsFuzzy(cpuFeatures, "avx512")) {
                configs.add(List.of("-XX:UseAVX=3"));
            }
            if (containsFuzzy(cpuFeatures, "avx2")) {
                configs.add(List.of("-XX:UseAVX=2"));
            }
            if (containsFuzzy(cpuFeatures, "avx")) {
                configs.add(List.of("-XX:UseAVX=1"));
            }
            if (containsFuzzy(cpuFeatures, "sse4")) {
                configs.add(List.of("-XX:UseAVX=0", "-XX:UseSSE=4"));
            }
            if (containsFuzzy(cpuFeatures, "sse3")) {
                configs.add(List.of("-XX:UseAVX=0", "-XX:UseSSE=3"));
            }
            if (containsFuzzy(cpuFeatures, "sse2")) {
                configs.add(List.of("-XX:UseAVX=0", "-XX:UseSSE=2"));
            }

            if (Platform.isX86()) {
                if (containsFuzzy(cpuFeatures, "sse")) {
                    configs.add(List.of("-XX:UseAVX=0", "-XX:UseSSE=1"));
                }

                configs.add(List.of("-XX:UseAVX=0", "-XX:UseSSE=0"));
            }

            if (Platform.isX64()) {
                configs = alternate(configs, "UseCompressedOops");
            }
            configs = alternate(configs, "UseUnalignedLoadStores");

        } else if (Platform.isAArch64()) {
            configs.add(new ArrayList());

            configs = alternate(configs, "UseCompressedOops");
            configs = alternate(configs, "UseSIMDForMemoryOps");
        } else {
            configs.add(new ArrayList());
        }

        String[] classNames = {
            "compiler.arraycopy.stress.StressBooleanArrayCopy",
            "compiler.arraycopy.stress.StressByteArrayCopy",
            "compiler.arraycopy.stress.StressCharArrayCopy",
            "compiler.arraycopy.stress.StressShortArrayCopy",
            "compiler.arraycopy.stress.StressIntArrayCopy",
            "compiler.arraycopy.stress.StressFloatArrayCopy",
            "compiler.arraycopy.stress.StressLongArrayCopy",
            "compiler.arraycopy.stress.StressDoubleArrayCopy",
            "compiler.arraycopy.stress.StressObjectArrayCopy",
        };

        ArrayList<Fork> forks = new ArrayList<>();
        int jobs = 0;

        for (List<String> c : configs) {
            for (String className : classNames) {
                {
                    ProcessBuilder pb = ProcessTools.createTestJavaProcessBuilder(mix(c, "-Xmx256m", className));
                    Process p = pb.start();
                    OutputAnalyzer oa = new OutputAnalyzer(p);
                    forks.add(new Fork(p, oa));
                    jobs++;
                }

                while (jobs >= MAX_PARALLELISM) {
                    Fork f = findDone(forks);
                    if (f != null) {
                        OutputAnalyzer oa = f.oa();
                        oa.shouldHaveExitValue(0);
                        forks.remove(f);
                        jobs--;
                    } else {
                        Thread.sleep(200);
                    }
                }
            }
        }

        for (Fork f : forks) {
            OutputAnalyzer oa = f.oa();
            oa.shouldHaveExitValue(0);
        }
    }

    private static Fork findDone(List<Fork> forks) {
        for (Fork f : forks) {
            if (!f.p().isAlive()) {
                return f;
            }
        }
        return null;
    }

    private static record Fork(Process p, OutputAnalyzer oa) {};

}
