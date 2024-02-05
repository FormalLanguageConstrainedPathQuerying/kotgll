/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8286069
 * @summary keytool prints out wrong key algorithm for -importpass command
 * @library /test/lib
 * @modules java.base/sun.security.util
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.security.DerUtils;
import sun.security.util.KnownOIDs;

import java.nio.file.Files;
import java.nio.file.Path;

public class ImportPassKeyAlg {
    public static void main(String[] args) throws Exception {
        importpass("def", null, KnownOIDs.PBES2,
                KnownOIDs.HmacSHA256, KnownOIDs.AES_256$CBC$NoPadding);
        importpass("pbe", "PBE", KnownOIDs.PBES2,
                KnownOIDs.HmacSHA256, KnownOIDs.AES_256$CBC$NoPadding);
        importpass("pbes2", "PBEWithHmacSHA1AndAES_128",
                KnownOIDs.PBES2, KnownOIDs.HmacSHA1, KnownOIDs.AES_128$CBC$NoPadding);
        importpass("des", "PBEwithMD5andDES", KnownOIDs.PBEWithMD5AndDES);
        importpass("3des", "PBEWithSHA1AndDESede", KnownOIDs.PBEWithSHA1AndDESede);
    }

    /**
     * Run `keytool -importpass`.
     *
     * @param name keystore name
     * @param algorithm -keyalg option value, null if not provided
     * @param oids expected OIDs inside keystore, if PBES2, plus prf and enc OIDs
     * @throws Exception
     */
    static void importpass(String name, String algorithm, KnownOIDs... oids) throws Exception {

        Files.deleteIfExists(Path.of(name));

        var cmd = "-keystore " + name + " -storepass changeit -importpass -v -alias a";
        if (algorithm != null) {
            cmd += " -keyalg " + algorithm;
        }

        SecurityTools.setResponse("changeit\nchangeit\n");
        SecurityTools.keytool(cmd)
                .shouldHaveExitValue(0)
                .shouldContain("Generated PBE secret key");

        var data = Files.readAllBytes(Path.of(name));
        DerUtils.checkAlg(data, "110c010c01010c00", oids[0]);
        if (oids[0] == KnownOIDs.PBES2) {
            DerUtils.checkAlg(data, "110c010c01010c010130", oids[1]);
            DerUtils.checkAlg(data, "110c010c01010c0110", oids[2]);
        }
    }
}
