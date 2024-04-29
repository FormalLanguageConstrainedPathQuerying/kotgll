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
 */

/*
 * @test
 * @bug 8051959
 * @summary Option to print extra information in java.security.debug output
 * @library /test/lib
 * @run junit DebugOptions
 */

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.KeyStore;
import java.security.Security;
import java.util.stream.Stream;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class DebugOptions {

    static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";

    private static Stream<Arguments> patternMatches() {
        return Stream.of(
                Arguments.of("properties",
                        "properties: Initial",
                        "properties\\["),
                Arguments.of("properties+thread",
                        "properties\\[.*\\|main\\|.*java.*]:",
                        "properties\\[" + DATE_REGEX),
                Arguments.of("properties+timestamp",
                        "properties\\[" + DATE_REGEX + ".*\\]",
                        "\\|main\\]:"),
                Arguments.of("properties+timestamp+thread",
                        "properties\\[.*\\|main|" + DATE_REGEX + ".*\\]:",
                        "properties:"),
                Arguments.of("properties+thread+timestamp",
                        "properties\\[.*\\|main|" + DATE_REGEX + ".*\\]:",
                        "properties:"),
                Arguments.of("properties,thread,timestamp",
                        "properties:",
                        "properties\\[.*\\|main|" + DATE_REGEX + ".*\\]:"),
                Arguments.of("properties+thread+timestamp,keystore",
                        "properties\\[.*\\|main|" + DATE_REGEX + ".*\\]:",
                        "keystore\\["),
                Arguments.of("keystore,properties+thread+timestamp",
                        "properties\\[.*\\|main|" + DATE_REGEX + ".*\\]:",
                        "keystore\\["),
                Arguments.of("keystore+thread,properties+thread",
                        "properties\\[.*\\|main|.*\\Rkeystore\\[.*\\|main|.*\\]:",
                        "\\|" + DATE_REGEX + ".*\\]:"),
                Arguments.of("keystore+thread,properties+thread,",
                        "properties\\[.*\\|main|.*\\Rkeystore\\[.*\\|main|.*\\]:",
                        "\\|" + DATE_REGEX + ".*\\]:"),
                Arguments.of("keystore+timestamp,properties+thread",
                        "properties\\[.*\\|main|.*\\Rkeystore\\[" + DATE_REGEX + ".*\\]:",
                        "properties\\[.*\\|" + DATE_REGEX + ".*\\]:"),
                Arguments.of("all+thread",
                        "properties\\[.*\\|main.*((.*\\R)*)keystore\\[.*\\|main.*java.*\\]:",
                        "properties\\[" + DATE_REGEX + ".*\\]:"),
                Arguments.of("all+thread+timestamp",
                        "properties\\[.*\\|main.*\\|" + DATE_REGEX +
                                ".*\\]((.*\\R)*)keystore\\[.*\\|main.*\\|" + DATE_REGEX + ".*\\]:",
                        "properties:"),
                Arguments.of("all+thread+timestamp,properties",
                        "properties\\[.*\\|main.*\\|" + DATE_REGEX +
                                ".*\\]((.*\\R)*)keystore\\[.*\\|main.*\\|" + DATE_REGEX + ".*\\]:",
                        "properties:"),
                Arguments.of("properties+thread,all",
                        "properties\\[.*\\|main\\|.*\\]:",
                        "keystore\\[.*\\|main\\|.*\\]:"),
                Arguments.of("properties,all+thread",
                        "properties\\[.*\\|main.*java" +
                                ".*\\]((.*\\R)*)keystore\\[.*\\|main.*java.*\\]:",
                        "properties:")
        );
    }

    @ParameterizedTest
    @MethodSource("patternMatches")
    public void shouldContain(String params, String expected, String notExpected) throws Exception {
        OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJava(
                "-Djava.security.debug=" + params,
                "DebugOptions"
        );
        outputAnalyzer.shouldHaveExitValue(0)
                .shouldMatch(expected)
                .shouldNotMatch(notExpected);
    }

    public static void main(String[] args) throws Exception {
        Security.getProperty("test");
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
    }
}
