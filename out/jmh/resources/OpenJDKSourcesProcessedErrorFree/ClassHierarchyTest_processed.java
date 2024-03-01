/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test of diagnostic command VM.class_hierarchy
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng ClassHierarchyTest
 */

import org.testng.annotations.Test;
import org.testng.Assert;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassHierarchyTest {





    static Pattern expected_lambda_line =
        Pattern.compile("\\|--DcmdTestClass\\$\\$Lambda.*");

    static Pattern expected_lines[] = {
        Pattern.compile("java.lang.Object/null"),
        Pattern.compile("\\|--DcmdBaseClass/0x(\\p{XDigit}*)"),
        Pattern.compile("\\|  implements Intf2/0x(\\p{XDigit}*) \\(declared intf\\)"),
        Pattern.compile("\\|  implements Intf1/0x(\\p{XDigit}*) \\(inherited intf\\)"),
        Pattern.compile("\\|  \\|--DcmdTestClass/0x(\\p{XDigit}*)"),
        Pattern.compile("\\|  \\|  implements Intf1/0x(\\p{XDigit}*) \\(inherited intf\\)"),
        Pattern.compile("\\|  \\|  implements Intf2/0x(\\p{XDigit}*) \\(inherited intf\\)")
    };

    public void run(CommandExecutor executor) throws ClassNotFoundException {
        OutputAnalyzer output;
        Iterator<String> lines;
        int i;

        Class<?> c = Class.forName("DcmdTestClass");

        output = executor.execute("VM.class_hierarchy");
        lines = output.asLines().iterator();
        Boolean foundMatch = false;
        while (lines.hasNext()) {
            String line = lines.next();
            Matcher m = expected_lambda_line.matcher(line);
            if (m.matches()) {
                foundMatch = true;
                break;
            }
        }
        if (!foundMatch) {
            Assert.fail("Failed to find lamda class");
        }

        output = executor.execute("VM.class_hierarchy DcmdBaseClass");
        lines = output.asLines().iterator();
        i = 0;
        while (lines.hasNext()) {
            String line = lines.next();
            Matcher m = expected_lines[i].matcher(line);
            i++;
            if (!m.matches()) {
                Assert.fail("Failed to match line #" + i + ": " + line);
            }
            if (i == 2) break;
        }
        if (lines.hasNext()) {
            String line = lines.next();
            Assert.fail("Unexpected dcmd output: " + line);
        }

        output = executor.execute("VM.class_hierarchy DcmdBaseClass -s");
        lines = output.asLines().iterator();
        i = 0;
        while (lines.hasNext()) {
            String line = lines.next();
            Matcher m = expected_lines[i].matcher(line);
            i++;
            if (!m.matches()) {
                Assert.fail("Failed to match line #" + i + ": " + line);
            }
            if (i == 2 || i == 4) i += 2;
        }
        if (lines.hasNext()) {
            String line = lines.next();
            Assert.fail("Unexpected dcmd output: " + line);
        }

        output = executor.execute("VM.class_hierarchy DcmdBaseClass -i -s");
        lines = output.asLines().iterator();
        i = 0;
        String classLoaderAddr = null;
        while (lines.hasNext()) {
            String line = lines.next();
            Matcher m = expected_lines[i].matcher(line);
            i++;
            if (!m.matches()) {
                Assert.fail("Failed to match line #" + i + ": " + line);
            }
            if (i == 2) {
                classLoaderAddr = m.group(1);
                System.out.println(classLoaderAddr);
            } else if (i > 2) {
                if (!classLoaderAddr.equals(m.group(1))) {
                    Assert.fail("Classloader address didn't match on line #"
                                        + i + ": " + line);
                }
            }
            if (i == expected_lines.length) break;
        }
        if (lines.hasNext()) {
            String line = lines.next();
            Assert.fail("Unexpected dcmd output: " + line);
        }
    }

    @Test
    public void jmx() throws ClassNotFoundException {
        run(new JMXExecutor());
    }
}

interface Intf1 {
}

interface Intf2 extends Intf1 {
}

class DcmdBaseClass implements Intf2 {
}

class DcmdTestClass extends DcmdBaseClass {
    static {
        Runnable r = () -> System.out.println("Hello");
        r.run();
    }
}
