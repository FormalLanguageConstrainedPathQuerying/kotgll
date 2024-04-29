/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7148584
 * @summary Jar tools fails to generate manifest correctly when boundary condition hit
 * @modules jdk.jartool/sun.tools.jar
 * @compile -XDignore.symbol.file=true CreateManifest.java
 * @run main CreateManifest
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.*;

public class CreateManifest {

public static void main(String arg[]) throws Exception {

    String jarFileName = "test.jar";
    String ManifestName = "MANIFEST.MF";

    Files.write(Paths.get(ManifestName), FILE_CONTENTS.getBytes());

    String [] args = new String [] { "cvfm", jarFileName, ManifestName};
    sun.tools.jar.Main jartool =
            new sun.tools.jar.Main(System.out, System.err, "jar");
    jartool.run(args);

    try (JarFile jf = new JarFile(jarFileName)) {
        Manifest m = jf.getManifest();
        String result = m.getMainAttributes().getValue("Class-path");
        if (result == null)
            throw new RuntimeException("Failed to add Class-path attribute to manifest");
    } finally {
        Files.deleteIfExists(Paths.get(jarFileName));
        Files.deleteIfExists(Paths.get(ManifestName));
    }

}

private static final String FILE_CONTENTS =
 "Class-path: \n" +
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
 " /ade/dtsao_re/oracle/emcore
}
