/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @bug 8327110 8327111
 * @requires vm.compiler2.enabled
 * @summary Test that DFS algorithm for cloning Template Assertion Predicate Expression does not endlessly process paths.
 * @run main/othervm/timeout=30 -Xcomp -XX:LoopMaxUnroll=0
 *                              -XX:CompileCommand=compileonly,*TestCloningWithManyDiamondsInExpression::test*
 *                              -XX:CompileCommand=inline,*TestCloningWithManyDiamondsInExpression::create*
 *                              compiler.predicates.TestCloningWithManyDiamondsInExpression
 * @run main/othervm/timeout=30 -Xbatch -XX:LoopMaxUnroll=0
 *                              -XX:CompileCommand=compileonly,*TestCloningWithManyDiamondsInExpression::test*
 *                              -XX:CompileCommand=inline,*TestCloningWithManyDiamondsInExpression::create*
 *                              compiler.predicates.TestCloningWithManyDiamondsInExpression
 * @run main/timeout=30 compiler.predicates.TestCloningWithManyDiamondsInExpression
 */

 /*
  * @test
  * @bug 8327111
  * @summary Test that DFS algorithm for cloning Template Assertion Predicate Expression does not endlessly process paths.
  * @run main/othervm/timeout=30 -Xcomp
  *                              -XX:CompileCommand=compileonly,*TestCloningWithManyDiamondsInExpression::test*
  *                              -XX:CompileCommand=inline,*TestCloningWithManyDiamondsInExpression::create*
  *                              compiler.predicates.TestCloningWithManyDiamondsInExpression
  * @run main/othervm/timeout=30 -Xbatch
  *                              -XX:CompileCommand=compileonly,*TestCloningWithManyDiamondsInExpression::test*
  *                              -XX:CompileCommand=inline,*TestCloningWithManyDiamondsInExpression::create*
  *                              compiler.predicates.TestCloningWithManyDiamondsInExpression
  */

package compiler.predicates;

public class TestCloningWithManyDiamondsInExpression {
    static int limit = 100;
    static int iFld;
    static boolean flag;
    static int[] iArr;

    public static void main(String[] strArr) {
        Math.min(10, 13); 
        for (int i = 0; i < 10_000; i++) {
            testSplitIf(i % 2);
            testLoopUnswitching(i % 2);
            testLoopUnrolling(i % 2);
            testLoopPeeling(i % 2);
        }
    }

    static void testLoopUnswitching(int x) {
        int[] a = new int[createExpressionWithManyDiamonds(x) + 1000];
        for (int i = 0; i < limit; i++) {
            a[i] = i; 
            if (x == 0) {
                iFld = 34;
            }
        }
    }

    static void testSplitIf(int x) {
        int e = createExpressionWithManyDiamonds(x);
        iArr = new int[1000];
        int a;
        if (flag) {
            a = 4;
        } else {
            a = 3;
        }

        for (int i = a; i < 100; i++) {
            iArr[i+e] = 34;
        }
    }

    static void testLoopUnrolling(int x) {
        int[] a = new int[createExpressionWithManyDiamonds(x) + 1000];
        for (int i = 0; i < limit; i++) {
            a[i] = i; 
        }
    }

    static void testLoopPeeling(int x) {
        int[] a = new int[createExpressionWithManyDiamonds(x) + 1000];
        for (int i = 0; i < limit; i++) {
            a[i] = i; 
            if (x == 0) { 
                return;
            }
        }
    }

    static int createExpressionWithManyDiamonds(int x) {
        int e = Math.min(10, Math.max(1, x));
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2) - 823542;
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2) - 823542;
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2);
        e = e + (e << 1) + (e << 2) - 823542;
        return e;
    }
}
