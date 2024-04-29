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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleDescriptor;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.spi.ToolProvider;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

/**
 * @test
 * @bug 8258117
 * @summary Tests that the content generated for module-info.class, using the jar command, is reproducible
 * @run testng JarToolModuleDescriptorReproducibilityTest
 */
public class JarToolModuleDescriptorReproducibilityTest {

    private static final String MODULE_NAME = "foo";
    private static final String MODULE_VERSION = "1.2.3";
    private static final String UPDATED_MODULE_VERSION = "1.2.4";
    private static final String MAIN_CLASS = "jdk.test.foo.Foo";
    private static final Path MODULE_CLASSES_DIR = Path.of("8258117-module-classes", MODULE_NAME).toAbsolutePath();

    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
            .orElseThrow(()
                    -> new RuntimeException("jar tool not found")
            );
    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(()
                    -> new RuntimeException("javac tool not found")
            );


    @BeforeClass
    public static void setup() throws Exception {
        compileModuleClasses();
    }

    /**
     * Launches a "jar --create" command multiple times with a module-info.class. The module-info.class
     * is internally updated by the jar tool to add additional data. Expects that each such generated
     * jar has the exact same bytes.
     */
    @Test
    public void testJarCreate() throws Exception {
        List<Path> jarFiles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Path targetJar = Files.createTempFile(Path.of("."), "8258117-jar-create", ".jar");
            jarFiles.add(targetJar);
            if (i > 0) {
                Thread.sleep(1000);
            }
            runJarCommand("--create",
                    "--file=" + targetJar,
                    "--main-class=" + MAIN_CLASS,
                    "--module-version=" + MODULE_VERSION,
                    "--no-manifest",
                    "-C", MODULE_CLASSES_DIR.toString(), ".");
            assertExpectedModuleInfo(targetJar, MODULE_VERSION);
        }
        assertAllFileContentsAreSame(jarFiles);
    }

    /**
     * Launches a "jar --update" process multiple times to update the module-info.class
     * descriptor with the same content and then expects that the modular jar created by
     * each of these processes has the exact same bytes.
     */
    @Test
    public void testJarUpdate() throws Exception {
        List<Path> jarFiles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Path targetJar = Files.createTempFile(Path.of("."), "8258117-jar-update", ".jar");
            jarFiles.add(targetJar);
            if (i > 0) {
                Thread.sleep(1000);
            }
            runJarCommand("--create",
                    "--file=" + targetJar,
                    "--module-version=" + MODULE_VERSION,
                    "--no-manifest",
                    "-C", MODULE_CLASSES_DIR.toString(), ".");
            assertExpectedModuleInfo(targetJar, MODULE_VERSION);
            runJarCommand("--update",
                    "--file=" + targetJar,
                    "--module-version=" + UPDATED_MODULE_VERSION,
                    "--no-manifest",
                    "-C", MODULE_CLASSES_DIR.toString(), "module-info.class");
            assertExpectedModuleInfo(targetJar, UPDATED_MODULE_VERSION);
        }
        assertAllFileContentsAreSame(jarFiles);
    }

    private static void compileModuleClasses() throws Exception {
        Path sourcePath = Path.of(System.getProperty("test.src", "."),
                "src", MODULE_NAME);
        List<String> sourceFiles = new ArrayList<>();
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    sourceFiles.add(file.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Path classesDir = Files.createDirectories(MODULE_CLASSES_DIR);
        List<String> javacArgs = new ArrayList<>();
        javacArgs.add("-d");
        javacArgs.add(classesDir.toString());
        sourceFiles.forEach((f) -> javacArgs.add(f));
        System.out.println("Launching javac command with args: " + javacArgs);
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            int exitCode = JAVAC_TOOL.run(pw, pw, javacArgs.toArray(new String[0]));
            assertEquals(exitCode, 0, "Module compilation failed: " + sw.toString());
        }
        System.out.println("Module classes successfully compiled to directory " + classesDir);
    }

    private static void runJarCommand(String... jarArgs) {
        StringWriter sw = new StringWriter();
        System.out.println("Launching jar command with args: " + Arrays.toString(jarArgs));
        try (PrintWriter pw = new PrintWriter(sw)) {
            int exitCode = JAR_TOOL.run(pw, pw, jarArgs);
            assertEquals(exitCode, 0, "jar command execution failed: " + sw.toString());
        }
    }

    private static void assertAllFileContentsAreSame(List<Path> files) throws Exception {
        Path firstFile = files.get(0);
        for (int i = 1; i < files.size(); i++) {
            assertEquals(Files.mismatch(firstFile, files.get(i)), -1,
                    "Content in file " + files.get(i) + " isn't the same as in file " + firstFile);
        }
    }

    private static void assertExpectedModuleInfo(Path jar, String expectedModuleVersion) throws Exception {
        try (JarInputStream jaris = new JarInputStream(Files.newInputStream(jar))) {
            JarEntry moduleInfoEntry = null;
            JarEntry entry = null;
            while ((entry = jaris.getNextJarEntry()) != null) {
                if (entry.getName().equals("module-info.class")) {
                    moduleInfoEntry = entry;
                    break;
                }
            }
            assertNotNull(moduleInfoEntry, "module-info.class is missing from jar " + jar);

            ModuleDescriptor md = ModuleDescriptor.read(jaris);
            assertEquals(md.name(), MODULE_NAME, "Unexpected module name");
            assertFalse(md.rawVersion().isEmpty(), "Module version missing from descriptor");

            String actualVersion = md.rawVersion().get();
            assertEquals(actualVersion, expectedModuleVersion, "Unexpected module version");

            System.out.println(moduleInfoEntry.getName() + " has a timestamp of "
                    + moduleInfoEntry.getTime() + " for version " + actualVersion);
        }
    }
}

