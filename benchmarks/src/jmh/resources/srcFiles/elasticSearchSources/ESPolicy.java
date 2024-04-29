/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.bootstrap;

import org.elasticsearch.core.Predicates;
import org.elasticsearch.core.SuppressForbidden;

import java.io.FilePermission;
import java.io.IOException;
import java.net.SocketPermission;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** custom policy for union of static and dynamic permissions */
final class ESPolicy extends Policy {

    /** template policy file, the one used in tests */
    static final String POLICY_RESOURCE = "security.policy";
    /** limited policy for scripts */
    static final String UNTRUSTED_RESOURCE = "untrusted.policy";

    final Policy template;
    final Policy untrusted;
    final Policy system;
    final PermissionCollection dynamic;
    final PermissionCollection dataPathPermission;
    final Map<String, Policy> plugins;

    ESPolicy(
        Map<String, URL> codebases,
        PermissionCollection dynamic,
        Map<String, Policy> plugins,
        boolean filterBadDefaults,
        List<FilePermission> dataPathPermissions
    ) {
        this.template = PolicyUtil.readPolicy(getClass().getResource(POLICY_RESOURCE), codebases);
        PermissionCollection dpPermissions = null;
        for (FilePermission permission : dataPathPermissions) {
            if (dpPermissions == null) {
                dpPermissions = permission.newPermissionCollection();
            }
            dpPermissions.add(permission);
        }
        this.dataPathPermission = dpPermissions == null ? new Permissions() : dpPermissions;
        this.dataPathPermission.setReadOnly();
        this.untrusted = PolicyUtil.readPolicy(getClass().getResource(UNTRUSTED_RESOURCE), Collections.emptyMap());
        if (filterBadDefaults) {
            this.system = new SystemPolicy(Policy.getPolicy());
        } else {
            this.system = Policy.getPolicy();
        }
        this.dynamic = dynamic;
        this.plugins = plugins;
    }

    @Override
    @SuppressForbidden(reason = "fast equals check is desired")
    public boolean implies(ProtectionDomain domain, Permission permission) {
        CodeSource codeSource = domain.getCodeSource();
        if (codeSource == null) {
            return false;
        }

        URL location = codeSource.getLocation();
        if (location != null) {
            if (BootstrapInfo.UNTRUSTED_CODEBASE.equals(location.getFile())) {
                return untrusted.implies(domain, permission);
            }
            Policy plugin = plugins.get(location.getFile());
            if (plugin != null && plugin.implies(domain, permission)) {
                return true;
            }
        }

        if (permission instanceof FilePermission) {
            if (dataPathPermission.implies(permission)) {
                return true;
            }
            if ("<<ALL FILES>>".equals(permission.getName())) {
                hadoopHack();
            }
        }

        return template.implies(domain, permission) || dynamic.implies(permission) || system.implies(domain, permission);
    }

    private static void hadoopHack() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if ("org.apache.hadoop.util.Shell".equals(element.getClassName()) && "runCommand".equals(element.getMethodName())) {
                rethrow(new IOException("no hadoop, you cannot do this."));
            }
        }
    }

    /**
     * Classy puzzler to rethrow any checked exception as an unchecked one.
     */
    private static class Rethrower<T extends Throwable> {
        @SuppressWarnings("unchecked")
        private void rethrow(Throwable t) throws T {
            throw (T) t;
        }
    }

    /**
     * Rethrows <code>t</code> (identical object).
     */
    private static void rethrow(Throwable t) {
        new Rethrower<Error>().rethrow(t);
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if ("sun.rmi.server.LoaderHandler".equals(element.getClassName()) && "loadClass".equals(element.getMethodName())) {
                return new Permissions();
            }
        }
        return super.getPermissions(codesource);
    }


    /**
     * Wraps a bad default permission, applying a pre-implies to any permissions before checking if the wrapped bad default permission
     * implies a permission.
     */
    private static class BadDefaultPermission extends Permission {

        private final Permission badDefaultPermission;
        private final Predicate<Permission> preImplies;

        /**
         * Construct an instance with a pre-implies check to apply to desired permissions.
         *
         * @param badDefaultPermission the bad default permission to wrap
         * @param preImplies           a test that is applied to a desired permission before checking if the bad default permission that
         *                             this instance wraps implies the desired permission
         */
        BadDefaultPermission(final Permission badDefaultPermission, final Predicate<Permission> preImplies) {
            super(badDefaultPermission.getName());
            this.badDefaultPermission = badDefaultPermission;
            this.preImplies = preImplies;
        }

        @Override
        public final boolean implies(Permission permission) {
            return preImplies.test(permission) && badDefaultPermission.implies(permission);
        }

        @Override
        public final boolean equals(Object obj) {
            return badDefaultPermission.equals(obj);
        }

        @Override
        public int hashCode() {
            return badDefaultPermission.hashCode();
        }

        @Override
        public String getActions() {
            return badDefaultPermission.getActions();
        }

    }

    private static final Permission BAD_DEFAULT_NUMBER_ONE = new BadDefaultPermission(
        new RuntimePermission("stopThread"),
        Predicates.always()
    );

    private static final Permission BAD_DEFAULT_NUMBER_TWO = new BadDefaultPermission(
        new SocketPermission("localhost:0", "listen"),
        p -> p instanceof SocketPermission && p.getActions().contains("listen")
    );

    /**
     * Wraps the Java system policy, filtering out bad default permissions that
     * are granted to all domains. Note, before java 8 these were even worse.
     */
    static class SystemPolicy extends Policy {
        final Policy delegate;

        SystemPolicy(Policy delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (BAD_DEFAULT_NUMBER_ONE.implies(permission) || BAD_DEFAULT_NUMBER_TWO.implies(permission)) {
                return false;
            }
            return delegate.implies(domain, permission);
        }
    }
}
