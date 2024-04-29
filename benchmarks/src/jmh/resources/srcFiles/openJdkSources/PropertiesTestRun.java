/*
 * Copyright (c) 2007, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6332666 6863624 7180362 8003846 8074350 8074351 8130246 8149735 7102969
 *      8157138 8190904 8210410
 * @summary Tests the capability of replacing the currency data with a user
 *          specified currency properties file in lib directory (old way) or
 *          via the system property in the cmdline (new way).
 * @library /test/lib
 * @build PropertiesTest
 * @run junit PropertiesTestRun
 */

import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class PropertiesTestRun {

    private static final String TEST_JDK = Utils.TEST_JDK;
    private static final String TEST_PROPS =
            Utils.TEST_SRC+Utils.FILE_SEPARATOR+"currency.properties";
    private static final String WRITABLE_JDK =
            "."+Utils.FILE_SEPARATOR+"WRITABLE_JDK";
    private static final String WRITABLE_JDK_LIB =
            WRITABLE_JDK+Utils.FILE_SEPARATOR+"lib";
    private static final String WRITABLE_JDK_BIN =
            WRITABLE_JDK+Utils.FILE_SEPARATOR+"bin";
    private static final String WRITABLE_JDK_JAVA_PATH =
            WRITABLE_JDK_BIN + Utils.FILE_SEPARATOR + "java";

    @BeforeAll
    static void setUp() throws Throwable {
        createWritableJDK();
        executeTestJDKMethod("PropertiesTest", "-d", "dump1");
        executeTestJDKMethod("-Djava.util.currency.data="+TEST_PROPS,
                "PropertiesTest", "-d", "dump2");
        executeWritableJDKMethod("PropertiesTest", "-d", "dump3");
    }

    private static void createWritableJDK() throws Throwable {
        executeProcess(new String[]{"cp", "-H", "-R", TEST_JDK, WRITABLE_JDK});
        executeProcess(new String[]{"chmod", "-R", "u+w", WRITABLE_JDK_LIB});
        executeProcess(new String[]{"cp", TEST_PROPS, WRITABLE_JDK_LIB});
    }

    @Test
    void compareDumps() throws Throwable {
        executeTestJDKMethod("PropertiesTest", "-c", "dump1", "dump2",
                TEST_PROPS);
        executeTestJDKMethod("PropertiesTest", "-c", "dump1", "dump3",
                TEST_PROPS);
    }

    @ParameterizedTest
    @MethodSource("PropertiesTestMethods")
    void launchPropertiesTests(String methodName) throws Throwable {
        executeWritableJDKMethod("PropertiesTest", methodName);
        executeTestJDKMethod("-Djava.util.currency.data="+TEST_PROPS,
                "PropertiesTest", methodName);
    }

    private static Stream<String> PropertiesTestMethods() {
        return Stream.of("bug7102969", "bug8157138", "bug8190904");
    }

    private static void executeTestJDKMethod(String... params) throws Throwable {
        int exitStatus = ProcessTools.executeTestJava(params).getExitValue();
        if (exitStatus != 0) {
            fail("Process started with: " + Arrays.toString(params) + " failed");
        }
    }

    private static void executeWritableJDKMethod(String... params) throws Throwable {
        String[] allParams = new String[3+params.length+Utils.getTestJavaOpts().length];
        allParams[0] = WRITABLE_JDK_JAVA_PATH;
        allParams[1] = "-cp";
        allParams[2] = System.getProperty("java.class.path");
        System.arraycopy(Utils.getTestJavaOpts(), 0, allParams, 3,
                Utils.getTestJavaOpts().length);
        System.arraycopy(params, 0, allParams, Utils.getTestJavaOpts().length+3,
                params.length);
        executeProcess(allParams);
    }

    private static void executeProcess(String[] params) throws Throwable {
        System.out.println("Command line: " + Arrays.toString(params));
        int exitStatus = ProcessTools.executeProcess(params).getExitValue();
        if (exitStatus != 0) {
            fail("Process started with: " + Arrays.toString(params) + " failed");
        }
    }

    @AfterAll
    static void tearDown() throws Throwable {
        executeProcess(new String[]{"rm", "-rf", WRITABLE_JDK});
    }
}
