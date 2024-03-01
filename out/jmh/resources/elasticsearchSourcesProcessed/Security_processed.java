/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.bootstrap;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.PathUtils;
import org.elasticsearch.core.SuppressForbidden;
import org.elasticsearch.env.Environment;
import org.elasticsearch.http.HttpTransportSettings;
import org.elasticsearch.jdk.JarHell;
import org.elasticsearch.plugins.PluginsUtils;
import org.elasticsearch.secure_sm.SecureSM;
import org.elasticsearch.transport.TcpTransport;

import java.io.FilePermission;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.net.SocketPermission;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.AccessMode;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.security.Permissions;
import java.security.Policy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.lang.invoke.MethodType.methodType;
import static org.elasticsearch.bootstrap.FilePermissionUtils.addDirectoryPath;
import static org.elasticsearch.bootstrap.FilePermissionUtils.addSingleFilePath;
import static org.elasticsearch.reservedstate.service.FileSettingsService.OPERATOR_DIRECTORY;
import static org.elasticsearch.reservedstate.service.FileSettingsService.SETTINGS_FILE_NAME;

/**
 * Initializes SecurityManager with necessary permissions.
 * <br>
 * <h2>Initialization</h2>
 * The JVM is not initially started with security manager enabled,
 * instead we turn it on early in the startup process. This is a tradeoff
 * between security and ease of use:
 * <ul>
 *   <li>Assigns file permissions to user-configurable paths that can
 *       be specified from the command-line or {@code elasticsearch.yml}.</li>
 *   <li>Allows for some contained usage of native code that would not
 *       otherwise be permitted.</li>
 * </ul>
 * <br>
 * <h2>Permissions</h2>
 * Permissions use a policy file packaged as a resource, this file is
 * also used in tests. File permissions are generated dynamically and
 * combined with this policy file.
 * <p>
 * For each configured path, we ensure it exists and is accessible before
 * granting permissions, otherwise directory creation would require
 * permissions to parent directories.
 * <p>
 * In some exceptional cases, permissions are assigned to specific jars only,
 * when they are so dangerous that general code should not be granted the
 * permission, but there are extenuating circumstances.
 * <p>
 * Scripts (groovy) are assigned minimal permissions. This does not provide adequate
 * sandboxing, as these scripts still have access to ES classes, and could
 * modify members, etc that would cause bad things to happen later on their
 * behalf (no package protections are yet in place, this would need some
 * cleanups to the scripting apis). But still it can provide some defense for users
 * that enable dynamic scripting without being fully aware of the consequences.
 * <br>
 * <h2>Debugging Security</h2>
 * A good place to start when there is a problem is to turn on security debugging:
 * <pre>
 * ES_JAVA_OPTS="-Djava.security.debug=access,failure" bin/elasticsearch
 * </pre>
 * <p>
 * When running tests you have to pass it to the test runner like this:
 * <pre>
 * gradle test -Dtests.jvm.argline="-Djava.security.debug=access,failure" ...
 * </pre>
 * See <a href="https:
 * Troubleshooting Security</a> for information.
 */
final class Security {

    static {
        prepopulateSecurityCaller();
    }

    /** no instantiation */
    private Security() {}

    static void setSecurityManager(@SuppressWarnings("removal") SecurityManager sm) {
        System.setSecurityManager(sm);
    }

    /**
     * Initializes SecurityManager for the environment
     * Can only happen once!
     * @param environment configuration for generating dynamic permissions
     * @param filterBadDefaults true if we should filter out bad java defaults in the system policy.
     */
    static void configure(Environment environment, boolean filterBadDefaults, Path pidFile) throws IOException {

        Map<String, URL> codebases = PolicyUtil.getCodebaseJarMap(JarHell.parseModulesAndClassPath());
        Policy.setPolicy(
            new ESPolicy(
                codebases,
                createPermissions(environment, pidFile),
                getPluginAndModulePermissions(environment),
                filterBadDefaults,
                createRecursiveDataPathPermission(environment)
            )
        );

        final String[] classesThatCanExit = new String[] {
            ElasticsearchUncaughtExceptionHandler.PrivilegedHaltAction.class.getName().replace("$", "\\$"),
            Bootstrap.class.getName() };
        setSecurityManager(new SecureSM(classesThatCanExit));

        selfTest();
    }

    /**
     * Sets properties (codebase URLs) for policy files.
     * we look for matching plugins and set URLs to fit
     */
    @SuppressForbidden(reason = "proper use of URL")
    static Map<String, Policy> getPluginAndModulePermissions(Environment environment) throws IOException {
        Map<String, Policy> map = new HashMap<>();
        Consumer<PluginPolicyInfo> addPolicy = pluginPolicy -> {
            if (pluginPolicy == null) {
                return;
            }

            for (URL jar : pluginPolicy.jars()) {
                if (map.put(jar.getFile(), pluginPolicy.policy()) != null) {
                    throw new IllegalStateException("per-plugin permissions already granted for jar file: " + jar);
                }
            }
        };

        for (Path plugin : PluginsUtils.findPluginDirs(environment.pluginsFile())) {
            addPolicy.accept(PolicyUtil.getPluginPolicyInfo(plugin, environment.tmpFile()));
        }
        for (Path plugin : PluginsUtils.findPluginDirs(environment.modulesFile())) {
            addPolicy.accept(PolicyUtil.getModulePolicyInfo(plugin, environment.tmpFile()));
        }

        return Collections.unmodifiableMap(map);
    }

    /** returns dynamic Permissions to configured paths and bind ports */
    static Permissions createPermissions(Environment environment, Path pidFile) throws IOException {
        Permissions policy = new Permissions();
        addClasspathPermissions(policy);
        addFilePermissions(policy, environment, pidFile);
        addBindPermissions(policy, environment.settings());
        return policy;
    }

    private static List<FilePermission> createRecursiveDataPathPermission(Environment environment) throws IOException {
        Permissions policy = new Permissions();
        for (Path path : environment.dataFiles()) {
            addDirectoryPath(policy, Environment.PATH_DATA_SETTING.getKey(), path, "read,readlink,write,delete", true);
        }
        return toFilePermissions(policy);
    }

    /** Adds access to classpath jars/classes for jar hell scan, etc */
    @SuppressForbidden(reason = "accesses fully qualified URLs to configure security")
    static void addClasspathPermissions(Permissions policy) throws IOException {
        for (URL url : JarHell.parseClassPath()) {
            Path path;
            try {
                path = PathUtils.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            if (Files.isDirectory(path)) {
                addDirectoryPath(policy, "class.path", path, "read,readlink", false);
            } else {
                addSingleFilePath(policy, path, "read,readlink");
            }
        }
    }

    /**
     * Adds access to all configurable paths.
     */
    static void addFilePermissions(Permissions policy, Environment environment, Path pidFile) throws IOException {
        addDirectoryPath(policy, Environment.PATH_HOME_SETTING.getKey(), environment.binFile(), "read,readlink", false);
        addDirectoryPath(policy, Environment.PATH_HOME_SETTING.getKey(), environment.libFile(), "read,readlink", false);
        addDirectoryPath(policy, Environment.PATH_HOME_SETTING.getKey(), environment.modulesFile(), "read,readlink", false);
        addDirectoryPath(policy, Environment.PATH_HOME_SETTING.getKey(), environment.pluginsFile(), "read,readlink", false);
        addDirectoryPath(policy, "path.conf", environment.configFile(), "read,readlink", false);
        addDirectoryPath(policy, "java.io.tmpdir", environment.tmpFile(), "read,readlink,write,delete", false);
        addDirectoryPath(policy, Environment.PATH_LOGS_SETTING.getKey(), environment.logsFile(), "read,readlink,write,delete", false);
        if (environment.sharedDataFile() != null) {
            addDirectoryPath(
                policy,
                Environment.PATH_SHARED_DATA_SETTING.getKey(),
                environment.sharedDataFile(),
                "read,readlink,write,delete",
                false
            );
        }
        final Set<Path> dataFilesPaths = new HashSet<>();
        for (Path path : environment.dataFiles()) {
            addDirectoryPath(policy, Environment.PATH_DATA_SETTING.getKey(), path, "read,readlink,write,delete", false);
            /*
             * We have to do this after adding the path because a side effect of that is that the directory is created; the Path#toRealPath
             * invocation will fail if the directory does not already exist. We use Path#toRealPath to follow symlinks and handle issues
             * like unicode normalization or case-insensitivity on some filesystems (e.g., the case-insensitive variant of HFS+ on macOS).
             */
            try {
                final Path realPath = path.toRealPath();
                if (dataFilesPaths.add(realPath) == false) {
                    throw new IllegalStateException("path [" + realPath + "] is duplicated by [" + path + "]");
                }
            } catch (final IOException e) {
                throw new IllegalStateException("unable to access [" + path + "]", e);
            }
        }
        for (Path path : environment.repoFiles()) {
            addDirectoryPath(policy, Environment.PATH_REPO_SETTING.getKey(), path, "read,readlink,write,delete", false);
        }
        if (pidFile != null) {
            addSingleFilePath(policy, pidFile, "delete");
        }
        addSingleFilePath(policy, environment.configFile().resolve(OPERATOR_DIRECTORY).resolve(SETTINGS_FILE_NAME), "read,readlink,write");
    }

    /**
     * Add dynamic {@link SocketPermission}s based on HTTP and transport settings.
     *
     * @param policy the {@link Permissions} instance to apply the dynamic {@link SocketPermission}s to.
     * @param settings the {@link Settings} instance to read the HTTP and transport settings from
     */
    private static void addBindPermissions(Permissions policy, Settings settings) {
        addSocketPermissionForHttp(policy, settings);
        addSocketPermissionForTransportProfiles(policy, settings);
    }

    /**
     * Add dynamic {@link SocketPermission} based on HTTP settings.
     *
     * @param policy the {@link Permissions} instance to apply the dynamic {@link SocketPermission}s to.
     * @param settings the {@link Settings} instance to read the HTTP settings from
     */
    private static void addSocketPermissionForHttp(final Permissions policy, final Settings settings) {
        final String httpRange = HttpTransportSettings.SETTING_HTTP_PORT.get(settings).getPortRangeString();
        addSocketPermissionForPortRange(policy, httpRange);
    }

    /**
     * Add dynamic {@link SocketPermission} based on transport settings. This method will first check if there is a port range specified in
     * the transport profile specified by {@code profileSettings} and will fall back to {@code settings}.
     *
     * @param policy          the {@link Permissions} instance to apply the dynamic {@link SocketPermission}s to
     * @param settings        the {@link Settings} instance to read the transport settings from
     */
    private static void addSocketPermissionForTransportProfiles(final Permissions policy, final Settings settings) {
        Set<TcpTransport.ProfileSettings> profiles = TcpTransport.getProfileSettings(settings);
        Set<String> uniquePortRanges = new HashSet<>();
        for (final TcpTransport.ProfileSettings profile : profiles) {
            if (uniquePortRanges.add(profile.portOrRange)) {
                addSocketPermissionForPortRange(policy, profile.portOrRange);
            }
        }
    }

    /**
     * Add dynamic {@link SocketPermission} for the specified port range.
     *
     * @param policy the {@link Permissions} instance to apply the dynamic {@link SocketPermission} to.
     * @param portRange the port range
     */
    private static void addSocketPermissionForPortRange(final Permissions policy, final String portRange) {
        policy.add(new SocketPermission("*:" + portRange, "listen,resolve"));
    }

    /**
     * Ensures configured directory {@code path} exists.
     * @throws IOException if {@code path} exists, but is not a directory, not accessible, or broken symbolic link.
     */
    static void ensureDirectoryExists(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            path.getFileSystem().provider().checkAccess(path.toRealPath(), AccessMode.READ);
        } else {
            try {
                Files.createDirectories(path);
            } catch (FileAlreadyExistsException e) {
                IOException e2 = new NotDirectoryException(path.toString());
                e2.addSuppressed(e);
                throw e2;
            }
        }
    }

    /** Simple checks that everything is ok */
    @SuppressForbidden(reason = "accesses jvm default tempdir as a self-test")
    static void selfTest() throws IOException {
        try {
            Path p = Files.createTempFile(null, null);
            try {
                Files.delete(p);
            } catch (IOException ignored) {
            }
        } catch (SecurityException problem) {
            throw new SecurityException("Security misconfiguration: cannot access java.io.tmpdir", problem);
        }
    }

    /**
     * Prepopulates the system's security manager callers map with this class as a caller.
     * This is loathsome, but avoids the annoying warning message at run time.
     * Returns true if the callers map has been populated.
     */
    static boolean prepopulateSecurityCaller() {
        Field f;
        try {
            f = getDeclaredField(Class.forName("java.lang.System$CallersHolder", true, null), "callers");
        } catch (NoSuchFieldException | ClassNotFoundException ignore) {
            return false;
        }
        try {
            Class<?> c = Class.forName("sun.misc.Unsafe");
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(c, MethodHandles.lookup());
            VarHandle handle = lookup.findStaticVarHandle(c, "theUnsafe", c);
            Object theUnsafe = handle.get();
            MethodHandle mh = lookup.findVirtual(c, "staticFieldBase", methodType(Object.class, Field.class));
            mh = mh.asType(mh.type().changeParameterType(0, Object.class));
            Object base = mh.invokeExact(theUnsafe, f);
            mh = lookup.findVirtual(c, "staticFieldOffset", methodType(long.class, Field.class));
            mh = mh.asType(mh.type().changeParameterType(0, Object.class));
            long offset = (long) mh.invokeExact(theUnsafe, f);
            mh = lookup.findVirtual(c, "getObject", methodType(Object.class, Object.class, long.class));
            mh = mh.asType(mh.type().changeParameterType(0, Object.class));
            Object callers = (Object) mh.invokeExact(theUnsafe, base, offset);
            if (Map.class.isAssignableFrom(callers.getClass())) {
                @SuppressWarnings("unchecked")
                Map<Class<?>, Boolean> map = Map.class.cast(callers);
                map.put(org.elasticsearch.bootstrap.Security.class, true);
                return true;
            }
        } catch (Throwable t) {
            throw new ElasticsearchException(t);
        }
        return false;
    }

    @SuppressForbidden(reason = "access violation required")
    private static Field getDeclaredField(Class<?> c, String name) throws NoSuchFieldException {
        return c.getDeclaredField(name);
    }

    /**
     * Assumes the given {@link Permissions} only contains {@link FilePermission} elements and returns them as
     * a list.
     *
     * @param permissions permissions to unwrap
     * @return list of file permissions found
     */
    static List<FilePermission> toFilePermissions(Permissions permissions) {
        return permissions.elementsAsStream().map(p -> {
            if (p instanceof FilePermission == false) {
                throw new AssertionError("[" + p + "] was not a file permission");
            }
            return (FilePermission) p;
        }).toList();
    }
}
