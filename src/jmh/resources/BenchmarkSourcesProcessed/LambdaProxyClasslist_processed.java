/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test some error handling on the "@lambda-proxy" entries in a classlist.
 * @requires vm.cds
 * @library /test/lib
 * @compile dynamicArchive/test-classes/LambHello.java
 * @run driver LambdaProxyClasslist
 */

import jdk.test.lib.process.OutputAnalyzer;

public class LambdaProxyClasslist {

  public static void main(String[] args) throws Exception {
    JarBuilder.build("lambhello", "LambHello");

    String appJar = TestCommon.getTestJar("lambhello.jar");

    OutputAnalyzer out = TestCommon.dump(appJar,
        TestCommon.list("LambHello",
                        "@lambda-proxy LambHello run ()Ljava/lang/Runnable; ()V REF_invokeStatic LambHello lambda$doTest$0 ()V ()V"));
    out.shouldHaveExitValue(0);

    out = TestCommon.dump(appJar,
        TestCommon.list("LambHello",
                        "@lambda-proxy LambHello"));
    out.shouldContain("An error has occurred while processing class list file")
       .shouldContain("Line with @ tag has too few items \"@lambda-proxy\" line #2")
       .shouldContain("class list format error")
       .shouldHaveExitValue(1);

    out = TestCommon.dump(appJar,
        TestCommon.list("LambHello",
                        "@lambda-proxy LambHello run ()Ljava/lang/Runnable; ()V REF_invokeStatic LambHello lambda$doTest$0 ()V ()Z"));
    out.shouldContain("[warning][cds] No invoke dynamic constant pool entry can be found for class LambHello. The classlist is probably out-of-date.")
       .shouldHaveExitValue(0);

    out = TestCommon.dump(appJar,
        TestCommon.list("LambHello",
                        "@lambda-proxy  LambHello run  ()Ljava/lang/Runnable; ()V REF_invokeStatic LambHello lambda$doTest$0 ()V ()V"));
    out.shouldHaveExitValue(0);

    out = TestCommon.dump(appJar,
        TestCommon.list("LambHello",
                        "@lambda-proxy LambHello run ()Ljava/lang/Runnable; ()V REF_invokeStatic LambHello lambda$doTest$0 ()V ()V "));
    out.shouldHaveExitValue(0);

    out = TestCommon.dump(appJar,
        TestCommon.list("LambHello",
                        "@lambda-proxy: LambHello run ()Ljava/lang/Runnable; ()V REF_invokeStatic LambHello lambda$doTest$0 ()V ()V"));
    out.shouldContain("Invalid @ tag at the beginning of line \"@lambda-proxy:\" line #2")
       .shouldHaveExitValue(1);
  }
}
