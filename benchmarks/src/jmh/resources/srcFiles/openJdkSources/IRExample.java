/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

package ir_framework.examples;

import compiler.lib.ir_framework.*;
import compiler.lib.ir_framework.driver.irmatching.IRViolationException;

/*
 * @test
 * @summary Example test to use the new test framework.
 * @library /test/lib /
 * @run driver ir_framework.examples.IRExample
 */

/**
 * The file shows some examples how IR verification can be done by using the {@link IR @IR} annotation. Additional
 * comments are provided at the IR rules to explain their purpose. A more detailed and complete description about
 * IR verification and the possibilities to write IR tests with {@link IR @IR} annotations can be found in the
 * IR framework README.md file.
 *
 * @see IR
 * @see Test
 * @see TestFramework
 */
public class IRExample {
    int iFld, iFld2, iFld3;
    public static void main(String[] args) {
        TestFramework.run(); 
        try {
            TestFramework.run(FailingExamples.class); 
        } catch (IRViolationException e) {
        }
    }

    @Test
    @IR(failOn = IRNode.LOAD) 
    @IR(failOn = {IRNode.LOAD, IRNode.LOOP}) 
    @IR(failOn = {IRNode.LOAD, "some regex that does not occur"}, 
        phase = CompilePhase.PRINT_IDEAL)
    @IR(failOn = {IRNode.STORE_OF_FIELD, "iFld2", IRNode.LOAD, IRNode.STORE_OF_CLASS, "Foo"})
    @IR(applyIf = {"UseZGC", "true"}, failOn = IRNode.LOAD)
    @IR(applyIf = {"TypeProfileLevel", ">= 100"}, failOn = IRNode.LOAD)
    public void goodFailOn() {
        iFld = 42; 
    }

    @Test
    @IR(counts = {IRNode.STORE, "2"}) 
    @IR(counts = {IRNode.LOAD, "0"}) 
    @IR(counts = {IRNode.STORE, "2",
                  IRNode.LOAD, "0"}) 
    @IR(counts = {IRNode.STORE, "2",
                  "some regex that does not occur", "0"}, 
        phase = CompilePhase.PRINT_IDEAL)
    @IR(counts = {IRNode.STORE_OF_FIELD, "iFld", "1",
                  IRNode.STORE, "2",
                  IRNode.STORE_OF_CLASS, "IRExample", "2"})
    public void goodCounts() {
        iFld = 42; 
        iFld2 = 42; 
    }

    @Test
    @IR(failOn = {IRNode.ALLOC,
                  IRNode.LOOP},
        counts = {IRNode.LOAD, "2",
                  IRNode.LOAD_OF_FIELD, "iFld2", "1",
                  IRNode.LOAD_OF_CLASS, "IRExample", "2"})
    public void mixFailOnAndCounts() {
        iFld = iFld2;
        iFld2 = iFld3;
    }

    @Test
    @IR(failOn = {IRNode.ALLOC, IRNode.LOAD})
    @IR(failOn = {IRNode.ALLOC, IRNode.LOAD}, phase = CompilePhase.AFTER_PARSING)
    @IR(counts = {IRNode.ALLOC, "0", IRNode.STORE_I, "1"}, phase = {CompilePhase.AFTER_PARSING, CompilePhase.CCP1})
    @IR(failOn = "LoadI", phase = CompilePhase.BEFORE_MATCHING)
    public void compilePhases() {
        iFld = 42;
    }

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR_I, "> 0"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE_MAX, "> 0"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "max_for_type", "> 0"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "max_int", "> 0"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE_ANY, "> 0"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "2,4,8,16,32,64", "> 0"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "min(max_for_type, max_int, LoopMaxUnroll, 64)", "> 0"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    static int[] testVectorNode() {
        int[] a = new int[1024*8];
        for (int i = 0; i < a.length; i++) {
            a[i]++;
        }
        return a;
    }

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR_F, "> 0"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE_16, "> 0"},
        applyIf = {"MaxVectorSize", "=64"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE_8, "> 0"},
        applyIf = {"MaxVectorSize", "=32"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE_4, "> 0"},
        applyIf = {"MaxVectorSize", "=16"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    static float[] testVectorNodeExactSize1() {
        float[] a = new float[1024*8];
        for (int i = 0; i < a.length; i++) {
            a[i]++;
        }
        return a;
    }

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE_4, "> 0"},
        applyIf = {"MaxVectorSize", ">=16"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(failOn = {IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE_2,
                  IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE_8,
                  IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE_16,
                  IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE_32,
                  IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE_64,
                  IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE + "2,8,16,32,64"},
        applyIf = {"MaxVectorSize", ">=16"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    static float[] testVectorNodeExactSize2() {
        float[] a = new float[1024*8];
        for (int i = 0; i < a.length/8; i++) {
            a[i*8 + 0]++; 
            a[i*8 + 1]++;
            a[i*8 + 2]++;
            a[i*8 + 3]++;
        }
        return a;
    }

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR_F, IRNode.VECTOR_SIZE + "min(8, max_float)", "> 0"},
        applyIf = {"MaxVectorSize", ">=16"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    static float[] testVectorNodeSizeMinClause() {
        float[] a = new float[1024*8];
        for (int i = 0; i < a.length/16; i++) {
            a[i*16 + 0]++; 
            a[i*16 + 1]++;
            a[i*16 + 2]++;
            a[i*16 + 3]++;
            a[i*16 + 4]++;
            a[i*16 + 5]++;
            a[i*16 + 6]++;
            a[i*16 + 7]++;
        }
        return a;
    }
}

class FailingExamples {
    int iFld2, iFld3;
    IRExample irExample = new IRExample();

    @Test
    @IR(failOn = IRNode.STORE)
    @IR(failOn = {IRNode.STORE, IRNode.LOOP}) 
    @IR(failOn = {IRNode.LOOP, IRNode.STORE}) 
    @IR(failOn = {IRNode.STORE, IRNode.LOAD}) 
    @IR(failOn = {"LoadI"}, phase = CompilePhase.PRINT_IDEAL) 
    @IR(failOn = {IRNode.STORE_OF_FIELD, "iFld", IRNode.STORE, IRNode.STORE_OF_CLASS, "IRExample"})
    public void badFailOn() {
        irExample.iFld = iFld2; 
    }


    @Test
    @IR(counts = {IRNode.STORE, "1"}) 
    @IR(counts = {IRNode.LOAD, "0"}) 
    @IR(counts = {IRNode.STORE, "1",
                  IRNode.LOAD, "1"}) 
    @IR(counts = {IRNode.LOAD, "1",
                  IRNode.STORE, "1"}) 
    @IR(counts = {"some regex that does not occur", "1"},
        phase = CompilePhase.PRINT_IDEAL) 
    @IR(counts = {IRNode.STORE_OF_FIELD, "iFld", "2", 
                  IRNode.LOAD, "2", 
                  IRNode.STORE_OF_CLASS, "Foo", "1"}) 
    public void badCounts() {
        irExample.iFld = iFld3; 
        iFld2 = 42; 
    }

    @Test
    @IR(failOn = IRNode.LOAD_I, phase = CompilePhase.BEFORE_STRINGOPTS)
    @IR(failOn = IRNode.STORE_I, phase = {CompilePhase.BEFORE_MATCHING, CompilePhase.CCP1, CompilePhase.BEFORE_STRINGOPTS,
                                         CompilePhase.AFTER_CLOOPS, CompilePhase.AFTER_PARSING})
    @IR(counts = {IRNode.STORE_I, "1"},
        phase = {CompilePhase.AFTER_PARSING, 
                 CompilePhase.ITER_GVN1}) 
    public void badCompilePhases() {
        iFld2 = 42;
        iFld2 = 42 + iFld2; 
    }

    @Test
    @IR(counts = {IRNode.STORE_I, "1"}) 
    public void testNotCompiled() {
        iFld2 = 34;
    }

    @Run(test = "testNotCompiled", mode = RunMode.STANDALONE)
    public void badStandAloneNotCompiled() {
        testNotCompiled();
    }

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR_F, "> 0"},
        applyIf = {"MaxVectorSize", ">16"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(failOn = {IRNode.LOAD_VECTOR_F},
        applyIf = {"MaxVectorSize", ">=16"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    @IR(counts = {IRNode.LOAD_VECTOR_F, "<2"},
        applyIf = {"MaxVectorSize", ">=16"},
        applyIfCPUFeatureOr = {"sse2", "true", "asimd", "true"})
    static float[] badTestVectorNodeSize() {
        float[] a = new float[1024*8];
        for (int i = 0; i < a.length/8; i++) {
            a[i*8 + 0]++; 
            a[i*8 + 1]++;
            a[i*8 + 2]++;
            a[i*8 + 3]++;
        }
        return a;
    }
}
