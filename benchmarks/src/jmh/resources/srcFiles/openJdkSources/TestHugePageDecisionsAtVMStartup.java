/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2024, Red Hat Inc.
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
 * @test id=Default
 * @summary Test JVM large page setup (default options)
 * @library /test/lib
 * @requires os.family == "linux"
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TestHugePageDecisionsAtVMStartup
 */

/*
 * @test id=LP_enabled
 * @summary Test JVM large page setup (+LP)
 * @library /test/lib
 * @requires os.family == "linux"
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TestHugePageDecisionsAtVMStartup -XX:+UseLargePages
 */

/*
 * @test id=THP_enabled
 * @summary Test JVM large page setup (+THP)
 * @library /test/lib
 * @requires os.family == "linux"
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TestHugePageDecisionsAtVMStartup -XX:+UseTransparentHugePages
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TestHugePageDecisionsAtVMStartup {

    static final String warningNoTHP = "[warning][pagesize] UseTransparentHugePages disabled, transparent huge pages are not supported by the operating system.";
    static final String warningNoLP = "[warning][pagesize] UseLargePages disabled, no large pages configured and available on the system.";

    static final String buildSizeString(long l) {
        String units[] = { "K", "M", "G" };
        long factor = 1024 * 1024 * 1024;
        for (int i = 2; i >= 0; i--) {
            if (l >= factor) {
                return Long.toString(l / factor) + units[i];
            }
            factor /= 1024;
        }
        return Long.toString(l) + "B";
    }

    static void testOutput(boolean useLP, boolean useTHP, OutputAnalyzer out, HugePageConfiguration configuration) {

        out.shouldHaveExitValue(0);


        boolean haveUsableExplicitHugePages = false;
        if (configuration.supportsExplicitHugePages()) {
            long defaultLargePageSize = configuration.getExplicitDefaultHugePageSize();
            Set<HugePageConfiguration.ExplicitHugePageConfig> configs = configuration.getExplicitHugePageConfigurations();
            for (HugePageConfiguration.ExplicitHugePageConfig config: configs) {
                if (config.pageSize <= defaultLargePageSize) {
                    if (config.nr_hugepages > 0 || config.nr_overcommit_hugepages > 0) {
                        haveUsableExplicitHugePages = true; break;
                    }
                }
            }
        }

        if (useTHP && !useLP) {
            useLP = true; 
        }

        if (!useLP) {
            out.shouldContain("[info][pagesize] Large page support disabled");
        } else if (useLP && !useTHP &&
                 (!configuration.supportsExplicitHugePages() || !haveUsableExplicitHugePages)) {
            out.shouldContain(warningNoLP);
        } else if (useLP && useTHP && !configuration.supportsTHP()) {
            out.shouldContain(warningNoTHP);
        } else if (useLP && !useTHP &&
                 configuration.supportsExplicitHugePages() && haveUsableExplicitHugePages) {
            out.shouldContain("[info][pagesize] Using the default large page size: " + buildSizeString(configuration.getExplicitDefaultHugePageSize()));
            out.shouldContain("[info][pagesize] UseLargePages=1, UseTransparentHugePages=0");
            out.shouldContain("[info][pagesize] Large page support enabled");
        } else if (useLP && useTHP && configuration.supportsTHP()) {
            long thpPageSize = configuration.getThpPageSizeOrFallback();
            String thpPageSizeString = buildSizeString(thpPageSize);
            out.shouldContain("[info][pagesize] UseLargePages=1, UseTransparentHugePages=1");
            out.shouldMatch(".*\\[info]\\[pagesize] Large page support enabled. Usable page sizes: \\d+[kK], " + thpPageSizeString + ". Default large page size: " + thpPageSizeString + ".*");
        }
    }

    public static void main(String[] extraOptions) throws Exception {
        List<String> allOptions = new ArrayList<String>();
        if (extraOptions != null) {
            allOptions.addAll(Arrays.asList(extraOptions));
        }
        allOptions.add("-Xmx128m");
        allOptions.add("-Xlog:pagesize");
        allOptions.add("-version");

        boolean useLP = allOptions.contains("-XX:+UseLargePages");
        boolean useTHP = allOptions.contains("-XX:+UseTransparentHugePages");
        System.out.println("useLP: " + useLP + " useTHP: " + useTHP);

        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder(allOptions.toArray(new String[0]));
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.reportDiagnosticSummary();
        HugePageConfiguration configuration = HugePageConfiguration.readFromOS();
        System.out.println("configuration read from OS:" + configuration);

        testOutput(useLP, useTHP, output, configuration);
    }
}
