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

/**
 * @test 8294236
 * @summary Tests different sources and combinations of preconditions.
 * @library /test/lib /
 * @requires vm.compiler2.enabled
 * @run driver ir_framework.tests.TestPreconditions
 */

package ir_framework.tests;

import compiler.lib.ir_framework.*;

public class TestPreconditions {

    public static void main(String[] args) {
        TestFramework.runWithFlags("-XX:LoopMaxUnroll=8");
    }

    @Test
    @IR(applyIf = {"LoopMaxUnroll", "= 0"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyIfOnly() {}

    @Test
    @IR(applyIfCPUFeatureAnd = {"asimd", "true", "sse", "true"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyIfCPUFeatureOnly() {}

    @Test
    @IR(applyIfCPUFeatureAnd = {"asimd", "true", "sse", "true"},
        applyIf = {"LoopMaxUnroll", "= 8"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyBoth1() {}

    @Test
    @IR(applyIfCPUFeature = {"asimd", "true"},
        applyIfAnd = {"UseSVE", "= 0", "LoopMaxUnroll", "= 0"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyBoth2() {}

    @Test
    @IR(applyIfCPUFeature = {"avx", "true"},
        applyIfAnd = {"UseAVX", "= 2", "LoopMaxUnroll", "= 0"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyBoth3() {}

    @Test
    @IR(applyIfPlatformAnd = {"linux", "true", "mac", "true"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyBothOs() {}

    @Test
    @IR(applyIfPlatformAnd = {"32-bit", "true", "64-bit", "true"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyBothDataModel() {}

    @Test
    @IR(applyIfPlatformAnd = {"x64", "true", "aarch64", "true"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyBothArch() {}

    @Test
    @IR(applyIfPlatform = {"aarch64", "true"},
        applyIfAnd = {"UseSVE", "= 0", "LoopMaxUnroll", "= 0"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyPlatformSVE() {}

    @Test
    @IR(applyIfPlatform = {"x64", "true"},
        applyIfAnd = {"UseAVX", "= 2", "LoopMaxUnroll", "= 0"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyPlatformAVX() {}

    @Test
    @IR(applyIfPlatformAnd = {"x64", "true", "linux", "true"},
        applyIfAnd = {"UseAVX", "= 2", "LoopMaxUnroll", "= 0"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyPlatformAVXAnd() {}

    @Test
    @IR(applyIfPlatformOr = {"x64", "true", "x86", "true"},
        applyIfAnd = {"UseSSE", "= 4", "LoopMaxUnroll", "= 0"},
        counts = {IRNode.LOOP, ">= 1000"})
    public static void testApplyPlatformSSEOr() {}

}
