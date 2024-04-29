/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jdk.test.lib.util.JarUtils;
import jdk.test.lib.SecurityTools;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8217375
 * @library /test/lib
 * @run testng WasSignedByOtherSigner
 * @summary Checks that {@code wasSigned} in
 * {@link jdk.security.jarsigner.JarSigner#sign0} is set true if the jar to sign
 * contains a signature that will not be overwritten with the current one.
 */
public class WasSignedByOtherSigner {

    static final String KEYSTORE_FILENAME = "test.jks";

    @BeforeClass
    public void prepareKeyStore() throws Exception {
        SecurityTools.keytool("-genkeypair -keyalg EC -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias a -dname CN=A").shouldHaveExitValue(0);
    }

    void test(String secondSigner, boolean expRrewritten) throws Exception {
        String jarFilename1 = "test" + secondSigner + "-1.jar";
        JarUtils.createJarFile(Path.of(jarFilename1), (Manifest) null,
                Path.of("."));
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " + jarFilename1 + " a")
                .shouldHaveExitValue(0);
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename1), "initial manifest");

        String jarFilename2 = "test" + secondSigner + "-2.jar";
        JarUtils.updateJar(jarFilename1, jarFilename2, Map.of(
                "META-INF/.SF", Name.SIGNATURE_VERSION + ": 1.0\r\n"));
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename2), "with fake META-INF.SF file");
        String jarFilename3 = "test" + secondSigner + "-3.jar";
        JarUtils.updateManifest(jarFilename2, jarFilename3, new Manifest() {
            @Override public void write(OutputStream out) throws IOException {
                out.write((Name.MANIFEST_VERSION + ": 1.0\r\n").getBytes(UTF_8));
            }
        });
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename3), "with manifest manipulated");
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " + jarFilename3 + " a")
                .shouldHaveExitValue(0);
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename3), "signed");
        String jarFilename4 = "test" + secondSigner + "-4.jar";
        JarUtils.updateJar(jarFilename3, jarFilename4,
                Map.of("META-INF/.SF", false));
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename4), "with fake META-INF.SF file removed");

        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug -sigfile " +
                secondSigner + " " + jarFilename4 + " a")
                .shouldHaveExitValue(0);
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename4), "signed again");

        SecurityTools.jarsigner("-verify -keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -debug -verbose " + jarFilename4)
                .shouldHaveExitValue(0);
        SecurityTools.jarsigner("-verify -keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -debug -verbose " + jarFilename4 +
                " a").shouldHaveExitValue(0);

        try (ZipFile jar = new ZipFile(jarFilename4)) {
            ZipEntry ze = jar.getEntry(JarFile.MANIFEST_NAME);
            byte[] manifestBytes = jar.getInputStream(ze).readAllBytes();
            Utils.echoManifest(manifestBytes, "manifest");
            String manifestString = new String(manifestBytes, UTF_8);
            boolean actRewritten = manifestString.endsWith("\r\n\r\n");
            assertEquals(actRewritten, expRrewritten);
        }
    }

    @Test
    public void reSignSameSigner() throws Exception {
        test("A", true);
    }

    @Test
    public void reSignOtherSigner() throws Exception {
        test("B", false);
    }

}
