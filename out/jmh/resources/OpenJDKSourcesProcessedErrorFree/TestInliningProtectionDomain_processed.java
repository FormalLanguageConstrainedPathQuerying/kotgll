/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8275868
 * @library / /test/lib
 * @summary Testing that ciReplay inlining does not fail with unresolved signature classes.
 * @requires vm.flightRecorder != true & vm.compMode != "Xint" & vm.compMode != "Xcomp" & vm.debug == true & vm.compiler2.enabled
 * @modules java.base/jdk.internal.misc
 * @run driver compiler.ciReplay.TestInliningProtectionDomain
 */

package compiler.ciReplay;

import jdk.test.lib.Asserts;

import java.util.List;

public class TestInliningProtectionDomain extends InliningBase {

    public static void main(String[] args) {
        new TestInliningProtectionDomain(ProtectionDomainTestCompiledBefore.class, true);
        new TestInliningProtectionDomain(ProtectionDomainTestNoOtherCompilationPublic.class, false);
        new TestInliningProtectionDomain(ProtectionDomainTestNoOtherCompilationPrivate.class, false);
        new TestInliningProtectionDomain(ProtectionDomainTestNoOtherCompilationPrivateString.class, false);
    }

    public TestInliningProtectionDomain(Class<?> testClass, boolean compileBar) {
        super(testClass);
        if (compileBar) {
            commandLineNormal.add("-XX:CompileCommand=compileonly," + testClass.getName() + "::bar");
        }
        runTest();
    }

    @Override
    public void testAction() {
        positiveTest(commandLineReplay);
        String entryString = getTestClass() + " " + "test";
        boolean inlineFails = testClass == ProtectionDomainTestNoOtherCompilationPrivate.class;
        int inlineeCount = inlineFails ? 1 : 5;

        List<InlineEntry> inlineesNormal = parseLogFile(LOG_FILE_NORMAL, entryString, "compile_id='" + getCompileIdFromFile(getReplayFileName()), inlineeCount);
        List<InlineEntry> inlineesReplay = parseLogFile(LOG_FILE_REPLAY, entryString, "test ()V", inlineeCount);
        verifyLists(inlineesNormal, inlineesReplay, inlineeCount);

        if (inlineFails) {
            Asserts.assertTrue(inlineesNormal.get(0).compare("compiler.ciReplay.ProtectionDomainTestNoOtherCompilationPrivate", "bar", inlineesNormal.get(0).isUnloadedSignatureClasses()));
            Asserts.assertTrue(inlineesReplay.get(0).compare("compiler.ciReplay.ProtectionDomainTestNoOtherCompilationPrivate", "bar", inlineesReplay.get(0).isDisallowedByReplay()));
        } else {
            Asserts.assertTrue(inlineesNormal.get(4).compare("compiler.ciReplay.InliningBar", "bar2", inlineesNormal.get(4).isNormalInline()));
            Asserts.assertTrue(inlineesReplay.get(4).compare("compiler.ciReplay.InliningBar", "bar2", inlineesReplay.get(4).isForcedByReplay() || inlineesReplay.get(4).isForcedIncrementalInlineByReplay()));
        }
    }
}

class ProtectionDomainTestCompiledBefore {
    public static void main(String[] args) {
        for (int i = 0; i < 10000; i++) {
            bar(); 
        }
        for (int i = 0; i < 10000; i++) {
            test();
        }
    }

    public static void test() {
        bar();
    }

    private static Integer bar() {
        InliningFoo.foo();
        return null;
    }
}

class ProtectionDomainTestNoOtherCompilationPublic {
    public static void main(String[] args) {
        for (int i = 0; i < 10000; i++) {
            test();
        }
    }

    public static void test() {
        bar(); 
    }

    public static Integer bar() {
        InliningFoo.foo();
        return null;
    }
}

class ProtectionDomainTestNoOtherCompilationPrivate {
    public static void main(String[] args) {
        for (int i = 0; i < 10000; i++) {
            test();
        }
    }

    public static void test() {
        bar(); 
    }

    private static Integer bar() {
        InliningFoo.foo();
        return null;
    }
}

class ProtectionDomainTestNoOtherCompilationPrivateString {
    public static void main(String[] args) {
        for (int i = 0; i < 10000; i++) {
            test();
        }
    }

    public static void test() {
        bar(); 
    }

    private static String bar() {
        InliningFoo.foo();
        return null;
    }
}

class InliningFoo {
    public static void foo() {
        foo2();
    }

    private static void foo2() {
        InliningBar.bar();
    }
}


class InliningBar {
    public static void bar() {
        bar2();
    }

    private static void bar2() {}
}
