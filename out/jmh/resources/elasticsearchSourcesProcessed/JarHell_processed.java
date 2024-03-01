/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.jdk;

import org.elasticsearch.core.PathUtils;
import org.elasticsearch.core.SuppressForbidden;

import java.io.IOException;
import java.lang.Runtime.Version;
import java.lang.module.Configuration;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableSet;

/**
 * Simple check for duplicate class files across the classpath.
 * <p>
 * This class checks for incompatibilities in the following ways:
 * <ul>
 *   <li>Checks that class files are not duplicated across jars.</li>
 *   <li>Checks any {@code X-Compile-Target-JDK} value in the jar
 *       manifest is compatible with current JRE</li>
 *   <li>Checks any {@code X-Compile-Elasticsearch-Version} value in
 *       the jar manifest is compatible with the current ES</li>
 * </ul>
 */
public class JarHell {

    /** no instantiation */
    private JarHell() {}

    /** Simple driver class, can be used eg. from builds. Returns non-zero on jar-hell */
    @SuppressForbidden(reason = "command line tool")
    public static void main(String args[]) throws Exception {
        System.out.println("checking for jar hell...");
        checkJarHell(System.out::println);
        System.out.println("no jar hell found");
    }

    /**
     * Checks the current classpath for duplicate classes
     * @param output A {@link String} {@link Consumer} to which debug output will be sent
     * @throws IllegalStateException if jar hell was found
     */
    public static void checkJarHell(Consumer<String> output) throws IOException {
        ClassLoader loader = JarHell.class.getClassLoader();
        output.accept("java.class.path: " + System.getProperty("java.class.path"));
        output.accept("sun.boot.class.path: " + System.getProperty("sun.boot.class.path"));
        if (loader instanceof URLClassLoader urlClassLoader) {
            output.accept("classloader urls: " + Arrays.toString(urlClassLoader.getURLs()));
        }
        checkJarHell(parseClassPath(), output);
    }

    /**
     * Parses the classpath into an array of URLs
     * @return array of URLs
     * @throws IllegalStateException if the classpath contains empty elements
     */
    public static Set<URL> parseClassPath() {
        return parseClassPath(System.getProperty("java.class.path"));
    }

    /**
     * Parses the classpath into a set of URLs. For testing.
     * @param classPath classpath to parse (typically the system property {@code java.class.path})
     * @return array of URLs
     * @throws IllegalStateException if the classpath contains empty elements
     */
    @SuppressForbidden(reason = "resolves against CWD because that is how classpaths work")
    static Set<URL> parseClassPath(String classPath) {
        if (classPath.isEmpty()) {
            return Set.of();
        }
        String pathSeparator = System.getProperty("path.separator");
        String fileSeparator = System.getProperty("file.separator");
        String elements[] = classPath.split(pathSeparator);
        Set<URL> urlElements = new LinkedHashSet<>(); 
        for (String element : elements) {
            /*
             * Technically empty classpath element behaves like CWD.
             * So below is the "correct" code, however in practice with ES, this is usually just a misconfiguration,
             * from old shell scripts left behind or something:
             *
             *   if (element.isEmpty()) {
             *      element = System.getProperty("user.dir");
             *   }
             *
             * Instead we just throw an exception, and keep it clean.
             */
            if (element.isEmpty()) {
                throw new IllegalStateException(
                    "Classpath should not contain empty elements! (outdated shell script from a previous"
                        + " version?) classpath='"
                        + classPath
                        + "'"
                );
            }
            if (element.startsWith("/") && "\\".equals(fileSeparator)) {
                element = element.replace("/", "\\");
                if (element.length() >= 3 && element.charAt(2) == ':') {
                    element = element.substring(1);
                }
            }
            try {
                if (element.equals("/")) {
                    continue;
                }
                URL url = PathUtils.get(element).toUri().toURL();
                if (urlElements.add(url) == false && element.endsWith(".jar")) {
                    throw new IllegalStateException(
                        "jar hell!" + System.lineSeparator() + "duplicate jar [" + element + "] on classpath: " + classPath
                    );
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return Collections.unmodifiableSet(urlElements);
    }

    /**
     * Returns a set of URLs that contain artifacts from both the non-JDK boot
     * modules and class path. These URLs constitute the loadable application
     * artifacts in the system class loader.
     */
    public static Set<URL> parseModulesAndClassPath() {
        return Stream.concat(parseClassPath().stream(), JarHell.nonJDKBootModuleURLs()).collect(toUnmodifiableSet());
    }

    /**
     * Returns a stream containing the URLs of all non-JDK modules in the boot layer.
     * The stream may be empty, if, say, running with ES on the class path.
     */
    static Stream<URL> nonJDKBootModuleURLs() {
        return nonJDKModuleURLs(ModuleLayer.boot().configuration());
    }

    static Stream<URL> nonJDKModuleURLs(Configuration configuration) {
        return Stream.concat(Stream.of(configuration), configuration.parents().stream())
            .map(Configuration::modules)
            .flatMap(Set::stream)
            .map(ResolvedModule::reference)
            .map(ModuleReference::location)
            .flatMap(Optional::stream)
            .map(JarHell::toURL)
            .filter(url -> url.getProtocol().equals("jrt") == false);
    }

    /**
     * Checks the set of URLs for duplicate classes
     * @param urls A set of URLs from the system class loader to be checked for conflicting jars
     * @param output A {@link String} {@link Consumer} to which debug output will be sent
     * @throws IllegalStateException if jar hell was found
     */
    @SuppressForbidden(reason = "needs JarFile for speed, just reading entries")
    public static void checkJarHell(Set<URL> urls, Consumer<String> output) throws IOException {
        String javaHome = System.getProperty("java.home");
        output.accept("java.home: " + javaHome);
        final Map<String, Path> clazzes = new HashMap<>(32768);
        Set<Path> seenJars = new HashSet<>();
        for (final URL url : urls) {
            final Path path = toPath(url);
            if (path.startsWith(javaHome)) {
                output.accept("excluding system resource: " + path);
                continue;
            }
            if (path.toString().endsWith(".jar")) {
                if (seenJars.add(path) == false) {
                    throw new IllegalStateException("jar hell!" + System.lineSeparator() + "duplicate jar on classpath: " + path);
                }
                output.accept("examining jar: " + path);
                try (JarFile file = new JarFile(path.toString())) {
                    Manifest manifest = file.getManifest();
                    if (manifest != null) {
                        checkManifest(manifest, path);
                    }
                    Enumeration<JarEntry> elements = file.entries();
                    while (elements.hasMoreElements()) {
                        String entry = elements.nextElement().getName();
                        if (entry.endsWith(".class")) {
                            entry = entry.replace('/', '.').substring(0, entry.length() - 6);
                            checkClass(clazzes, entry, path);
                        }
                    }
                }
            } else {
                output.accept("examining directory: " + path);
                final Path root = toPath(url);
                final String sep = root.getFileSystem().getSeparator();

                if (Files.exists(root)) {
                    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            String entry = root.relativize(file).toString();
                            if (entry.endsWith(".class")) {
                                entry = entry.replace(sep, ".").substring(0, entry.length() - ".class".length());
                                checkClass(clazzes, entry, path);
                            }
                            return super.visitFile(file, attrs);
                        }
                    });
                }
            }
        }
    }

    /** inspect manifest for sure incompatibilities */
    private static void checkManifest(Manifest manifest, Path jar) {
        String targetVersion = manifest.getMainAttributes().getValue("X-Compile-Target-JDK");
        if (targetVersion != null) {
            checkJavaVersion(jar.toString(), targetVersion);
        }
    }

    /**
     * Checks that the java specification version {@code targetVersion}
     * required by {@code resource} is compatible with the current installation.
     */
    public static void checkJavaVersion(String resource, String targetVersion) {
        Version version = Version.parse(targetVersion);
        if (Runtime.version().feature() < version.feature()) {
            throw new IllegalStateException(
                String.format(Locale.ROOT, "%s requires Java %s:, your system: %s", resource, targetVersion, Runtime.version().toString())
            );
        }
    }

    private static void checkClass(Map<String, Path> clazzes, String clazz, Path jarpath) {
        if (clazz.equals("module-info") || clazz.endsWith(".module-info")) {
            return;
        }
        Path previous = clazzes.put(clazz, jarpath);
        if (previous != null) {
            if (previous.equals(jarpath)) {
                throw new IllegalStateException(
                    "jar hell!"
                        + System.lineSeparator()
                        + "class: "
                        + clazz
                        + System.lineSeparator()
                        + "exists multiple times in jar: "
                        + jarpath
                        + " !!!!!!!!!"
                );
            } else {
                throw new IllegalStateException(
                    "jar hell!"
                        + System.lineSeparator()
                        + "class: "
                        + clazz
                        + System.lineSeparator()
                        + "jar1: "
                        + previous
                        + System.lineSeparator()
                        + "jar2: "
                        + jarpath
                );
            }
        }
    }

    private static URL toURL(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    private static Path toPath(URL url) {
        try {
            return PathUtils.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }
}
